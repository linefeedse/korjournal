FROM ubuntu:16.04
MAINTAINER Tor-Ake Fransson <tor-ake.fransson@linefeed.se>

# the echo here will effectively decide machine patch level. Set to todays date
RUN apt-get update && echo 2016-05-06
RUN apt-get -y upgrade
RUN apt-get install -y software-properties-common rsync

#
# Setup locales
#
RUN locale-gen en_US.UTF-8
RUN dpkg-reconfigure locales
RUN echo "LC_CTYPE=en_US.UTF-8\nLC_ALL=en_US.UTF-8\nLANG=en_US.UTF-8\nLANGUAGE=en_US.UTF-8" | tee -a /etc/environment > /dev/null

#
# Setup Timezone
#
RUN rsync --itemize-changes --checksum --copy-links /usr/share/zoneinfo/Europe/Stockholm /etc/localtime
RUN echo "Europe/Stockholm" > /etc/timezone
RUN dpkg-reconfigure --frontend noninteractive tzdata

# Nginx
#
RUN add-apt-repository -y ppa:nginx/stable
RUN apt-key adv --recv-keys --keyserver hkp://keyserver.ubuntu.com 00A6F0A3C300EE8C
RUN apt-get update && apt-get -y install nginx
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

# Pip
#
RUN apt-get install -y python3-dev python3-setuptools python3-pip

# Django
#
RUN apt-get -y install python3-django python3-django-uwsgi uwsgi-plugin-python3 python3-djangorestframework
RUN mkdir /var/log/uwsgi/

# Flask
#
RUN apt-get -y install python3-flask

# Pymysql
#
RUN apt-get -y install python3-pymysql

# Supervisor and conf
#
RUN apt-get install -y supervisor
ADD ./conf/nginx-app.conf /etc/nginx/sites-enabled/nginx-app.conf
ADD ./conf/supervisor-app.conf /etc/supervisor/conf.d/supervisor-app.conf
RUN rm /etc/nginx/sites-enabled/default

RUN apt-get -y install less

EXPOSE 80
CMD ["supervisord", "-n"]
