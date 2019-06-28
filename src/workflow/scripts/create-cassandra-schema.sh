#!/usr/bin/env bash
echo "Creating Cassandra schema."
cqlsh -f cassandra-schema.cql
echo "Schema creation successful."
