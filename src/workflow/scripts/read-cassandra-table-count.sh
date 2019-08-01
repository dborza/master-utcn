#!/usr/bin/env bash

echo "Connecting to Cassandra DB"
echo "User: "${CASSANDRA_USER}
echo "Pass: "${CASSANDRA_PASS}
echo "Host: "${CQLSH_HOST}
echo "Keyspace: "${CASSANDRA_KEYSPACE}

DEVICE_ROWS=`cqlsh -e "select count(*) from "${CASSANDRA_KEYSPACE}".device;" -u ${CASSANDRA_USER} -p ${CASSANDRA_PASS} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing device rows #"$DEVICE_ROWS

SENSOR_ROWS=`cqlsh -e "select count(*) from "${CASSANDRA_KEYSPACE}".sensor;" -u ${CASSANDRA_USER} -p ${CASSANDRA_PASS} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing sensor rows #"$SENSOR_ROWS

MEASUREMENT_ROWS=`cqlsh -e "select count(*) from "${CASSANDRA_KEYSPACE}".measurement;" -u ${CASSANDRA_USER} -p ${CASSANDRA_PASS} | sed -n 4p | awk '{$1=$1};1'`
echo "Found existing measurement rows #"$MEASUREMENT_ROWS
