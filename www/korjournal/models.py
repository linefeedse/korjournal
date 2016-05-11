from django.db import models
from django.utils import timezone

class Vehicle(models.Model):
    name = models.CharField(max_length=64, blank=False, default='')
    group = models.ForeignKey('auth.Group')

    def __str__(self):
        return self.name

class OdometerSnap(models.Model):
    uploadedby = models.ForeignKey('auth.User')
    owner = models.ForeignKey('auth.Group')
    vehicle = models.ForeignKey('Vehicle')
    odometer = models.IntegerField()
    when = models.DateTimeField(default=timezone.now)
    where = models.CharField(max_length=128, default="")
    poslat = models.DecimalField(max_digits=10, decimal_places=7, default=0)
    poslon = models.DecimalField(max_digits=10, decimal_places=7, default=0)

    def __str__(self):
        return "%s-%s-%s" % (self.vehicle, self.odometer, self.when)
