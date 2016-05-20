# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        ('auth', '0006_require_contenttypes_0002'),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('korjournal', '0008_odometersnap_why'),
    ]

    operations = [
        migrations.CreateModel(
            name='OdometerImage',
            fields=[
                ('id', models.AutoField(serialize=False, primary_key=True, auto_created=True, verbose_name='ID')),
                ('imagefile', models.FileField(upload_to='')),
                ('odometersnap', models.ForeignKey(unique=True, to='korjournal.OdometerSnap')),
                ('owner', models.ForeignKey(to='auth.Group')),
                ('uploadedby', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
        ),
    ]
