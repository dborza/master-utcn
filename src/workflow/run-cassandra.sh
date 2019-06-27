#!/usr/bin/env bash
DEVICE_ROWS=`cqlsh -e "select count(*) from master.device;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cqlsh -e "select count(*) from master.sensor;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cqlsh -e "select count(*) from master.measurement;" | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS

mvn clean install && ycsb run cassandra-cql -P workloads/workload1 -p hosts="localhost" -p cassandra.keyspace="master" \
-p device_rows=$DEVICE_ROWS -p sensor_rows=$SENSOR_ROWS -p measurement_rows=$MEASUREMENT_ROWS \
-threads 10 -cp ./target/core-0.16.0-SNAPSHOT.jar