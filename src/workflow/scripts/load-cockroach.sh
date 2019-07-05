#!/usr/bin/env bash

sh ./create-cockroach-schema.sh

source ./read-cockroach-table-count.sh

source ./read-input-args.sh

eval ycsb load ${COCKROACH_ARGS}