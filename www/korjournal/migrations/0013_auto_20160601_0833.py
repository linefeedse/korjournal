# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0012_auto_20160531_0812'),
    ]

    operations = [
        migrations.AlterUniqueTogether(
            name='driver',
            unique_together=set([('user', 'vehicle')]),
        ),
    ]
