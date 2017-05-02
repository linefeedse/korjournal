from django.shortcuts import render
from django.http import HttpResponse, HttpResponseBadRequest, HttpResponseNotFound
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import authentication, permissions
from korjournal.serializers import InvoiceSerializer
from korjournal.model.invoice import Invoice
from django.core.mail import EmailMessage
from django.core.validators import validate_email
from django.core.exceptions import ValidationError
from django.template.loader import get_template
from django.template import Context
import tempfile, os
from django.utils import timezone


def view(request):
    if request.GET.get('pdf'):
        return PDF(request)
    elif request.GET.get('email'):
        return email(request)
    else:
        return preview(request)

def preview(request):
    link_id = request.GET.get('l')
    try:
        invoice = Invoice.objects.filter(link_id=link_id)[0]
    except IndexError:
        return HttpResponseNotFound('Felaktig fakturalänk')
    context = { 'invoice': invoice }
    return render(request, 'korjournal/invoice.html', context)

def PDF(request):
    link_id = request.GET.get('l')
    try:
        invoice = Invoice.objects.filter(link_id=link_id)[0]
    except IndexError:
        return HttpResponseNotFound('Felaktig fakturalänk')

    response = HttpResponse(content_type='application/pdf')
    response['Content-Disposition'] = 'attachment; filename="kilometerkoll_faktura.pdf"'
    return(invoice.renderpdf(response))

def email(request):
    link_id = request.GET.get('l')
    try:
        invoice = Invoice.objects.filter(link_id=link_id)[0]
    except IndexError:
        return HttpResponseNotFound('Felaktig fakturalänk')

    try:
        validate_email( invoice.customer_address )
    except ValidationError:
        return HttpResponseBadRequest('<h1>Epostadressen är inte giltig</h1>')

    template = get_template('mail/invoice.txt')
    context = Context({
        'customer_number': invoice.customer.username,
        'duedate': invoice.duedate.strftime('%Y-%m-%d')
        })
    content = template.render(context)
    emailMessage = EmailMessage(
            "Kilometerkoll, faktura",
            content,
            invoice.customer.username + " <noreply@kilometerkoll.se>",
            [invoice.customer_address],
            headers = {'Reply-To': 'kundtjanst@kilometerkoll.se'}
        )
    fd, pdfname = tempfile.mkstemp()
    invoice.renderpdf(pdfname)
    pdftmp = open(pdfname, 'rb')
    emailMessage.attach('faktura_kilometerkoll_' + str(invoice.invoice_number) + '.pdf',
    pdftmp.read())
    emailMessage.send()
    pdftmp.close()
    os.close(fd)
    os.remove(pdfname)
    return render(request, 'korjournal/emailsent.html', {'email': invoice.customer_address})

class InvoicesDue(APIView):
    authentication_classes = (authentication.TokenAuthentication, authentication.BasicAuthentication,)
    permission_classes = (permissions.IsAuthenticated,)

    def get(self, request, format=None):
        invoicedue = Invoice.objects.filter(customer=request.user, duedate__lt=timezone.now(), is_paid=False)
        response = {}
        invoice_summaries = []
        for invoice in invoicedue:
            invoice_summaries.append({
                "duedate": invoice.duedate,
                "link_id": invoice.link_id
                })
        if (len(invoicedue) > 0):
            response['results'] = invoice_summaries
        return Response(response)


