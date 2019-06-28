#!/usr/bin/env bash
cqlsh -f cassandra-schema.cql | sed -n 4p | awk '{$1=$1};1'
