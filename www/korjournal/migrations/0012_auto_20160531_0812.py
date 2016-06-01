# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('korjournal', '0011_auto_20160527_1525'),
    ]

    operations = [
        migrations.CreateModel(
            name='Driver',
            fields=[
                ('id', models.AutoField(auto_created=True, serialize=False, primary_key=True, verbose_name='ID')),
                ('user', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.RenameField(
            model_name='odometerimage',
            old_name='uploadedby',
            new_name='driver',
        ),
        migrations.RenameField(
            model_name='odometersnap',
            old_name='uploadedby',
            new_name='driver',
        ),
        migrations.RemoveField(
            model_name='odometerimage',
            name='owner',
        ),
        migrations.RemoveField(
            model_name='odometersnap',
            name='owner',
        ),
        migrations.RemoveField(
            model_name='vehicle',
            name='group',
        ),
        migrations.AddField(
            model_name='vehicle',
            name='owner',
            field=models.ForeignKey(to=settings.AUTH_USER_MODEL, default=1),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='driver',
            name='vehicle',
            field=models.ForeignKey(to='korjournal.Vehicle'),
        ),
    ]
