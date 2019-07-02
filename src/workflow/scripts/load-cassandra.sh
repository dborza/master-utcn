#!/usr/bin/env bash

source ./read-input-args.sh

sh ./create-cassandra-schema.sh

source ./read-cassandra-table-count.sh

ycsb load cassandra-cql -P ../workloads/workload1 \
-p hosts="localhost" -p cassandra.keyspace="master" \
-p device_rows=${DEVICE_ROWS} -p sensor_rows=${SENSOR_ROWS} -p measurement_rows=${MEASUREMENT_ROWS} \
-threads ${THREADS} -cp ./../target/core-0.16.0-SNAPSHOT.jar:./../lib/slf4j-simple-1.7.26.jar