from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseNotFound, HttpResponseForbidden
from django.core.urlresolvers import reverse
from django.core.paginator import Paginator, EmptyPage, PageNotAnInteger
from django.contrib.auth.models import User, Group, AnonymousUser
from django.contrib.auth.decorators import login_required
from django.contrib.auth.hashers import make_password
from django.utils.decorators import method_decorator
from django.utils import timezone
from datetime import timedelta
from dateutil import tz, parser
from rest_framework import viewsets, permissions, filters, renderers
from rest_framework.decorators import api_view, permission_classes
from korjournal.models import Vehicle, Driver, OdometerSnap, OdometerImage, RegisterCode
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer, OdometerImageSerializer, RegisterCodeSerializer, DriverSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll, IsDriver
from korjournal.forms import DeleteOdoSnapForm, YearVehicleForm, DeleteOdoImageForm, RegistrationForm, VerificationForm, DeleteVehicleForm, DeleteDriverForm, ContactForm, ApplinkForm
import copy
import subprocess
import sys
import random
import json
import pprint
from django.core.exceptions import ObjectDoesNotExist, PermissionDenied
from django.views.decorators.csrf import csrf_exempt
from django.db.models import Q
from django.http.request import RawPostDataException
from django.db import IntegrityError
from django.template import Context
from django.template.loader import get_template
from django.core.mail import EmailMessage
from korjournal.utils import sendsms as smsutil

def make_contactform(request):

    if request.method != 'POST':
        return ContactForm()

    form = ContactForm(data=request.POST)
    if form.is_valid():
        name = form.cleaned_data['name']
        email = form.cleaned_data['email']
        subject = form.cleaned_data['subject']
        message = form.cleaned_data['message']

        template = get_template('mail/contact.txt')
        context = Context({
            'name': name,
            'email': email,
            'subject': subject,
            'message': message
            })
        content = template.render(context)
        emailMessage = EmailMessage(
                "Kilometerkolls kontaktformulär",
                content,
                "Kilometerkoll <noreply@kilometerkoll.se>",
                ['kundtjanst@kilometerkoll.se'],
                headers = {'Reply-To': email }
            )
        emailMessage.send()
        form.add_error('message', "Meddelandet har skickats")
        return form
    form.add_error('recaptcha', "Formuläret var inte giltigt")
    return form

# Create your views here.
def landing(request):
    baseurl_host = request.get_host()
    navigation1 = {}
    navigation1['link'] =  '/register/'
    navigation1['text'] = 'Registrera'
    navigation2 = {}
    navigation2['link'] =  '/login/'
    navigation2['text'] = 'Logga in'
    contactform = make_contactform(request)
    return render(request, 'korjournal/landing.html', {'baseurl_host': baseurl_host, 'navigation1': navigation1, 'navigation2': navigation2, 'contactform': contactform})

def privacy_policy(request):
    baseurl_host = request.get_host()
    navigation1 = {}
    navigation1['link'] =  '/register/'
    navigation1['text'] = 'Registrera'
    navigation2 = {}
    navigation2['link'] =  '/login/'
    navigation2['text'] = 'Logga in'
    contactform = make_contactform(request)
    return render(request, 'korjournal/privacy-policy.html', {'baseurl_host': baseurl_host, 'navigation1': navigation1, 'navigation2': navigation2, 'contactform': contactform})

@login_required(login_url='/login')
def profile(request):
    context = { 'username': request.user.username }
    return render(request, 'korjournal/profile.html', context)

