#!/usr/bin/env bash

DEVICE_ROWS=`cqlsh -e "select count(*) from master.device;" -u ${CASSANDRA_USER} -p ${CASSANDRA_USER} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cqlsh -e "select count(*) from master.sensor;" -u ${CASSANDRA_USER} -p ${CASSANDRA_USER} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cqlsh -e "select count(*) from master.measurement;" -u ${CASSANDRA_USER} -p ${CASSANDRA_PASS} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS
