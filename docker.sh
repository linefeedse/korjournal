#!/bin/bash -e

dockerhost=$(ip addr list docker0 | sed -n 's,.*inet \([^/]*\)/.*,\1,p')
dockerhn=vagrant
cd /vagrant
docker build -t korjournal:latest .
docker kill korjournal 2>/dev/null||true
docker rm korjournal 2>/dev/null||true
docker run --name=korjournal --hostname=korjournal --restart=always --env-file=/etc/docker/envfile.txt -d -v /vagrant:/vagrant -p 80:80 -p 443:443 --add-host=db:$dockerhost korjournal:latest
docker logs korjournal
