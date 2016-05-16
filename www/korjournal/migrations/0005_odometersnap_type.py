# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0004_auto_20160512_1111'),
    ]

    operations = [
        migrations.AddField(
            model_name='odometersnap',
            name='type',
            field=models.CharField(choices=[('1', 'start'), ('2', 'end')], max_length=1, default='2'),
        ),
    ]
