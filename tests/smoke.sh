#!/bin/bash
scriptdir=${0%/*}

if [ -n "$1" ] ; then
    host=$1
else
    host=192.168.50.4
fi
admin=admin:password123
api=http://$host/api
header1="Content-Type: application/json"
header2="Accept: application/json; indent=4"

make_groups() {
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "ABC123" }' -u $admin $api/groups/ >/dev/null
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "DEF456" }' -u $admin $api/groups/ >/dev/null
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "GHI789" }' -u $admin $api/groups/ >/dev/null
}

make_users() {
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "abc", "groups": [ "'$api'/groups/1/" ], "password": "123" }' -u $admin $api/users/ >/dev/null

#curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "def", "groups": [ "'$api'/groups/2/" ], "password": "456" }' -u $admin $api/users/ >/dev/null
#curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "ghi", "groups": [ "'$api'/groups/3/" ], "password": "789" }' -u $admin $api/users/ >/dev/null
}

make_vehicles() {
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "ABC123", "group": "'$api'/groups/1/" }' -u $admin $api/vehicle/ >/dev/null
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "DEF456", "group": "'$api'/groups/2/" }' -u $admin $api/vehicle/ >/dev/null
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "GHI789", "group": "'$api'/groups/3/" }' -u $admin $api/vehicle/ >/dev/null
}

test_odosnap_simple() {
	testodo=`date +%s`
	#
	echo -n "Testing post of odometer $testodo..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'$testodo'", "vehicle": "'$api'/vehicle/1/", "poslat": "59.3325800", "poslon": "18.0649000", "where": "kurrekurreduttgatan \"3\", 12345 Ingalunda",  "type": "1"}' -u abc:123 $api/odometersnap/ >/dev/null
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].odometer' | grep $testodo && echo "OK"
	#curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'$testodo'", "vehicle": "'$api'/vehicle/1/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u abc:123 $api/odometersnap/ >/dev/null
	#curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].odometer' | grep $testodo && echo "OK"
}

dump_snaps() {
	echo dumping all snaps for abc:123
	echo all
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/
	echo days=5
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/?days=5
	echo days=6
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/?days=6'&'ordering=when
}

upload_odoimage() {
	lastsnap=$(curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].url'|tail -1)
	lastsnap=$(eval echo $lastsnap)
	echo -n "Uploading image for lastsnap..."
	curl -X POST -s -H "$header2" -u "abc:123" -F "imagefile=@$scriptdir/odometerimage.jpg;type=image/jpg" -F 'odometersnap='$lastsnap $api/odometerimage/ | jq '.imagefile'
}

test_ocr() {
	if [ -z "$1" ]; then
		imgfile=45678.jpg
	else
		imgfile=$1
	fi
	echo "Testing ocr.."
	lastsnap=$(curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'0'", "vehicle": "'$api'/vehicle/1/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u abc:123 $api/odometersnap/ | jq '.url')
	lastsnap=$(eval echo $lastsnap)
	curl -X POST -s -H "$header2" -u "abc:123" -F "imagefile=@$scriptdir/$imgfile;type=image/jpg" -F 'odometersnap='$lastsnap $api/odometerimage/ #| jq '.imagefile'
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].odometer'|tail -1
}

add_driver() {
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "user": "0707354449", "vehicle": 1}' -u abc:123 $api/driver/ #>/dev/null
}

#make_vehicles
for i in *.jpg ; do test_ocr $i ; done
