from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm, mm
from reportlab.platypus import Image
from reportlab.lib.colors import black, grey, white
from django.shortcuts import render
from django.http import HttpResponse
from django.utils import timezone
from dateutil import tz
from datetime import timedelta
import locale
from django.http import HttpResponseNotFound
#from django.db import DoesNotExist
from korjournal.serializers import InvoiceSerializer
from korjournal.models import Invoice

def view(request):
    if request.GET.get('pdf'):
        return PDF(request)
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
    tzsweden = tz.gettz("Europe/Stockholm")
    locale.setlocale(locale.LC_NUMERIC, "sv_SE.utf8")

    invoicedate = invoice.invoicedate.strftime('%Y-%m-%d')
    duedate = invoice.duedate.strftime('%Y-%m-%d')
    customer_designation = invoice.customer.username
    customer_name = invoice.customer_name
    customer_address = invoice.customer_address
    specification = invoice.specification
    scope = "%s - %s" % (invoice.scope_from.strftime('%Y-%m-%d'),
        invoice.scope_to.strftime('%Y-%m-%d'))
    amount = invoice.amount
    vat_percent = 25
    invoice_number = invoice.invoice_number
    vat_regno = "SE556959142201"
    seller_name = "Linefeed AB"
    seller_address = "Ekbacksvägen 59b"
    seller_city = "18432 Åkersberga"
    seller_bankgiro = "542-0286"
    seller_iban = "SE6180000832799349770843"
    seller_orgnr = "556959-1422"

    # Create the PDF object, using the response object as its "file."
    p = canvas.Canvas(response, pagesize=A4)
    width, height = A4

    p.translate(0, height)


    # Draw things on the PDF. Here's where the PDF generation happens.
    # See the ReportLab documentation for the full list of functionality.
    p.setFont("Helvetica", 18)
    p.drawImage("/vagrant/www/static/digimad-van-300x300.png", 2*cm, -21*mm, 7*mm, 7*mm)
    p.drawString(3*cm, -2*cm, "Kilometerkoll.se")
    p.drawString(11*cm, -2*cm, "Faktura")

    p.setFont("Helvetica-Oblique", 9)
    p.drawString(11*cm, -3*cm, "Datum")
    p.drawString(14*cm, -3*cm, "Kundnr")
    p.drawString(17*cm, -3*cm, "Fakturanr")

    p.setFont("Helvetica", 11)
    p.drawString(11*cm, -35*mm, invoicedate)
    p.drawString(14*cm, -35*mm, customer_designation)
    p.drawString(17*cm, -35*mm, "%d" % invoice_number)
    p.setStrokeColor(black)
    p.rect(168*mm, -37*mm, 25*mm, 6*mm)
    p.setFont("Helvetica-Oblique", 9)
    p.drawString(11*cm, -40*mm, "Förfallodag")
    p.setFont("Helvetica-Bold", 11)
    p.drawString(11*cm, -45*mm, duedate)

    p.setFont("Helvetica-Bold", 11)
    p.drawString(2*cm, -40*mm, "Kundservice:")
    p.setFont("Helvetica", 11)
    p.drawString(2*cm, -45*mm, "E-post: kundtjanst@kilometerkoll.se")
    p.drawString(2*cm, -50*mm, "Mina sidor: kilometerkoll.se/login/")

    p.drawString(11*cm, -55*mm, customer_name)
    p.drawString(11*cm, -60*mm, customer_address)

    p.setStrokeColor(black)
    p.line(2*cm, -7*cm, 19*cm, -7*cm)

    p.drawString(2*cm, -8*cm, specification)
    p.drawString(11*cm, -8*cm, scope)
    amount_string = "%.2f kr" % amount
    p.drawString(17*cm, -8*cm, amount_string.replace(".",","))

    p.drawString(2*cm, -9*cm, "Moms %d%%" % vat_percent)
    vat_string = "%.2f kr" % (amount * vat_percent / 100)
    p.drawString(172*mm, -9*cm, vat_string.replace(".",","))

    p.setFont("Helvetica-Bold", 11)
    p.drawString(2*cm, -10*cm, "Att betala")
    to_pay = "%.02f" % (float(amount) * ((100 + vat_percent) / 100))
    p.drawString(17*cm, -10*cm, "%s kr" % to_pay.replace(".",","))

    p.setFont("Helvetica-BoldOblique", 12)

    p.rotate(12)
    p.drawString(-20*mm, -165*mm, "Obs! Inget betalningskrav. Om du inte vill använda Kilometerkoll.se, bortse från denna faktura.")
    p.rotate(-12)

    p.line(2*cm, -17*cm, 19*cm, -17*cm)

    p.setFont("Helvetica", 9)
    p.drawString(2*cm, -175*mm, seller_name)
    p.drawString(2*cm, -179*mm, seller_address)
    p.drawString(2*cm, -183*mm, seller_city)

    p.drawString(8*cm, -175*mm, "Orgnr: %s" % seller_orgnr)
    p.drawString(8*cm, -179*mm, "Momsreg. nr:")
    p.drawString(8*cm, -183*mm, vat_regno)

    p.drawString(14*cm, -175*mm, "Bankgiro: %s" % seller_bankgiro)
    p.drawString(14*cm, -179*mm, "IBAN:")
    p.drawString(14*cm, -183*mm, seller_iban)

    p.setFont("Helvetica-Oblique", 9)
    p.drawString(3*cm, -195*mm,
        "Vid betalning på annat sätt än med denna avi, t.ex. internetbank, ange fakturanumret %d som meddelande" % invoice_number)

    p.setFont("Times-Roman", 12)
    p.drawString(15*mm, -21*cm, "bankgirot")
    p.setFont("Helvetica", 11)
    p.drawString(11*cm, -21*cm, "INBETALNING/GIRERING AVI")
    p.drawString(17*cm, -21*cm, "Nr")
    p.setFont("Courier-Bold", 11)
    p.drawString(177*mm, -21*cm, str(invoice_number))

    p.setStrokeColor(black)
    p.rect(15*mm, -243*mm, 185*mm, 32*mm)
    p.rect(15*mm, -266*mm, 95*mm, 23*mm)
    p.setFont("Helvetica", 7)
    p.drawString(16*mm, -246*mm, "Betalningsavsändare")
    p.rect(110*mm, -266*mm, 90*mm, 23*mm)
    p.drawString(111*mm, -246*mm, "Betalningsmottagare")
    p.drawString(60*mm, -269*mm, "Belopp - ange alltid kronor och öre")
    p.drawString(116*mm, -269*mm, "Till bankgironummer (ifylls alltid)")
    p.rect(165*mm, -266*mm, 35*mm, 7*mm)
    p.drawString(166*mm, -261*mm, "Inbetalningsavgift")
    p.drawString(166*mm, -264*mm, "(Ifylls av banken)")

    p.setFont("Helvetica", 11)
    p.drawString(120*mm, -256*mm, seller_name)

    p.setFillGray(0.85)
    p.rect(15*mm, -280*mm, 186*mm, 10*mm, stroke=0, fill=1)
    p.setFillColor(white)
    p.rect(60*mm, -277*mm, 50*mm, 5*mm, stroke=0, fill=1)
    p.rect(116*mm, -277*mm, 45*mm, 5*mm, stroke=0, fill=1)

    p.setFillColor(black)
    p.setFont("Courier-Bold", 11)
    p.drawString(9*cm, -276*mm, to_pay)
    p.drawString(118*mm, -276*mm, seller_bankgiro)

    p.setFont("Courier", 11)
    p.drawString(15*mm, -286*mm, "#")
    p.drawString(48*mm, -286*mm, "#")
    p.drawString(170*mm, -286*mm, "%s #45#" % seller_bankgiro.replace("-",""))

    # Close the PDF object cleanly, and we're done.
    p.showPage()
    p.save()
    return response
