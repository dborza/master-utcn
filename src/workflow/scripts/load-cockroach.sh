#!/usr/bin/env bash

sh ./create-cockroach-schema.sh

source ./read-cockroach-table-count.sh

ycsb load jdbc -P ../workloads/workload1 \
-p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root \
-p device_rows=$DEVICE_ROWS -p sensor_rows=$SENSOR_ROWS -p measurement_rows=$MEASUREMENT_ROWS \
-threads 10 -cp ./../target/core-0.16.0-SNAPSHOT.jar