@login_required(login_url='/login')
def editor(request):
    odo_snap_all = OdometerSnap.objects.filter(Q(vehicle__owner=request.user)|Q(vehicle__driver__user=request.user)).order_by('-when')
    paginator = Paginator(odo_snap_all, 50)

    page = request.GET.get('page')
    try:
        odo_snap_list = paginator.page(page)
    except PageNotAnInteger:
        odo_snap_list = paginator.page(1)
    except EmptyPage:
        odo_snap_list = paginator.page(paginator.num_pages)

    for odo_snap in odo_snap_list:
        if (odo_snap.vehicle.owner == request.user or odo_snap.driver == request.user):
            odo_snap.editable = True
        else:
            odo_snap.editable = False

    my_vehicles = Vehicle.objects.filter(Q(owner=request.user)|Q(driver__user=request.user))
    last_month = timezone.now() - timedelta(days=31)
    odo_unique_reason = OdometerSnap.objects.filter(Q(vehicle__owner=request.user)|Q(vehicle__driver__user=request.user),when__gt=last_month).values('why').distinct().order_by('why')
    form = DeleteOdoSnapForm()
    context = { 'odo_snap_list': odo_snap_list, 'form': form, 'my_vehicles': my_vehicles, 'odo_unique_reason': odo_unique_reason, 'username': request.user.username }
    return render(request, 'korjournal/editor.html', context)

def delete_odo_snap(request,odo_snap_id):
    odo_snap = get_object_or_404(OdometerSnap, pk=odo_snap_id)
    form = DeleteOdoSnapForm(request.POST)
    if form.is_valid():
        odo_snap.delete()
        paginator_page = request.POST.get('paginator_page')
        return HttpResponseRedirect(reverse('editor') + '?page=' + paginator_page)

    return editor(request)

class UserViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows users to be viewed or edited.
    """
    queryset = User.objects.all().order_by('-date_joined')
    serializer_class = UserSerializer


class GroupViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows groups to be viewed or edited.
    """
    queryset = Group.objects.all()
    serializer_class = GroupSerializer

class VehicleViewSet(viewsets.ModelViewSet):
    serializer_class = VehicleSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner,IsDriver)
    renderer_classes = (renderers.JSONRenderer,)

    def get_queryset(self):
        user = self.request.user
        if isinstance(user, AnonymousUser):
            raise PermissionDenied
            return Vehicle.objects.none()
        return Vehicle.objects.filter(Q(owner=self.request.user)|Q(driver__user=self.request.user))

    def perform_create(self,serializer):
        serializer.save(owner=self.request.user)

class DriverViewSet(viewsets.ModelViewSet):
    serializer_class = DriverSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner)

    def get_queryset(self):
        return Driver.objects.filter(vehicle__owner=self.request.user)

    def create(self,request):
        try:
            data = json.loads(self.request.body.decode('utf-8'))
        except RawPostDataException:
            data = request.data
        try:
            user = User.objects.filter(username=data['user'])[0]
        except IndexError:
            return HttpResponseNotFound('Användaren är ej registerad')
        try:
            vehicle = Vehicle.objects.filter(pk=data['vehicle'],owner=request.user)[0]
        except IndexError:
            return HttpResponseNotFound('{"errors":{"vehicle": "Fordonet finns inte"}}')

        try:
            driver = Driver(user=user,vehicle=vehicle)
            driver.save()
        except IntegrityError:
            return HttpResponseNotFound('Föraren finns redan på fordonet')
        return HttpResponse('{"id": %d}' % driver.id)

    def list(self, request):
        return HttpResponseNotFound('Förare kan inte listas via API')

def delete_odo_image(request,odo_image_id):
    odo_image = get_object_or_404(OdometerImage, pk=odo_image_id)
    form = DeleteOdoImageForm(request.POST)
    if form.is_valid():
        odo_image.delete()
        paginator_page = request.POST.get('paginator_page')
        return HttpResponseRedirect(reverse('editor') + '?page=' + paginator_page)

    return editor(request)

def make_register_code(phone):
    random.seed()
    code = random.randint(10000,99999)
    serializer = RegisterCodeSerializer(data={'phone': phone, 'code': code})
    if (serializer.is_valid()):
        # The serializer will send the sms
        serializer.save()
    else:
        die


