from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User, Group
from django.contrib.auth.decorators import login_required
from django.contrib.auth.hashers import make_password
from django.utils.decorators import method_decorator
from django.utils import timezone
from datetime import timedelta
from rest_framework import viewsets, permissions, filters
from korjournal.models import Vehicle, OdometerSnap, OdometerImage, RegisterCode
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer, OdometerImageSerializer, RegisterCodeSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll
from korjournal.forms import DeleteOdoSnapForm, YearVehicleForm, DeleteOdoImageForm, RegistrationForm, VerificationForm
import copy
import subprocess
import sys
import random
from django.core.exceptions import ObjectDoesNotExist

# Create your views here.
def landing(request):
    return render(request, 'korjournal/landing.html', {})

@login_required(login_url='/login')
def editor(request):
    try:
        usergroup = request.user.groups.all()[0]
        odo_snap_list = OdometerSnap.objects.filter(owner=usergroup).order_by('-when');
        my_vehicles = Vehicle.objects.filter(group=usergroup)
    except IndexError:
        usergroup = "none"
        odo_snap_list = OdometerSnap.objects.none()
        my_vehicles = Vehicle.objects.none()
    last_month = timezone.now() - timedelta(days=31)
    odo_unique_reason = OdometerSnap.objects.filter(owner=usergroup,when__gt=last_month).values('why').distinct().order_by('why')
    form = DeleteOdoSnapForm()
    context = { 'odo_snap_list': odo_snap_list, 'form': form, 'my_vehicles': my_vehicles, 'odo_unique_reason': odo_unique_reason }
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
    try:
        usergroup = request.user.groups.all()[0]
        my_vehicles = Vehicle.objects.filter(group=usergroup)
    except IndexError:
        usergroup = "none"
        my_vehicles = Vehicle.objects.none()

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
        odo_snap_list = OdometerSnap.objects.filter(owner=usergroup,vehicle=selected_vehicle).order_by('when');
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
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner)

    def get_queryset(self):
        try:
            usergroup = self.request.user.groups.all()[0]
        except IndexError:
            return ""
        return Vehicle.objects.filter(group=usergroup)
 
class OdometerSnapViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerSnapSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner)
    filter_backends = (filters.OrderingFilter,)
    ordering_fields = '__all__'
    
    def get_queryset(self):
        try:
            usergroup = self.request.user.groups.all()[0]
        except IndexError:
            return ""
        return OdometerSnap.objects.filter(owner=usergroup)

    def perform_create(self,serializer):
        matchinggroup = Group.objects.filter(name=serializer.validated_data['vehicle'])[0:1].get()
        serializer.save(owner=matchinggroup, uploadedby=self.request.user)

class OdometerImageViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerImageSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner)

    def perform_create(self,serializer):
        usergroup = self.request.user.groups.all()[0]
        imgfile = self.request.FILES.get('imagefile')
        odoimage = serializer.save(owner=usergroup, uploadedby=self.request.user, imagefile=imgfile)
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
        try:
            usergroup = self.request.user.groups.all()[0]
        except IndexError:
            return ""
        return OdometerImage.objects.filter(owner=usergroup)

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
                return render(request, 'registration/verify.html', {'form': form, 'phone': phone})
            try:
                user = User.objects.filter(username=phone)[0]
                user.password=make_password(code)
            except IndexError:
                user = User(username=phone,password=make_password(code));
            user.save()
            return HttpResponseRedirect('/registration_complete/?phone=%s' % phone)
        else:
            form = VerificationForm(request.POST)
            return render(request, 'registration/verify.html', {'form': form,})
    phone = request.GET['phone']
    form = VerificationForm()
    return render(request, 'registration/verify.html', {'form': form, 'phone': phone})

def registration_complete(request):
    phone = request.GET['phone']
    return render(request, 'registration/complete.html', {'phone': phone})
