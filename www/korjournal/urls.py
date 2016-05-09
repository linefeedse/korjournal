from django.conf.urls import url, include
from rest_framework import routers
from korjournal import views

from . import views

router = routers.DefaultRouter()
router.register(r'users', views.UserViewSet)
router.register(r'groups', views.GroupViewSet)
router.register(r'odometersnap', views.OdometerSnapViewSet, "odometersnap")
router.register(r'vehicle', views.VehicleViewSet, "vehicle")

urlpatterns = [
    url(r'^$', views.landing, name='landing'),
    url(r'^api/', include(router.urls)),
    url(r'^api-auth/$', include('rest_framework.urls', namespace='rest_framework')),
]