@csrf_exempt
def doregister(request):
    if request.method == 'POST':
        form = RegistrationForm(request.POST)
        if form.is_valid():
            phone = form.cleaned_data['phone']
            make_register_code(phone)
            return HttpResponseRedirect('/verify/?phone=%s' % phone)
        else:
            form = RegistrationForm(request.POST)
            return render(request, 'registration/register.html', {'form': form})
    form = RegistrationForm()
    return render(request, 'registration/register.html', {'form': form,})

@csrf_exempt
def verify(request):
    if request.method == 'POST':
        form = VerificationForm(request.POST)
        if form.is_valid():
            phone = form.cleaned_data['phone']
            code = form.cleaned_data['code']
            validtime = timezone.now() - timedelta(hours=12)
            try:
                valid = RegisterCode.objects.filter(phone=phone,code=code,when__gt=validtime)[0]
            except IndexError:
                form.add_error('code','Den angivna koden är inte giltig')
                return render(request, 'registration/verify.html', {'form': form, 'phone': phone}, status=404)
            try:
                user = User.objects.filter(username=phone)[0]
                user.password=make_password(code)
            except IndexError:
                user = User(username=phone,password=make_password(code));
            user.save()
            return HttpResponseRedirect('/registration_complete/?phone=%s' % phone)
        else:
            form = VerificationForm(request.POST)
            return render(request, 'registration/verify.html', {'form': form,}, status=400)
    phone = request.GET['phone']
    form = VerificationForm()
    return render(request, 'registration/verify.html', {'form': form, 'phone': phone})

def registration_complete(request):
    phone = request.GET['phone']
    return render(request, 'registration/complete.html', {'phone': phone})

@login_required(login_url='/login?next=/vehicles')
def vehicles(request):
    my_vehicles = Vehicle.objects.filter(owner=request.user)
    for vehicle in my_vehicles:
        vehicle.driver = Driver.objects.filter(vehicle=vehicle)
    drive_vehicles = Vehicle.objects.filter(driver__user=request.user)
    form = DeleteVehicleForm()
    context = { 'form': form, 'my_vehicles': my_vehicles, 'drive_vehicles': drive_vehicles, 'username': request.user.username}
    return render(request, 'korjournal/vehicles.html', context)

def delete_vehicle(request,vehicle_id):
    vehicle = get_object_or_404(Vehicle, pk=vehicle_id)
    form = DeleteVehicleForm(request.POST)
    if form.is_valid():
        vehicle.delete()
        return HttpResponseRedirect(reverse('vehicles'))
    return vehicles(request)

@api_view(('POST',))
@permission_classes((IsOwner,))
def delete_driver(request,driver_id):
    driver = get_object_or_404(Driver, pk=driver_id)
    form = DeleteDriverForm(request.POST)
    if form.is_valid():
        driver.delete()
        return HttpResponseRedirect(reverse('vehicles'))
    return vehicles(request)

def send_applink(phone):
    smsutil.send_sms(phone, "Kilometerkoll för iPhone: https://itunes.apple.com/se/app/kilometerkoll/id1202196220?mt=8 Android: http://play.google.com/store/apps/details?id=se.linefeed.korjournal")

def applink(request):
    baseurl_host = request.get_host()
    navigation1 = { "link": '/register/', "text": 'Registrera',}
    navigation2 = { "link": '/login/', "text": 'Logga in'}

    if request.method == 'POST':
        form = ApplinkForm(request.POST)
        if form.is_valid():
            phone = form.cleaned_data['phone']
            send_applink(phone)
        return render(request, 'korjournal/app.html', {'baseurl_host': baseurl_host, 'navigation1': navigation1, 'navigation2': navigation2, 'form': form})

    form = ApplinkForm()
    return render(request, 'korjournal/app.html', {'baseurl_host': baseurl_host, 'navigation1': navigation1, 'navigation2': navigation2, 'form': form})
