# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0005_odometersnap_type'),
    ]

    operations = [
        migrations.AlterField(
            model_name='odometersnap',
            name='type',
            field=models.CharField(default='2', choices=[('1', 'start'), ('2', 'end')], max_length=1),
        ),
    ]
