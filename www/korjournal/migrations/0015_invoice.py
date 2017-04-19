# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import datetime
import django.utils.timezone
from django.conf import settings
from django.utils.timezone import utc


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('korjournal', '0014_auto_20160601_0859'),
    ]

    operations = [
        migrations.CreateModel(
            name='Invoice',
            fields=[
                ('id', models.AutoField(primary_key=True, verbose_name='ID', auto_created=True, serialize=False)),
                ('link_id', models.CharField(unique=True, max_length=64)),
                ('customer_name', models.CharField(default='', max_length=128)),
                ('customer_address', models.CharField(default='', max_length=128)),
                ('scope_from', models.DateTimeField(default=django.utils.timezone.now)),
                ('scope_to', models.DateTimeField(default=datetime.datetime(2018, 4, 19, 9, 1, 41, 212804, tzinfo=utc))),
                ('invoicedate', models.DateTimeField(default=django.utils.timezone.now)),
                ('duedate', models.DateTimeField(default=datetime.datetime(2017, 4, 29, 9, 1, 41, 212843, tzinfo=utc))),
                ('specification', models.CharField(default='', max_length=128)),
                ('amount', models.DecimalField(default=223.2, max_digits=10, decimal_places=2)),
                ('invoice_number', models.IntegerField(unique=True)),
                ('customer', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
        ),
    ]
