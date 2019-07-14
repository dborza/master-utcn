#!/usr/bin/env bash
echo "Creating Cockroach schema."
cockroach sql --insecure --user=${COCKROACH_USER} --host=${COCKROACH_HOST} --port=${COCKROACH_PORT} < cockroach-schema.sql
echo "Schema creation end."
