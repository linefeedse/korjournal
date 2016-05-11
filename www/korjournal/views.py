from django.shortcuts import render
from django.contrib.auth.models import User, Group
from rest_framework import viewsets, permissions
from korjournal.models import Vehicle, OdometerSnap
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll

# Create your views here.
def landing(request):
    return render(request, 'korjournal/landing.html', {})

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
        return Vehicle.objects.filter(name=usergroup)
 
class OdometerSnapViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerSnapSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner)
    
    def get_queryset(self):
        try:
            usergroup = self.request.user.groups.all()[0]
        except IndexError:
            return ""
        return OdometerSnap.objects.filter(owner=usergroup)

    def perform_create(self,serializer):
        matchinggroup = Group.objects.filter(name=serializer.validated_data['vehicle'])[0:1].get()
        serializer.save(owner=matchinggroup, uploadedby=self.request.user)
