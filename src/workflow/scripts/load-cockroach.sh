#!/usr/bin/env bash

source ./read-cockroach-table-count.sh

source ./read-input-args.sh

eval ycsb load ${COCKROACH_ARGS} "$@"