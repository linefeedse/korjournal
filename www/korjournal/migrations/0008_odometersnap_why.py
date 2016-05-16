# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0007_auto_20160516_1353'),
    ]

    operations = [
        migrations.AddField(
            model_name='odometersnap',
            name='why',
            field=models.CharField(default='', max_length=128),
        ),
    ]
