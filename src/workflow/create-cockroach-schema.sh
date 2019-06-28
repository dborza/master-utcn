#!/usr/bin/env bash
cockroach sql --insecure --user=root --host=localhost --port=26257 < cockroach-schema.sql
