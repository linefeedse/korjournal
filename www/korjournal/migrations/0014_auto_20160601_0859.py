# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0013_auto_20160601_0833'),
    ]

    operations = [
        migrations.AlterField(
            model_name='vehicle',
            name='owner',
            field=models.ForeignKey(null=True, to=settings.AUTH_USER_MODEL, default=None),
        ),
    ]
