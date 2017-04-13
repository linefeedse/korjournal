from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm, mm
from reportlab.platypus import Image
from reportlab.lib.colors import black
from django.http import HttpResponse
from django.utils import timezone
from dateutil import tz
import locale

def invoice(request):
    response = HttpResponse(content_type='application/pdf')
    response['Content-Disposition'] = 'attachment; filename="kilometerkoll_faktura.pdf"'
    tzsweden = tz.gettz("Europe/Stockholm")
    locale.setlocale(locale.LC_NUMERIC, "sv_SE.utf8")

    invoicedate = timezone.now().astimezone(tzsweden).strftime('%Y-%m-%d')
    customer_designation = "078912345"
    customer_name = "Kunden"
    customer_address = "Kundgatan4, 142 42 Kvisslehamn"
    specification = "Abbonemang, elektronisk körjournal"
    scope = "2017-04-01 - 2018-04-01"
    amount = 276
    vat_percent = 25
    invoice_number = 1636
    vat_regno = "SE556959142201"
    seller_name = "Linefeed AB"
    seller_address = "18432 Åkersberga"

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

    p.setFont("Helvetica", 11)
    p.drawString(11*cm, -3*cm, "Datum")
    p.drawString(14*cm, -3*cm, "Kundnr")
    p.drawString(17*cm, -3*cm, "Fakturanr")
    p.drawString(11*cm, -35*mm, invoicedate)
    p.drawString(14*cm, -35*mm, customer_designation)
    p.drawString(17*cm, -35*mm, "%d" % invoice_number)

    p.setFont("Helvetica-Bold", 11)
    p.drawString(2*cm, -40*mm, "Kundservice")
    p.setFont("Helvetica", 11)
    p.drawString(2*cm, -45*mm, "E-post: kundtjanst@kilometerkoll.se")
    p.drawString(2*cm, -50*mm, "Mina sidor: kilometerkoll.se/login/")

    p.drawString(11*cm, -45*mm, customer_name)
    p.drawString(11*cm, -50*mm, customer_address)

    p.setStrokeColor(black)
    p.line(2*cm, -6*cm, 19*cm, -6*cm)

    p.drawString(2*cm, -7*cm, specification)
    p.drawString(11*cm, -7*cm, scope)
    p.drawString(17*cm, -7*cm, "%d,00 kr" % amount)

    p.drawString(2*cm, -9*cm, "Moms %d%%" % vat_percent)
    vat_string = "%.2f kr" % (amount * vat_percent / 100)
    p.drawString(172*mm, -9*cm, vat_string.replace(".",","))

    p.setFont("Helvetica-Bold", 11)
    p.drawString(2*cm, -10*cm, "Att betala")
    to_pay = "%.02f kr" % (amount * ((100 + vat_percent) / 100))
    p.drawString(17*cm, -10*cm, to_pay.replace(".",","))

    # Close the PDF object cleanly, and we're done.
    p.showPage()
    p.save()
    return response
