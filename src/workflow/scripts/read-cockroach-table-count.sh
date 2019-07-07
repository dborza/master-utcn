#!/usr/bin/env bash

DEVICE_ROWS=`cockroach sql --insecure --user=${COCKROACH_USER} --host=${COCKROACH_HOST} --port=${COCKROACH_PORT} --database=${COCKROACH_DB} --execute="select count(*) from device;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cockroach sql --insecure --user=${COCKROACH_USER} --host=${COCKROACH_HOST} --port=${COCKROACH_PORT} --database=${COCKROACH_DB} --execute="select count(*) from sensor;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cockroach sql --insecure --user=${COCKROACH_USER} --host=${COCKROACH_HOST} --port=${COCKROACH_PORT} --database=${COCKROACH_DB} --execute="select count(*) from measurement;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS