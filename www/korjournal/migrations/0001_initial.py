# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import django.utils.timezone
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('auth', '0006_require_contenttypes_0002'),
    ]

    operations = [
        migrations.CreateModel(
            name='OdometerSnap',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, serialize=False, primary_key=True)),
                ('odometer', models.IntegerField()),
                ('when', models.DateTimeField(default=django.utils.timezone.now)),
                ('owner', models.ForeignKey(to='auth.Group')),
                ('uploadedby', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.CreateModel(
            name='Vehicle',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, serialize=False, primary_key=True)),
                ('name', models.CharField(default='', max_length=64)),
                ('group', models.ForeignKey(to='auth.Group')),
            ],
        ),
        migrations.AddField(
            model_name='odometersnap',
            name='vehicle',
            field=models.ForeignKey(to='korjournal.Vehicle'),
        ),
    ]
