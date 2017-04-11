FROM ubuntu:16.04
MAINTAINER Tor-Ake Fransson <tor-ake.fransson@linefeed.se>

# the echo here will effectively decide machine patch level. Set to todays date
RUN apt-get update && echo 2017-04-10
RUN apt-get -y upgrade
RUN apt-get install -y software-properties-common rsync less

#
# Setup locales
#
RUN locale-gen en_US.UTF-8
RUN dpkg-reconfigure locales
RUN echo "LC_CTYPE=en_US.UTF-8\nLC_ALL=en_US.UTF-8\nLANG=en_US.UTF-8\nLANGUAGE=en_US.UTF-8" | tee -a /etc/environment > /dev/null

#
# Setup Timezone
#
RUN rsync --itemize-changes --checksum --copy-links /usr/share/zoneinfo/Europe/Stockholm /etc/localtime && echo "Europe/Stockholm" > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata

# Nginx
#
RUN add-apt-repository -y ppa:nginx/stable && apt-key adv --recv-keys --keyserver hkp://keyserver.ubuntu.com 00A6F0A3C300EE8C
RUN apt-get update && apt-get -y install nginx && echo "daemon off;" >> /etc/nginx/nginx.conf
RUN openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048

# Pip, Django and Flask
#
RUN apt-get install -y python3-dev python3-setuptools python3-pip python3-dateutil && apt-get -y install python3-django python3-django-uwsgi uwsgi-plugin-python3 python3-djangorestframework && mkdir /var/log/uwsgi/ && apt-get -y install python3-flask && apt-get -y install python3-pymysql python3-requests-oauthlib && pip3 install django-bootstrap3

# Tesseract OCR
#
RUN apt-get install -y tesseract-ocr && echo tessedit_char_whitelist 0123456789 > /usr/share/tesseract-ocr/tessdata/configs/digits

# OpenCV for image processing
#
RUN apt-get -y install build-essential cmake pkg-config unzip python3-numpy curl
# must make all in one go for image to not be huge
RUN curl -s -O https://codeload.github.com/opencv/opencv/zip/3.2.0 && unzip 3.2.0 && mkdir buildcv && cd buildcv && cmake -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=/usr/local ../opencv-3.2.0 && make -j3 && make install/strip && cd .. && rm -rf buildcv opencv-3.2.0

# Supervisor and conf
#
RUN apt-get install -y supervisor
ADD ./conf/nginx-app.conf /etc/nginx/sites-enabled/nginx-app.conf
ADD ./conf/supervisor-app.conf /etc/supervisor/conf.d/supervisor-app.conf
ADD ./conf/nginx-selfsigned.crt /etc/ssl/certs/kilometerkoll_se.crt
ADD ./conf/nginx-selfsigned.key /etc/ssl/private/kilometerkoll_se.key
RUN rm /etc/nginx/sites-enabled/default

# Let's encrypt
# 
RUN add-apt-repository -y ppa:certbot/certbot && apt-get update && apt-get install -y certbot

ADD ./www /vagrant/www

RUN sed -i 's/DEBUG = True/DEBUG = False/' /vagrant/www/app/settings.py

EXPOSE 80 443
CMD ["supervisord", "-n"]
