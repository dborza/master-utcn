#!/usr/bin/env bash

# Maven copies over all needed jars to this folder.
LIBS="./../target"

CLASSPATH="$LIBS/core-0.16.0-SNAPSHOT.jar:$LIBS/dependency/*"

LOG_DIR='./logs'
mkdir ${LOG_DIR}

# Where all the logs are being stored.
LOG_NAME="${LOG_DIR}/`date +\"%Y-%m-%d-%T\"`.log"

echo "Storing output in log file "${LOG_NAME}

# Common args to both the Cassandra and CockroachDB runs.
COMMON_ARGS="-P ../workloads/workload1 \
-cp ${CLASSPATH} \
-jvm-args \"-Dlog4j.configurationFile=log4j.xml -DlogName=${LOG_NAME} \" \
-p exportfile=${LOG_NAME}.report \
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