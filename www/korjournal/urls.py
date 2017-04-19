from django.conf.urls import url, include
from rest_framework import routers
from korjournal import views
from korjournal.view import report, invoice, invoicelist
from korjournal.viewset import odometersnap, odometerimage, invoiceviewset

from . import views

router = routers.DefaultRouter()
router.register(r'users', views.UserViewSet)
router.register(r'groups', views.GroupViewSet)
router.register(r'odometersnap', odometersnap.OdometerSnapViewSet, "odometersnap")
router.register(r'odometerimage', odometerimage.OdometerImageViewSet, "odometerimage")
router.register(r'vehicle', views.VehicleViewSet, "vehicle")
router.register(r'driver', views.DriverViewSet, "driver")
router.register(r'invoice', invoiceviewset.InvoiceViewSet, "invoice")

urlpatterns = [
    url(r'^$', views.landing, name='landing'),
    url(r'^privacy-policy', views.privacy_policy, name="privacy-policy"),
    url(r'^editor/$', views.editor, name='editor'),
    url(r'^editor/(?P<odo_snap_id>[0-9]+)/delete$', views.delete_odo_snap, name='delete_odo_snap'),
    url(r'^editor/(?P<odo_image_id>[0-9]+)/deleteimage$', views.delete_odo_image, name='delete_odo_image'),
    url(r'^report/$', report.report, name='report'),
    url(r'^invoice/', invoice.view, name='invoice'),
    url(r'^invoicelist/', invoicelist.invoicelist, name='invoicelist'),
    url(r'^api/', include(router.urls)),
    url(r'^api-auth/$', include('rest_framework.urls', namespace='rest_framework')),
    url(r'^login/', 'django.contrib.auth.views.login', name='login'),
    url(r'^logout/', 'django.contrib.auth.views.logout', name='logout'),
    url(r'^register/', views.doregister, name='register'),
    url(r'^verify/', views.verify, name='verify'),
    url(r'^accounts/profile/', views.profile, name='profile'),
    url(r'^registration_complete/', views.registration_complete, name='registration_complete'),
    url(r'^vehicles/$', views.vehicles, name='vehicles'),
    url(r'^vehicles/(?P<vehicle_id>[0-9]+)/delete$', views.delete_vehicle, name='delete_vehicle'),
    url(r'^vehicles/(?P<driver_id>[0-9]+)/deletedriver$', views.delete_driver, name='delete_driver'),
    url(r'^applink', views.applink, name='applink'),
]

