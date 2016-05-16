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
from korjournal.forms import DeleteOdoSnapForm

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
