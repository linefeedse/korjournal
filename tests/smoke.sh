#!/bin/bash

if [ -n "$1" ] ; then
    host=$1
else
    host=korjournal.linefeed.se
fi
admin=admin:password123
api=http://$host/api
header1="Content-Type: application/json"
header2="Accept: application/json; indent=4"

curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "ABC123" }' -u $admin $api/groups/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "DEF456" }' -u $admin $api/groups/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "GHI789" }' -u $admin $api/groups/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "abc", "groups": [ "'$api'/groups/1/" ], "password": "123" }' -u $admin $api/users/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "def", "groups": [ "'$api'/groups/2/" ], "password": "456" }' -u $admin $api/users/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "ghi", "groups": [ "'$api'/groups/3/" ], "password": "789" }' -u $admin $api/users/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "ABC123", "group": "'$api'/groups/1/" }' -u $admin $api/vehicle/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "DEF456", "group": "'$api'/groups/2/" }' -u $admin $api/vehicle/ >/dev/null
curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "GHI789", "group": "'$api'/groups/3/" }' -u $admin $api/vehicle/ >/dev/null

testodo=`date +%s`

echo -n "Testing post of odometer $testodo..."
curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'$testodo'", "vehicle": "'$api'/vehicle/1/", "poslat": "59.3325800", "poslon": "18.0649000", "where": "kurrekurreduttgatan \"3\", 12345 Ingalunda" }' -u abc:123 $api/odometersnap/ >/dev/null
curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].odometer' | grep $testodo && echo "OK"
