from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseNotFound
from rest_framework import viewsets, permissions, filters
from rest_framework.decorators import api_view, permission_classes
from korjournal.models import OdometerSnap, OdometerImage
from korjournal.serializers import OdometerSnapSerializer, OdometerImageSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll, IsDriver
from django.core.exceptions import ObjectDoesNotExist
from django.views.decorators.csrf import csrf_exempt
from django.db.models import Q
from django.http.request import RawPostDataException
from django.db import IntegrityError
from django.utils import timezone
from datetime import timedelta
from dateutil import tz, parser
import cv2
import subprocess
import sys
import os

class OdometerImageViewSet(viewsets.ModelViewSet):
    serializer_class = OdometerImageSerializer
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,IsOwner,IsDriver)

    def runtess(self, imgfile):
        ocr = subprocess.run(["/usr/bin/tesseract", imgfile, "stdout", "nobatch", "digits"], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, universal_newlines=True).stdout
        try:
            newodokm = int(ocr.replace(" ",""))
            return newodokm
        except ValueError:
            return 0

    def do_ocr(self, imgfile, lim_max, lim_min = 0):
        img = cv2.imread(imgfile,0)
        height, width = img.shape
        x1 = 0
        y1 = 0
        xleft = int(width * 0.17)
        xright = int(width * 0.83)
        ybottom = int(height * 0.83)
        ytop = int(height * 0.17)
        xmiddle1 = int(width*0.07)
        xmiddle2 = int(width*0.93)
        ymiddle1 = int(height*0.07)
        ymiddle2 = int(height*0.93)
        x2 = width
        y2 = height

        crops = [
            [y1, ybottom, xleft, x2],
            [ymiddle1, ymiddle2, xleft, x2],
            [ytop, y2, xleft, x2],
            [ytop, y2, x1, xright],
            [ymiddle1, ymiddle2, x1, xright],
            [y1, ybottom, x1, xright],
            [ymiddle1, ymiddle2, xmiddle1, xmiddle2]
        ]

        guesses = [0,0,0,0,0]

        bestguess = self.runtess(imgfile)
        guesses[2] = bestguess
        cropnumber = 0
        for crop in crops:

            y1 = crop[0]
            y2 = crop[1]
            x1 = crop[2]
            x2 = crop[3]
            cropped = img[y1:y2, x1:x2]
            filename = "/tmp/ocrthis" + str(os.getpid()) + ".png"
            cv2.imwrite(filename, cropped)
            guess = self.runtess(filename)
            os.unlink(filename)
            if guess == 0 or guess in guesses:
                continue
            if guess < lim_min:
                if guesses[1] < guess:
                    guesses[0] = guesses[1]
                guesses[1] = guess
                continue
            if guess > lim_max:
                if guesses[3] > guess:
                    guesses[4] = guesses[3]
                guesses[3] = guess
                continue
            if guess == bestguess:
                if guesses[2] > bestguess:
                    guesses[4] = guesses[3]
                    guesses[3] = guesses[2]
                    guesses[2] = guess
                if guesses[2] < bestguess and guesses[2] != 0:
                    guesses[0] = guesses[1]
                    guesses[1] = guesses[2]
                    guesses[2] = guess
                continue
            if guess > bestguess:
                bestguess = guess
                if guesses[2] > 0:
                    guesses[0] = guesses[1]
                    guesses[1] = guesses[2]
                guesses[2] = guess
                continue
            if guess < bestguess:
                if guesses[1] > 0:
                    guesses[0] = guesses[1]
                guesses[1] = guess
        if guesses[2] == 0:
            if guesses[1] > 0:
                guesses[2] = guesses[1]
                guesses[1] = guesses[0]
                guesses[0] = 0
            elif guesses[3] > 0:
                guesses[2] = guesses[3]
                guesses[3] = guesses[4]
                guesses[4] = 0
        return guesses

    def perform_create(self,serializer):
        imgfile = self.request.FILES.get('imagefile')
        odoimage = serializer.save(driver=self.request.user, imagefile=imgfile)
        lim_min = 0
        lim_max = 9999999
        # From the last three non-null odometers, pick the second largest odometer,
        # this is our MIN
        # From the MIN date, calculate reasonable kilometers until today,
        # this is our MAX
        try:
            last_odometers = OdometerSnap.objects.filter(
                vehicle=odoimage.odometersnap.vehicle,odometer__gt=0).order_by('-when')[:3]
            prev_odometers = OdometerSnap.objects.filter(
                vehicle=odoimage.odometersnap.vehicle,when__gt=last_odometers[2].when).order_by('-odometer')[:2]
            lim_min = prev_odometers[1].odometer

            since_days = timezone.now() - prev_odometers[1].when
            max_km_per_day = 300
            lim_max = prev_odometers[1].odometer + since_days.days * max_km_per_day + max_km_per_day
        except IndexError:
            pass
        guesses = [0,0,0,0,0]
        if (odoimage.odometersnap.odometer < 1):
            guesses = self.do_ocr("/vagrant/www/media/" + odoimage.imagefile.name, lim_max, lim_min)
            odoimage.odometersnap.odometer = guesses[2]
            odoimage.odometersnap.save()
        odoimage.guess0 = guesses[0]
        odoimage.guess1 = guesses[1]
        odoimage.guess2 = guesses[2]
        odoimage.guess3 = guesses[3]
        odoimage.guess4 = guesses[4]
        odoimage.save()

    def get_queryset(self):
        return OdometerImage.objects.filter(Q(odometersnap__vehicle__owner=self.request.user)|Q(driver=self.request.user))
