#!/usr/bin/env bash
echo "Creating Cassandra schema."
cqlsh -f cassandra-schema-cloud.cql "$@"
echo "Schema creation end."
