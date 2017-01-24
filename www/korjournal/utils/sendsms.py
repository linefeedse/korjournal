from os import getenv
from requests_oauthlib import OAuth1Session
import requests
import logging

def send_sms(phone, message):
    key = getenv('SMS_GATEWAY_KEY')
    secret = getenv('SMS_GATEWAY_SECRET')
    gwapi = OAuth1Session(key, client_secret=secret)
    req = {
        'recipients': [{'msisdn': int('46%d' % int(phone))}],
        'message': message,
        'sender': 'KmKoll',
    }
    # res = gwapi.post('https://gatewayapi.com/rest/mtsms', json=req)
    logger = logging.getLogger(__name__)
    # if (res.status_code != requests.codes.ok):
    #     logger.error('Return code %d from sms gateway' % res.status_code)
    #     die
    logger.error(u"""Sent message "%(msg)s" to %(phone)d""" % { "msg": message.encode('utf-8'), "phone": int(phone)})