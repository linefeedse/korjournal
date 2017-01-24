from django.conf import settings
from django import forms
from django.utils.encoding import smart_text
from django.utils.translation import ugettext_lazy as _

from korjournal.utils.widgets import ReCaptcha
from korjournal.recaptcha import captcha

class ReCaptchaField(forms.CharField):
    default_error_messages = {
        'captcha_invalid': _(u'Ogiltig captcha')
    }

    def __init__(self, *args, **kwargs):
        self.widget = ReCaptcha
        self.required = True
        super(ReCaptchaField, self).__init__(*args, **kwargs)

    def clean(self, value):
        super(ReCaptchaField, self).clean(value)
        recaptcha_response_value = smart_text(value)
        
        """ Submits a reCAPTCHA request for verification. Returns a 
        RecaptchaResponse object containing info if the request 
        was successful or not.
        
        
        _____Parameters_____
        
        recaptcha_challenge_field - The value of recaptcha_challenge_field from the form.
        recaptcha_response_field  - The value of recaptcha_response_field from the form.
        private_key               - Your Private reCAPTCHA API Key.
        remoteip                  - IP address of the user submitting the reCAPTCHA.
        use_ssl                   - True/False if SSL should be used or not.
        timeout_seconds           - Seconds to wait for urllib to connect to reCAPTCHA servers.
        
        
        _____Return Value_____
        
        - RecaptchaResponse.is_valid == True if successful.
        - RecaptchaResponse.is_valid == False if failure.
            - RecaptchaResponse.error_code will also be set.
        """
        check_captcha = captcha.submit(recaptcha_response_value, settings.RECAPTCHA_PRIVATE_KEY, None, True, 15)
        if not check_captcha.is_valid:
            raise forms.ValidationError(self.error_messages['captcha_invalid'])
        return value