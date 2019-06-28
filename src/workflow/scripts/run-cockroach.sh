#!/usr/bin/env bash

source ./read-cockroach-table-count.sh

ycsb run jdbc -P ../workloads/workload1 \
-p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root \
-p device_rows=${DEVICE_ROWS} -p sensor_rows=${SENSOR_ROWS} -p measurement_rows=${MEASUREMENT_ROWS} \
-threads 10 -cp ./../target/core-0.16.0-SNAPSHOT.jar:./../lib/slf4j-simple-1.7.26.jar