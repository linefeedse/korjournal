from django.contrib.auth.models import User, Group
from korjournal.models import OdometerSnap, Vehicle, OdometerImage, RegisterCode, Driver
from korjournal.model.invoice import Invoice
from rest_framework import serializers
import requests
from requests_oauthlib import OAuth1Session
from os import getenv
import logging
from korjournal.utils import sendsms as smsutil


class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ('url', 'username', 'email', 'password')
        write_only_fields = ('password',)
    def create(self, validated_data):
        user = User(
            username=validated_data['username']
        )
        user.set_password(validated_data['password'])
        user.save()
        return user


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
        fields = ('url','odometersnap','imagefile','driver', 'guess0', 'guess1', 'guess2', 'guess3', 'guess4')

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
        smsutil.send_sms(validated_data['phone'], 'Din kod: %d' % validated_data['code'])
        return code

class DriverSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Driver
        fields = ('url','vehicle','user')

class InvoiceSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Invoice
        fields = (
            'url',
            'link_id',
            'customer',
            'customer_name',
            'customer_address',
            'scope_from',
            'scope_to',
            'invoicedate',
            'duedate',
            'specification',
            'amount',
            'invoice_number',
            'is_paid')