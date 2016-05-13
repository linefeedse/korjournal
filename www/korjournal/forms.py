from django.forms import ModelForm, Textarea
from korjournal.models import OdometerSnap

class DeleteOdoSnapForm(ModelForm):
    class Meta:
        model = OdometerSnap
        fields = ['id']
