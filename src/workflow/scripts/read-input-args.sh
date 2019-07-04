#!/usr/bin/env bash

# Common input args
THREADS="10"

CLASSPATH="./../target/core-0.16.0-SNAPSHOT.jar:./../lib/slf4j-simple-1.7.26.jar"

COMMON_ARGS="-P ../workloads/workload1 \
-cp ${CLASSPATH} \
-p threads=${THREADS} \
-p device_rows=${DEVICE_ROWS} -p sensor_rows=${SENSOR_ROWS} -p measurement_rows=${MEASUREMENT_ROWS}"

# Cassandra input args
CASSANDRA_HOSTS="localhost"
CASSANDRA_KEYSPACE="master"
CASSANDRA_USER="cassandra"
CASSANDRA_PASS="cassandra"

CASSANDRA_ARGS="cassandra-cql \
${COMMON_ARGS} \
-p hosts=${CASSANDRA_HOSTS} -p cassandra.keyspace=${CASSANDRA_KEYSPACE} \
-p cassandra.username=${CASSANDRA_USER} -p cassandra.password=${CASSANDRA_PASS}"

# Cockroach input args
COCKROACH_DRIVER="org.postgresql.Driver"
COCKROACH_USER="root"
COCKROACH_PASS="root"
COCKROACH_DB="master"
COCKROACH_PORT="26257"
COCKROACH_HOST="localhost"
COCKROACH_URL="jdbc:postgresql://${COCKROACH_HOST}:${COCKROACH_PORT}/${COCKROACH_DB}"

COCKROACH_ARGS="jdbc
${COMMON_ARGS} \
-p db.driver=${COCKROACH_DRIVER} -p db.url=${COCKROACH_URL} -p db.user=${COCKROACH_USER}"