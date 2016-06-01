from rest_framework import permissions
from korjournal.models import Driver


class IsOwner(permissions.BasePermission):
    """
    Custom permission to only allow owners of an object to see it
    """

    def has_object_permission(self, request, view, obj):
        user = request.user
        try:
            if (obj.owner == user):
                return True
            else:
                return False
        except AttributeError:
            try:
                if (obj.vehicle.owner == user):
                    return True
                else:
                    return False
            except AttributeError:
                try:
                    if (obj.odometersnap.vehicle.owner == user):
                        return True
                    else:
                        return False
                except AttributeError:
                    return False
        return False

class IsDriver(permissions.BasePermission):
    """
    Custom permission to only allow drivers/uploaders  of an object to see it
    """

    def has_object_permission(self, request, view, obj):
        if IsOwner.has_object_permission(self, request, view, obj):
            return True
        user = request.user
        try:
            if (obj.driver == user):
                return True
            else:
                return False
        except AttributeError:
            try:
                driver = Driver.objects.filter(user=request.user,vehicle=obj)[0]
                return True
            except IndexError:
                return False
        return False

class AnythingGoes(permissions.BasePermission):

    def has_object_permission(self, request, view, obj):
        return True

class DenyAll(permissions.BasePermission):

    def has_permission(self,request,view):
        return False

    def has_object_permission(self, request, view, obj):
        return False

