from django.contrib import admin
from .models import Vehicle, OdometerSnap, OdometerImage
from .model.invoice import Invoice


class OdometerImageInline(admin.TabularInline):
	model = OdometerImage

class OdometerSnapAdmin(admin.ModelAdmin):
	list_filter = ('driver', )
	date_hierarchy = 'when'
	inlines = ( OdometerImageInline, )

class OdometerImageAdmin(admin.ModelAdmin):
	list_filter = ('driver', )

class InvoiceAdmin(admin.ModelAdmin):
	list_filter = ('customer', 'is_paid', )
	date_hierarchy = 'duedate'

admin.site.register(Vehicle)
admin.site.register(OdometerSnap, OdometerSnapAdmin)
admin.site.register(OdometerImage, OdometerImageAdmin)
admin.site.register(Invoice, InvoiceAdmin)
