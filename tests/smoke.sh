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

make_users() {
	echo "creating users..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "abc", "password": "123" }' -u $admin $api/users/ | jq .username
    curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "def", "password": "456" }' -u $admin $api/users/ | jq .username
    curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "ghi", "password": "789" }' -u $admin $api/users/ | jq .username
}

unauthorized_mkuser() {
	echo "Unauthorized user creation..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "username": "jkl", "password": "890" }' -u abc:123 $api/users/ 
}

make_vehicles() {
	echo "creating vehicles..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "ABC123" }' -u abc:123 $api/vehicle/ | jq .name
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "DEF 456" }' -u def:456 $api/vehicle/ | jq .name
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "name": "GHI 789" }' -u ghi:789 $api/vehicle/ | jq .name
}

assign_drivers() {
	echo "assigning driver"
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "vehicle": "1", "user": "def" }' -u abc:123 $api/driver/ | jq .id
}

query_vehicles() {
    echo "checking how many vehicles 'def' can see (should be 2)"
	curl -s -H "$header1" -H "$header2" -u def:456 $api/vehicle/ | jq .count
}

unauthorized_driver() {
	echo "Unauthorized driver assignment..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "vehicle": "3", "user": "abc" }' -u abc:123 $api/driver/
}

test_odosnap_simple() {
	testodo=`date +%s`
	#
	echo -n "Testing post of odometer $testodo..."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'$testodo'", "vehicle": "'$api'/vehicle/2/", "poslat": "59.3325800", "poslon": "18.0649000", "where": "kurrekurreduttgatan \"3\", 12345 Ingalunda",  "type": "1"}' -u def:456 $api/odometersnap/ >/dev/null
	curl -s -H "$header1" -H "$header2" -u def:456 $api/odometersnap/ | jq '.results[].odometer' | grep -q $testodo && echo "OK"
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "0", "vehicle": "'$api'/vehicle/1/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u abc:123 $api/odometersnap/ >/dev/null
	curl -s -H "$header1" -H "$header2" -u abc:123 $api/odometersnap/ | jq '.results[].odometer' | grep -q 0 && echo "OK"
}

unauthorized_odosnap() {
		echo "Unauthorized snap upload..."
		curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "0", "vehicle": "'$api'/vehicle/3/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u def:456 $api/odometersnap/
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
		imgfile=$scriptdir/45678.jpg
	else
		imgfile=$1
	fi
	echo "Testing ocr.."
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'45000'", "vehicle": "'$api'/vehicle/3/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u ghi:789 $api/odometersnap/ 
	lastsnap=$(curl -s -H "$header1" -H "$header2" -X POST -d '{ "odometer": "'0'", "vehicle": "'$api'/vehicle/3/", "poslat": "59.3325800", "poslon": "18.0659000", "where": "kurrekurreduttgatan \"5\", 12345 Ingalunda", "type": "2"}' -u ghi:789 $api/odometersnap/ | jq '.url')
	lastsnap=$(eval echo $lastsnap)
	curl -X POST -s -H "$header2" -u "ghi:789" -F "imagefile=@$imgfile;type=image/jpg" -F 'odometersnap='$lastsnap $api/odometerimage/ #| jq '.imagefile'
	curl -s -H "$header1" -H "$header2" -u ghi:789 $api/odometersnap/ | jq '.results[].odometer'|tail -1
}

add_driver() {
	curl -s -H "$header1" -H "$header2" -X POST -d '{ "user": "0707354449", "vehicle": 1 }' -u abc:123 $api/driver/ #>/dev/null
}

default_testsuite() {
	# this can be run after a /vagrant/www/manage.py flush ; /vagrant/www/manage.py createsuperuser
	make_users
	make_vehicles
	assign_drivers
    query_vehicles
	test_odosnap_simple
	echo "testing ocr accuracy"
	test_ocr | grep -q 45678 && echo OK
	unauthorized_mkuser ; echo
	unauthorized_driver ; echo
	unauthorized_odosnap ; echo
}

default_testsuite
#for i in $scriptdir/odometerimages/1????.jpg ; do test_ocr $i ; done
