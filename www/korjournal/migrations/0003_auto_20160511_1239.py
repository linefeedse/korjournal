# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0002_auto_20160511_1213'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='odometersnap',
            name='geopos',
        ),
        migrations.AddField(
            model_name='odometersnap',
            name='poslat',
            field=models.DecimalField(decimal_places=7, max_digits=10, default=0),
        ),
        migrations.AddField(
            model_name='odometersnap',
            name='poslon',
            field=models.DecimalField(decimal_places=7, max_digits=10, default=0),
        ),
    ]
