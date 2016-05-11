from django.contrib.auth.models import User, Group
from korjournal.models import OdometerSnap, Vehicle
from rest_framework import serializers


class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ('url', 'username', 'email', 'groups')


class GroupSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Group
        fields = ('url', 'name')

class VehicleSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Vehicle
        fields = ('url','name');

class OdometerSnapSerializer(serializers.HyperlinkedModelSerializer):
    uploadedby = serializers.ReadOnlyField(source='uploadedby.username')
    owner = serializers.ReadOnlyField(source='owner.name')
    class Meta:
        model = OdometerSnap
        fields = ('url','vehicle','odometer','uploadedby', 'owner', 'poslat', 'poslon', 'where')

