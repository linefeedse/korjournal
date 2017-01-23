from django import forms
from django.utils.safestring import mark_safe
from django.conf import settings
from korjournal.recaptcha import captcha

class ReCaptcha(forms.widgets.Widget):
    recaptcha_challenge_name = 'recaptcha_challenge_field' # unused
    recaptcha_response_name = 'g-recaptcha-response'

    def render(self, name, value, attrs=None):
        return mark_safe(u'%s' % captcha.autorender(settings.RECAPTCHA_PUBLIC_KEY))

    def value_from_datadict(self, data, files, name):
        return [data.get(self.recaptcha_challenge_name, None), 
            data.get(self.recaptcha_response_name, None)]