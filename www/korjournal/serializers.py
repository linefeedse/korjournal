from django.contrib.auth.models import User, Group
from korjournal.models import OdometerSnap, Vehicle, OdometerImage, RegisterCode, Driver
from rest_framework import serializers
import requests
from requests_oauthlib import OAuth1Session
from os import getenv
import logging


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
        fields = ('url','name','owner');

class OdometerSnapSerializer(serializers.HyperlinkedModelSerializer):
    driver = serializers.ReadOnlyField(source='driver.username')
    class Meta:
        model = OdometerSnap
        fields = ('url','vehicle','odometer','driver','poslat','poslon','where','when','type','why')

class OdometerImageSerializer(serializers.HyperlinkedModelSerializer):
    driver = serializers.ReadOnlyField(source='driver.username')
    class Meta:
        model = OdometerImage
        fields = ('url','odometersnap','imagefile','driver')

class RegisterCodeSerializer(serializers.ModelSerializer):
    class Meta:
        model = RegisterCode
        fields = ('phone','code','when')

    def create(self,validated_data):
        code = RegisterCode(
                phone=validated_data['phone'],
                code=validated_data['code'],
               )
        code.save()
        key = getenv('SMS_GATEWAY_KEY');
        secret = getenv('SMS_GATEWAY_SECRET');
        gwapi = OAuth1Session(key, client_secret=secret)
        req = {
            'recipients': [{'msisdn': int('46%d' % validated_data['phone'])}],
            'message': 'Din kod: %d' % validated_data['code'],
            'sender': 'Körjournal',
        }
        res = gwapi.post('https://gatewayapi.com/rest/mtsms', json=req)
        if (res.status_code != requests.codes.ok):
            logger = logging.getLogger(__name__)
            logger.error('Return code %d from sms gateway' % res.status_code)
            die
        return code

class DriverSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Driver
        fields = ('url','vehicle','user')

#    def create(self,validated_data):
#        try:
#            user = User.objects.filter(username=validated_data['user'])[0]
#            vehicle = Vehicle.objects.filter(id=validated_data['vehicle'])[0]
#        except IndexError:
#            return None
#       driver = Driver(user=user,vehicle=vehicle)
