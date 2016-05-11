# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='odometersnap',
            name='geopos',
            field=models.CharField(default='0 0', max_length=23),
        ),
        migrations.AddField(
            model_name='odometersnap',
            name='where',
            field=models.CharField(default='', max_length=128),
        ),
    ]
