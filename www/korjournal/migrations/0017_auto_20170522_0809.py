# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
from django.utils.timezone import utc
import datetime


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0016_auto_20170419_0918'),
    ]

    operations = [
        migrations.AddField(
            model_name='odometerimage',
            name='guess0',
            field=models.IntegerField(default=0, blank=True),
        ),
        migrations.AddField(
            model_name='odometerimage',
            name='guess1',
            field=models.IntegerField(default=0, blank=True),
        ),
        migrations.AddField(
            model_name='odometerimage',
            name='guess2',
            field=models.IntegerField(default=0, blank=True),
        ),
        migrations.AddField(
            model_name='odometerimage',
            name='guess3',
            field=models.IntegerField(default=0, blank=True),
        ),
        migrations.AddField(
            model_name='odometerimage',
            name='guess4',
            field=models.IntegerField(default=0, blank=True),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='duedate',
            field=models.DateTimeField(default=datetime.datetime(2017, 6, 5, 8, 9, 10, 642413, tzinfo=utc)),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='scope_to',
            field=models.DateTimeField(default=datetime.datetime(2018, 5, 22, 8, 9, 10, 642368, tzinfo=utc)),
        ),
    ]
