# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0006_auto_20160516_1353'),
    ]

    operations = [
        migrations.AlterField(
            model_name='odometersnap',
            name='type',
            field=models.CharField(max_length=1, choices=[('1', 'start'), ('2', 'end')]),
        ),
    ]
