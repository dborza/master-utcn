#!/usr/bin/env bash
DEVICE_ROWS=`cockroach sql --insecure --user=root --host=localhost --port=26257 --database=master --execute="select count(*) from device;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cockroach sql --insecure --user=root --host=localhost --port=26257 --database=master --execute="select count(*) from sensor;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cockroach sql --insecure --user=root --host=localhost --port=26257 --database=master --execute="select count(*) from measurement;" | sed -n 2p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS

mvn clean install && ycsb load jdbc -P workloads/workload1 \
-p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root \
-p device_rows=$DEVICE_ROWS -p sensor_rows=$SENSOR_ROWS -p measurement_rows=$MEASUREMENT_ROWS \
-threads 10 -cp ./target/core-0.16.0-SNAPSHOT.jar