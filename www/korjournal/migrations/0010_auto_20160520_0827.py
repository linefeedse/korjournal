# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0009_odometerimage'),
    ]

    operations = [
        migrations.AlterField(
            model_name='odometerimage',
            name='odometersnap',
            field=models.OneToOneField(to='korjournal.OdometerSnap'),
        ),
    ]
