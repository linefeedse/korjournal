import locale, random, re
from rest_framework import viewsets, permissions, renderers
from rest_framework.exceptions import PermissionDenied
from django.http import HttpResponseNotFound
from korjournal.model.invoice import Invoice
from korjournal.serializers import InvoiceSerializer
from korjournal.permissions import IsAdminOrPartialUpdate
from uuid import uuid4


def new_invoice_number(previous_invoice_number):
    new_number = previous_invoice_number + random.randint(10, 99)
    s = str(new_number)
    digits = [int(d) for d in re.sub(r'\D', '', s)]

    # Luhn-algoritmen
    digits_nocheck = digits[0:len(digits)-2]
    even_digitsum = sum(x if x < 5 else x - 9 for x in digits[::2])
    check_digit = sum(digits, even_digitsum) % 10
    last_digit = 10 - check_digit if check_digit else 0

    new_number = new_number - (new_number % 10) + last_digit
    return new_number

class InvoiceViewSet(viewsets.ModelViewSet):
    serializer_class = InvoiceSerializer
    permission_classes = (IsAdminOrPartialUpdate,)
    renderer_classes = (renderers.JSONRenderer,)
    queryset = Invoice.objects.all().order_by('-invoice_number')

    def perform_create(self, serializer):
        new_link_id = str(uuid4())
        try:
            previous_invoice = Invoice.objects.latest('invoice_number')
            previous_invoice_number = previous_invoice.invoice_number
        except Invoice.DoesNotExist:
            previous_invoice_number = 1500
        serializer.save(link_id=new_link_id[0:7], invoice_number=new_invoice_number(previous_invoice_number))

    def partial_update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        try:
            check_correct_link_id = Invoice.objects.filter(pk=kwargs['pk'],link_id=request.data['link_id'])[0]
        except IndexError:
            raise PermissionDenied({"message":"You don't have permission to access",
                "object_id": kwargs['pk']})
        return self.update(request, *args, **kwargs)
