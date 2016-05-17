from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User, Group
from django.contrib.auth.decorators import login_required
from django.utils.decorators import method_decorator
from django.utils import timezone
from datetime import timedelta
from rest_framework import viewsets, permissions, filters
from korjournal.models import Vehicle, OdometerSnap
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll
from korjournal.forms import DeleteOdoSnapForm, YearVehicleForm
import copy

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

    odo_snap_list = OdometerSnap.objects.all()
    context = { 'odo_snap_list': odo_snap_list }
    return render(request, 'korjournal/editor.html', context)

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

        if (snap.type == "2"):
            t['endodo'] = snap.odometer
            t['endwhere'] = snap.where
            t['endwhen'] = snap.when
            t['reason'] = snap.why

            try:
                if (t['endodo'] - t['startodo'] > 0):
                    t['km'] = int(t['endodo']) - int(t['startodo'])
            except KeyError:
                t['km'] = 0
            try:
                if (t['startid']):
                   trips.append(copy.deepcopy(t))
            except KeyError:
                pass
            # Should the next position be another end, we put in this position as start
            t['startid'] = snap.id
            t['startodo'] = snap.odometer
            t['startwhere'] = snap.where
            t['startwhen'] = snap.when
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
