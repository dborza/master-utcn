#!/usr/bin/env bash

source ./read-cassandra-table-count.sh

source ./read-input-args.sh

eval ycsb load ${CASSANDRA_ARGS} "$@"