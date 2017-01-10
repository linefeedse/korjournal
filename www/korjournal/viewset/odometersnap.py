from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseNotFound
from django.utils import timezone
from datetime import timedelta
from dateutil import tz, parser
from rest_framework import viewsets, permissions, filters, serializers
from rest_framework.decorators import api_view, permission_classes
from korjournal.models import OdometerSnap, OdometerImage, Driver, Vehicle
from korjournal.serializers import OdometerSnapSerializer, OdometerImageSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll, IsDriver
from django.core.exceptions import ObjectDoesNotExist
from django.views.decorators.csrf import csrf_exempt
from django.db.models import Q
from django.http.request import RawPostDataException
from django.db import IntegrityError
import re


class OdometerSnapViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerSnapSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly, IsOwner, IsDriver)
    filter_backends = (filters.OrderingFilter,)
    ordering_fields = '__all__'
    
    def get_queryset(self):
        queryset = OdometerSnap.objects.filter(Q(vehicle__owner=self.request.user)|Q(driver=self.request.user))
        after = self.request.query_params.get('after', None)
        days = self.request.query_params.get('days', None)
        if after is not None:
            after_when = parser.parse(after)
            queryset = queryset.filter(Q(when__gte=after_when.isoformat()))
        else:
            if days is not None:
                after_when = timezone.now() - timedelta(days=int(days))
                queryset = queryset.filter(Q(when__gte=after_when.isoformat()))
        return queryset

    def perform_create(self,serializer):
        try:
            vehicle_pk = re.sub(r'.*/([0-9]+)/*$', r'\1', self.request.data['vehicle'])
            driver = Driver.objects.filter(user=self.request.user,vehicle=vehicle_pk)[0]
        except IndexError:
            try:
                vehicleByOwner = Vehicle.objects.filter(id=vehicle_pk, owner=self.request.user)[0]
            except IndexError:
                raise serializers.ValidationError('User is not a driver or owner for vehicle ' + str(vehicle_pk) )
        except AttributeError:
            raise serializers.ValidationError(self.request.data)
        serializer.save(driver=self.request.user)

    def perform_update(self,serializer):
        instance = self.get_object()
        if "when" in self.request.data:
            tzsweden = tz.gettz('Europe/Stockholm')
            tzutc = tz.gettz('UTC')
            when_tzsweden = parser.parse(self.request.data['when']).replace(tzinfo=tzsweden)
            serializer.save(when=when_tzsweden.astimezone(tzutc))
        else:
            serializer.save()