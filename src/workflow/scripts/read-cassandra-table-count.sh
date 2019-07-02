#!/usr/bin/env bash

DEVICE_ROWS=`cqlsh -e "select count(*) from master.device;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cqlsh -e "select count(*) from master.sensor;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cqlsh -e "select count(*) from master.measurement;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS
