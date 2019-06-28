#!/usr/bin/env bash

sh ./create-cassandra-schema.sh

source ./read-cassandra-table-count.sh

ycsb load cassandra-cql -P ../workloads/workload1 \
-p hosts="localhost" -p cassandra.keyspace="master" \
-p device_rows=$DEVICE_ROWS -p sensor_rows=$SENSOR_ROWS -p measurement_rows=$MEASUREMENT_ROWS \
-threads 10 -cp ./../target/core-0.16.0-SNAPSHOT.jar