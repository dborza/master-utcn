#!/usr/bin/env bash

source ./read-cassandra-table-count.sh

source ./read-input-args.sh

ycsb run ${CASSANDRA_ARGS}