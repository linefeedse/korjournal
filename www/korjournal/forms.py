from django.forms import ModelForm, Textarea, Form, ChoiceField, CharField, EmailField
from korjournal.models import OdometerSnap, OdometerImage, Vehicle, Driver
from django.core.validators import RegexValidator
from korjournal.utils import fields as utils_fields

class DeleteOdoSnapForm(ModelForm):
    class Meta:
        model = OdometerSnap
        fields = ['id']

class DeleteOdoImageForm(ModelForm):
    class Meta:
        model = OdometerImage
        fields = ['id']

class DeleteVehicleForm(ModelForm):
    class Meta:
        model = Vehicle
        fields = ['id']

class DeleteDriverForm(ModelForm):
    class Meta:
        model = Driver
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

class RegistrationForm(Form):
    phone = CharField(error_messages={'incomplete': 'Ange ett mobiltelefonnummer'},
                      validators=[RegexValidator(r'^[0-9]{9,11}$', 'Ange ett giltigt mobiltelefonnummer')])

class VerificationForm(Form):
    phone = CharField(error_messages={'incomplete': 'Ange ett mobiltelefonnummer'},
                      validators=[RegexValidator(r'^[0-9]{9,11}$', 'Ange ett giltigt mobiltelefonnummer')])
    code = CharField(error_messages={'incomplete': 'Ange den femsiffriga koden från ditt SMS'},
                      validators=[RegexValidator(r'^[0-9]{5}$', 'Ange den femsiffriga koden från ditt SMS')])

class ContactForm(Form):
    name = CharField(required=True)
    email = EmailField(required=True)
    subject = CharField(required=True)
    message = CharField(required=True, widget=Textarea)
    recaptcha = utils_fields.ReCaptchaField()

class ApplinkForm(Form):
    phone = CharField(error_messages={'incomplete': 'Ange ett mobiltelefonnummer'},
                      validators=[RegexValidator(r'^[0-9]{9,11}$', 'Ange ett giltigt mobiltelefonnummer')])