from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseNotFound
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User, Group
from django.contrib.auth.decorators import login_required
from django.contrib.auth.hashers import make_password
from django.utils.decorators import method_decorator
from django.utils import timezone
from datetime import timedelta
from rest_framework import viewsets, permissions, filters
from rest_framework.decorators import api_view, permission_classes
from korjournal.models import Vehicle, Driver, OdometerSnap, OdometerImage, RegisterCode
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer, OdometerImageSerializer, RegisterCodeSerializer, DriverSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll, IsDriver
from korjournal.forms import DeleteOdoSnapForm, YearVehicleForm, DeleteOdoImageForm, RegistrationForm, VerificationForm, DeleteVehicleForm, DeleteDriverForm
import copy
import subprocess
import sys
import random
import json
import pprint
from django.core.exceptions import ObjectDoesNotExist
from django.views.decorators.csrf import csrf_exempt
from django.db.models import Q
from django.http.request import RawPostDataException
from django.db import IntegrityError

# Create your views here.
def landing(request):
    return render(request, 'korjournal/landing.html', {})

@login_required(login_url='/login')
def editor(request):
    odo_snap_list = OdometerSnap.objects.filter(Q(vehicle__owner=request.user)|Q(vehicle__driver__user=request.user)).order_by('-when')
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
        return HttpResponseRedirect(reverse('editor'))

    return editor(request)

@login_required(login_url='/login')
def report(request):
    year = '2016'
    message = "Välj fordon och år"
    my_vehicles = Vehicle.objects.filter(owner=request.user)

    vehicle_choices = ()
    for v in my_vehicles:
        vehicle_choices = vehicle_choices + ((v.id,v.name),)

    if request.method == 'POST':
        message = "0 resor"
        form = YearVehicleForm(request.POST,vehicles=vehicle_choices)
        if form.is_valid():
            vehicle_id = form.cleaned_data['vehicle']
            year = form.cleaned_data['year']
    else:
        form = YearVehicleForm(vehicles=vehicle_choices)

    try:
        selected_vehicle = Vehicle.objects.filter(pk=vehicle_id)[0]
        odo_snap_list = OdometerSnap.objects.filter(vehicle__owner=request.user,vehicle=selected_vehicle,when__year=year).order_by('when');
    except (IndexError,UnboundLocalError):
        odo_snap_list = OdometerSnap.objects.none()

    trips = []
    t = {}
    for snap in odo_snap_list:

        if (snap.type == "1"):
            t['startid'] = snap.id
            t['startodo'] = snap.odometer
            t['startwhere'] = snap.where
            t['startwhen'] = snap.when
            try:
                t['startimage'] = snap.odometerimage.image
            except ObjectDoesNotExist:
                t['startimage'] = None

        if (snap.type == "2"):
            t['endodo'] = snap.odometer
            t['endwhere'] = snap.where
            t['endwhen'] = snap.when
            t['reason'] = snap.why

            t['km'] = 0
            try:
                if (t['endodo'] - t['startodo'] > 0):
                    t['km'] = int(t['endodo']) - int(t['startodo'])
            except KeyError:
                pass

            try:
                if (t['startid']):
                   trips.append(copy.deepcopy(t))
            except KeyError:
                pass

            try:
                t['endimage'] = snap.odometerimage.image
            except ObjectDoesNotExist:
                t['endimage'] = None

            # Should the next position be another end, we put in this position as start
            t['startid'] = snap.id
            t['startodo'] = snap.odometer
            t['startwhere'] = snap.where
            t['startwhen'] = snap.when
            try:
                t['startimage'] = snap.odometerimage.image
            except ObjectDoesNotExist:
                t['startimage'] = None
            message = "%d resor" % len(trips)

    context = { 'reports': trips, 'year_vehicle_form': form, 'year': year, 'summary': message }
    return render(request, 'korjournal/report.html', context)

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

    def get_queryset(self):
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
            vehicle = Vehicle.objects.filter(pk=data['vehicle'])[0]
        except IndexError:
            return HttpResponseNotFound('{"errors":{"vehicle": "Fordonet finns inte"}}')

        try:
            driver = Driver(user=user,vehicle=vehicle)
            driver.save()
        except IntegrityError:
            return HttpResponseNotFound('Föraren finns redan på fordonet')
        return HttpResponse('{"id": %d}' % driver.id)

 
class OdometerSnapViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerSnapSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner,IsDriver)
    filter_backends = (filters.OrderingFilter,)
    ordering_fields = '__all__'
    
    def get_queryset(self):
        return OdometerSnap.objects.filter(Q(vehicle__owner=self.request.user)|Q(driver=self.request.user))

    def perform_create(self,serializer):
        serializer.save(driver=self.request.user)

class OdometerImageViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerImageSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner,IsDriver)

    def perform_create(self,serializer):
        imgfile = self.request.FILES.get('imagefile')
        odoimage = serializer.save(driver=self.request.user, imagefile=imgfile)
        if (odoimage.odometersnap.odometer < 1):
            ocr = subprocess.run(["/usr/bin/tesseract", "/vagrant/www/media/" + odoimage.imagefile.name, "stdout", "nobatch", "digits"], stdout=subprocess.PIPE,universal_newlines=True).stdout
            print(ocr,file=sys.stderr)
            try:
                newodokm = int(ocr.replace(" ",""))
                odoimage.odometersnap.odometer = newodokm
                odoimage.odometersnap.save()
            except ValueError:
                pass

    def get_queryset(self):
        return OdometerImage.objects.filter(Q(vehicle__owner=self.request.user)|Q(driver__user=self.request.user))

def delete_odo_image(request,odo_image_id):
    odo_image = get_object_or_404(OdometerImage, pk=odo_image_id)
    form = DeleteOdoImageForm(request.POST)
    if form.is_valid():
        odo_image.delete()
        return HttpResponseRedirect(reverse('editor'))

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
