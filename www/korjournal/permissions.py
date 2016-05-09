from rest_framework import permissions


class IsOwner(permissions.BasePermission):
    """
    Custom permission to only allow owners of an object to see it
    """

    def has_object_permission(self, request, view, obj):
        try:
            usergroup = request.user.groups.all()[0]
        except IndexError:
            return False
        return obj.owner == usergroup

class AnythingGoes(permissions.BasePermission):

    def has_object_permission(self, request, view, obj):
        return True

class DenyAll(permissions.BasePermission):

    def has_permission(self,request,view):
        return False

    def has_object_permission(self, request, view, obj):
        return False

