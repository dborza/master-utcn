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

# Cassandra default input args
CASSANDRA_KEYSPACE="master"

if [ -z ${CQLSH_HOST+x} ]; then CQLSH_HOST="localhost"; fi
if [ -z ${CASSANDRA_USER+x} ]; then CASSANDRA_USER="cassandra"; fi
if [ -z ${CASSANDRA_PASS+x} ]; then CASSANDRA_PASS="cassandra"; fi

CASSANDRA_ARGS="cassandra-cql \
${COMMON_ARGS} \
-p hosts=${CQLSH_HOST} -p cassandra.keyspace=${CASSANDRA_KEYSPACE} \
-p cassandra.username=${CASSANDRA_USER} -p cassandra.password=${CASSANDRA_PASS}"

# Cockroach default input args
COCKROACH_DRIVER="org.postgresql.Driver"
if [ -z ${COCKROACH_USER+x} ]; then COCKROACH_USER="root"; fi
if [ -z ${COCKROACH_PASS+x} ]; then COCKROACH_PASS="root"; fi
if [ -z ${COCKROACH_DB+x} ]; then COCKROACH_DB="master"; fi
if [ -z ${COCKROACH_PORT+x} ]; then COCKROACH_PORT="26257"; fi
if [ -z ${COCKROACH_HOST+x} ]; then COCKROACH_HOST="localhost"; fi
if [ -z ${COCKROACH_URL+x} ]; then COCKROACH_URL="jdbc:postgresql://${COCKROACH_HOST}:${COCKROACH_PORT}/${COCKROACH_DB}"; fi

COCKROACH_ARGS="jdbc
${COMMON_ARGS} \
-p db.driver=${COCKROACH_DRIVER} -p db.url=${COCKROACH_URL} -p db.user=${COCKROACH_USER}"