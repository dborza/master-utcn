CREATE KEYSPACE IF NOT EXISTS master WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'DC0' : '3' };

DROP TABLE IF EXISTS master.device;
DROP TABLE IF EXISTS master.sensor;
DROP TABLE IF EXISTS master.measurement;

USE master;

CREATE TABLE IF NOT EXISTS master.device (
  y_id text PRIMARY KEY,
  name text
);

CREATE TABLE IF NOT EXISTS master.sensor (
  y_id text PRIMARY KEY,
  name text,
  device_id text
);

CREATE TABLE IF NOT EXISTS master.measurement (
  y_id text PRIMARY KEY,
  type text,
  sensor_id text,
  values text,
  create_time text
);


