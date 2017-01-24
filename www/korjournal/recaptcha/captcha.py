""" 
This captcha client has been modified to use siteverify api "I am not a robot"
"""

import urllib.request, urllib.error, urllib.parse, json

API_SSL_SERVER  = "https://www.google.com/recaptcha/api"
API_SERVER      = "http://www.google.com/recaptcha/api"


class RecaptchaResponse(object):
    """ The response returned from the reCAPTCHA server, indicating 
        if the submitted reCAPTCHA was correct or not.
        
        
        _____Attributes_____
        
        is_valid   - True/False, incidating if reCAPTCHA was successful or not.
        error_code - If |is_valid| is False, an error code string.
        
        
        _____Discussion_____
        
        An instance of this object is returned by submit(). You can 
        check its attributes to see if the reCAPTCHA was successful. 
        There is no need for you to create an instance of this object 
        yourself.
        
        Error codes are documented here, and may change in the future: 
        http://code.google.com/apis/recaptcha/docs/verify.html
        
        If you pass your own custom |error| to displayhtml(), it will 
        appear here instead of reCAPTCHA's error code.
    """
    
    def __init__(self, is_valid, error_code=None):
        self.is_valid   = is_valid
        self.error_code = error_code
    
def autorender(public_key):
    html_values = {
        'PublicKey':  public_key, 
    }
    html = """<div class="g-recaptcha" data-sitekey="%(PublicKey)s"></div>""" % html_values
    return html

def submit(recaptcha_response_field, private_key, remoteip, use_ssl=False, timeout_seconds=None):
    """ Submits a reCAPTCHA request for verification. Returns a 
        RecaptchaResponse object containing info if the request 
        was successful or not.
        
        
        _____Parameters_____
        
        recaptcha_response_field  - The value of recaptcha_response_field from the form.
        private_key               - Your Private reCAPTCHA API Key.
        remoteip                  - IP address of the user submitting the reCAPTCHA.
        use_ssl                   - True/False if SSL should be used or not.
        timeout_seconds           - Seconds to wait for urllib to connect to reCAPTCHA servers.
        
        
        _____Return Value_____
        
        - RecaptchaResponse.is_valid == True if successful.
        - RecaptchaResponse.is_valid == False if failure.
            - RecaptchaResponse.error_code will also be set.
        
        
        _____Discussion_____
        
        Returns RecaptchaResponse.is_valid == False if either 
        'recaptcha_response_field' or 'recaptcha_challenge_field' 
        are not provided, or are empty strings.
        
        Internally, if urllib.error.URLError is raised due to a 
        problem connecting to the reCAPTCHA server,  
        RecaptchaResponse.is_valid == False will be returned and 
        RecaptchaResponse.error_code will be set to a string 
        "urllib.error.URLError exception was raised: %s", where %s 
        contains info about the exception. This can occur if 
        the internet connection is down, the connection times 
        out, the reCAPTCHA server refuses the connection, etc...
        See the urllib docs for info.
        
        You should set |use_ssl| to True to protect your 
        |private_key|. Otherwise it's sent in plain-text to 
        the reCAPTCHA server.
        
        WARNING: Interally, urllib.request.urlopen() will not do 
        any verification of the servers certificate if SSL is 
        used. This is a limitation of the API.
    """
    
    if not (recaptcha_response_field and len(recaptcha_response_field)):
        return RecaptchaResponse(is_valid=False, error_code='incorrect-captcha-sol')
    
    if use_ssl:
        server = API_SSL_SERVER
    else:
        server = API_SERVER
    
    
    post_data = {
        'secret': private_key,
        'response' :  recaptcha_response_field,
        'remoteip' :  remoteip,
    }
    
    params = urllib.parse.urlencode(post_data)
    params = params.encode('utf-8')
    
    request = urllib.request.Request(
                url     = "%s/siteverify" % server,
                data    = params,
                headers = {"Content-type":"application/x-www-form-urlencoded", "User-agent":"reCAPTCHA Python"},
            )
    
    
    try:
        http_response = urllib.request.urlopen(url=request, timeout=timeout_seconds)
    except urllib.error.URLError as e:
        error = "urllib.error.URLError exception was raised: %s" % e
        return RecaptchaResponse(is_valid=False, error_code=error)
    
    return_values = json.loads(http_response.read().decode('utf-8'))
    http_response.close()
    return_code = return_values['success']
    
    if (return_code):
        return RecaptchaResponse(is_valid=True)
    else:
        try:
            error_code = return_values['error-codes'][0]
        except KeyError:
            error_code = ""
        return RecaptchaResponse(is_valid=False, error_code=error_code)
