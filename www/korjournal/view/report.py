from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseNotFound
from django.contrib.auth.decorators import login_required
from django.utils.decorators import method_decorator
from django.utils import timezone
from datetime import timedelta
from dateutil import tz, parser
from korjournal.models import Vehicle, Driver, OdometerSnap, OdometerImage, RegisterCode
from korjournal.serializers import UserSerializer, GroupSerializer, VehicleSerializer, OdometerSnapSerializer, OdometerImageSerializer, RegisterCodeSerializer, DriverSerializer
from korjournal.permissions import IsOwner, AnythingGoes, DenyAll, IsDriver
from korjournal.forms import DeleteOdoSnapForm, YearVehicleForm, DeleteOdoImageForm, RegistrationForm, VerificationForm, DeleteVehicleForm, DeleteDriverForm
import copy
import csv
from django.core.exceptions import ObjectDoesNotExist

@login_required(login_url='/login?next=/report')
def report(request):
    year = '2016'
    message = "Välj fordon och år"
    my_vehicles = Vehicle.objects.filter(owner=request.user)
    export_csv = False

    vehicle_choices = ()
    for v in my_vehicles:
        vehicle_choices = vehicle_choices + ((v.id,v.name),)

    if request.method == 'POST':
        message = "0 resor"
        form = YearVehicleForm(request.POST,vehicles=vehicle_choices)
        if form.is_valid():
            vehicle_id = form.cleaned_data['vehicle']
            year = form.cleaned_data['year']
            if request.POST.get('export'):
                export_csv=True

    else:
        form = YearVehicleForm(vehicles=vehicle_choices)

    try:
        selected_vehicle = Vehicle.objects.filter(pk=vehicle_id)[0]
        odo_snap_list = OdometerSnap.objects.filter(vehicle__owner=request.user,vehicle=selected_vehicle,when__year=year).order_by('when');
    except (IndexError,UnboundLocalError):
        odo_snap_list = OdometerSnap.objects.none()

    trips = []
    t = {}
    for snap in odo_snap_list:

        if (snap.type == "1"):
            t['startid'] = snap.id
            t['startodo'] = snap.odometer
            t['startwhere'] = snap.where
            t['startwhen'] = snap.when
            try:
                t['startimage'] = snap.odometerimage.imagefile
            except ObjectDoesNotExist:
                t['startimage'] = None

        if (snap.type == "2"):
            t['endodo'] = snap.odometer
            t['endwhere'] = snap.where
            t['endwhen'] = snap.when
            t['reason'] = snap.why

            t['km'] = 0
            try:
                if (t['endodo'] - t['startodo'] > 0):
                    t['km'] = int(t['endodo']) - int(t['startodo'])
            except KeyError:
                pass

            try:
                t['endimage'] = snap.odometerimage.imagefile
            except ObjectDoesNotExist:
                t['endimage'] = None

            try:
                if (t['startid']):
                   trips.append(copy.deepcopy(t))
            except KeyError:
                pass

            # Should the next position be another end, we put in this position as start
            t['startid'] = snap.id
            t['startodo'] = snap.odometer
            t['startwhere'] = snap.where
            t['startwhen'] = snap.when
            try:
                t['startimage'] = snap.odometerimage.imagefile
            except ObjectDoesNotExist:
                t['startimage'] = None
            message = "%d resor" % len(trips)

    context = { 'reports': trips, 'year_vehicle_form': form, 'year': year, 'summary': message }
    if (export_csv == False):
        return render(request, 'korjournal/report.html', context)

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="' + selected_vehicle.name + '_' + year + '.csv"'

    writer = csv.writer(response)
    writer.writerow([
        'Datum',
        'Start km',
        'Slut km',
        'Km',
        'Från',
        'Till',
        'Syfte'
    ])
    tzsweden = tz.gettz("Europe/Stockholm")
    for trip in trips:
        writer.writerow([
            trip['endwhen'].astimezone(tzsweden).strftime('%Y-%m-%d'),
            trip['startodo'],
            trip['endodo'],
            trip['km'],
            trip['startwhere'],
            trip['endwhere'],
            trip['reason']
        ])

    return response
