#!/usr/bin/env bash
echo "Creating Cockroach schema."
cockroach sql --insecure --user=root --host=localhost --port=26257 < cockroach-schema.sql
echo "Schema creation end."
