from django.conf.urls import url, include
from rest_framework import routers
from korjournal import views

from . import views

router = routers.DefaultRouter()
router.register(r'users', views.UserViewSet)
router.register(r'groups', views.GroupViewSet)
router.register(r'odometersnap', views.OdometerSnapViewSet, "odometersnap")
router.register(r'odometerimage', views.OdometerImageViewSet, "odometerimage")
router.register(r'vehicle', views.VehicleViewSet, "vehicle")

urlpatterns = [
    url(r'^$', views.landing, name='landing'),
    url(r'^editor/$', views.editor, name='editor'),
    url(r'^editor/(?P<odo_snap_id>[0-9]+)/delete$', views.delete_odo_snap, name='delete_odo_snap'),
    url(r'^editor/(?P<odo_image_id>[0-9]+)/deleteimage$', views.delete_odo_image, name='delete_odo_image'),
    url(r'^report/$', views.report, name='report'),
    url(r'^api/', include(router.urls)),
    url(r'^api-auth/$', include('rest_framework.urls', namespace='rest_framework')),
    url(r'^login/', 'django.contrib.auth.views.login', name='login'),
    url(r'^logout/', 'django.contrib.auth.views.logout', name='logout'),
]

