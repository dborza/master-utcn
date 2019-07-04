#!/usr/bin/env bash

source ./read-cockroach-table-count.sh

source ./read-input-args.sh

ycsb run ${COCKROACH_ARGS}