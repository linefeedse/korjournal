from django.db import models
from django.utils import timezone
from datetime import datetime, timedelta
from uuid import uuid4
import os

class Vehicle(models.Model):
    name = models.CharField(max_length=64, blank=False, unique=True)
    owner = models.ForeignKey('auth.User', null=True, default = None)

    def __str__(self):
        return self.name

class Driver(models.Model):
    user = models.ForeignKey('auth.User')
    vehicle = models.ForeignKey('Vehicle')

    def __str__(self):
        return "%s-%s" % (self.user, self.vehicle)
    class Meta:
        unique_together = ('user', 'vehicle',)

class OdometerSnap(models.Model):
    driver = models.ForeignKey('auth.User')
    vehicle = models.ForeignKey('Vehicle')
    odometer = models.IntegerField()
    when = models.DateTimeField(default=timezone.now)
    where = models.CharField(max_length=128, default="")
    why = models.CharField(max_length=128, default="")
    poslat = models.DecimalField(max_digits=10, decimal_places=7, default=0)
    poslon = models.DecimalField(max_digits=10, decimal_places=7, default=0)
    START_OR_END = (
        ('1', 'start'),
        ('2', 'end'),
    )
    type = models.CharField(max_length=1, choices=START_OR_END)

    def __str__(self):
        return "%s-%s-%s" % (self.vehicle, self.odometer, self.when)

def raw_media_file_name(instance,filename):
    basename, ext = os.path.splitext(filename)
    f = str(uuid4())
    return os.path.join(f[0:1],f[1:2],f + ext.lower())

class OdometerImage(models.Model):
    driver = models.ForeignKey('auth.User')
    odometersnap = models.OneToOneField('OdometerSnap', unique=True)
    imagefile = models.FileField(upload_to=raw_media_file_name, blank=False)

    def __str__(self):
        return "%s" % self.imagefile.name

class RegisterCode(models.Model):
    phone = models.IntegerField()
    code = models.IntegerField()
    when = models.DateTimeField(default=timezone.now)

class Invoice(models.Model):
    link_id = models.CharField(max_length=64, blank=False, unique=True, default=None)
    customer = models.ForeignKey('auth.User', null=False)
    customer_name = models.CharField(max_length=128, default="")
    customer_address = models.CharField(max_length=128, default="")
    scope_from = models.DateTimeField(default=timezone.now)
    scope_to = models.DateTimeField(default=timezone.now() + timedelta(days=365))
    invoicedate = models.DateTimeField(default=timezone.now)
    duedate = models.DateTimeField(default=timezone.now() + timedelta(days=10))
    specification = models.CharField(max_length=128, default="")
    amount = models.DecimalField(max_digits=10, decimal_places=2, default=223.2)
    invoice_number = models.IntegerField(null=False, unique=True, default=None)
    is_paid = models.BooleanField(default=False)

    def __str__(self):
        return self.link_id

    def amount_vatincl(self):
        return float(self.amount) * 1.25
