#!/usr/bin/env bash

sh ./create-cassandra-schema.sh

source ./read-cassandra-table-count.sh

source ./read-input-args.sh

eval ycsb load ${CASSANDRA_ARGS} "$@"