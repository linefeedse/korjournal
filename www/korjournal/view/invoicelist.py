from django.contrib.auth.decorators import login_required
from django.shortcuts import render
from korjournal.models import Invoice


@login_required(login_url='/login')
def invoicelist(request):
    try:
        invoices = Invoice.objects.filter(customer=request.user)
    except Invoice.DoesNotExist:
        invoices = None
    context = { 'username': request.user.username, 'invoices': invoices }
    return render(request, 'korjournal/invoicelist.html', context)