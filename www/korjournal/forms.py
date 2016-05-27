from django.forms import ModelForm, Textarea, Form, ChoiceField
from korjournal.models import OdometerSnap, OdometerImage

class DeleteOdoSnapForm(ModelForm):
    class Meta:
        model = OdometerSnap
        fields = ['id']

class DeleteOdoImageForm(ModelForm):
    class Meta:
        model = OdometerImage
        fields = ['id']

class YearVehicleForm(Form):
    VEHICLES = (
        ('none', 'Inget fordon'),
    )
    YEARS = (
        ('2016','2016'),
        ('2017','2017'),
        ('2018','2018'),
    )

    vehicle = ChoiceField(label="Fordon",choices=VEHICLES,required=True)
    year = ChoiceField(label="År",choices=YEARS,required=True)

    def __init__(self, *args, **kwargs):
        vehicles = kwargs.pop("vehicles",None)
        super(YearVehicleForm, self).__init__(*args, **kwargs)
        if vehicles:
            self.fields['vehicle'].choices = vehicles
