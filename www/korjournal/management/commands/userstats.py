from django.core.management.base import BaseCommand, CommandError
from django.contrib.auth.models import User
from korjournal.models import OdometerSnap
from korjournal.model.invoice import Invoice

from datetime import timedelta

class Command(BaseCommand):
    help = 'Show users' 

    def handle(self, *args, **options):
            try:
                users = User.objects.all()
            except:
                raise CommandError('No users!')

            self.stdout.write("There are %d users" % len(users))

            for user in users:
                try:
                    user_odosnaps = OdometerSnap.objects.filter(driver=user).order_by('when')
                    needthree = user_odosnaps[2]
                    firstsnap = user_odosnaps[0]
                    lastsnap = user_odosnaps[len(user_odosnaps) - 1]
                    if (firstsnap.when + timedelta(days=3) > lastsnap.when):
                        continue

                    self.stdout.write("User %s: %d snaps" % (user.username, len(user_odosnaps)))
                    self.stdout.write("First odosnap: %s" % firstsnap.when)
                    self.stdout.write("Last odosnap: %s" % lastsnap.when)

                    last_invoice = Invoice.objects.filter(customer=user).order_by('-duedate')[0]
                    self.stdout.write(self.style.WARNING("Invoice due date: %s" % last_invoice.duedate))

                except IndexError:
                    pass

