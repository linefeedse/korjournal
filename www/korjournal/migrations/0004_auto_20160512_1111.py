# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0003_auto_20160511_1239'),
    ]

    operations = [
        migrations.AlterField(
            model_name='vehicle',
            name='name',
            field=models.CharField(max_length=64, unique=True),
        ),
    ]
