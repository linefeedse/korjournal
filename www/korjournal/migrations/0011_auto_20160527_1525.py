# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import korjournal.models
import django.utils.timezone


class Migration(migrations.Migration):

    dependencies = [
        ('korjournal', '0010_auto_20160520_0827'),
    ]

    operations = [
        migrations.CreateModel(
            name='RegisterCode',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, verbose_name='ID', serialize=False)),
                ('phone', models.IntegerField()),
                ('code', models.IntegerField()),
                ('when', models.DateTimeField(default=django.utils.timezone.now)),
            ],
        ),
        migrations.AlterField(
            model_name='odometerimage',
            name='imagefile',
            field=models.FileField(upload_to=korjournal.models.raw_media_file_name),
        ),
    ]
