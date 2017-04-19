# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import datetime
from django.utils.timezone import utc


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0015_invoice'),
    ]

    operations = [
        migrations.AddField(
            model_name='invoice',
            name='is_paid',
            field=models.BooleanField(default=False),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='duedate',
            field=models.DateTimeField(default=datetime.datetime(2017, 4, 29, 9, 18, 32, 644671, tzinfo=utc)),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='invoice_number',
            field=models.IntegerField(unique=True, default=None),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='link_id',
            field=models.CharField(unique=True, default=None, max_length=64),
        ),
        migrations.AlterField(
            model_name='invoice',
            name='scope_to',
            field=models.DateTimeField(default=datetime.datetime(2018, 4, 19, 9, 18, 32, 644632, tzinfo=utc)),
        ),
    ]
