#!/bin/bash

if test -z "$1" ; then
    echo usage: $0 image-name
fi

image=$1
output=`basename $1`

convert $1 -resize 144x144 mipmap-xxhdpi/$output
convert $1 -resize 96x96 mipmap-xhdpi/$output
convert $1 -resize 72x72 mipmap-hdpi/$output
convert $1 -resize 48x48 mipmap-mdpi/$output
