CREATE DATABASE IF NOT EXISTS master;

DROP TABLE IF EXISTS master.measurement;
DROP TABLE IF EXISTS master.sensor;
DROP TABLE IF EXISTS master.device;

USE master;

CREATE TABLE IF NOT EXISTS device (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255)
);


CREATE TABLE IF NOT EXISTS sensor (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255),
  device_id VARCHAR(255) NOT NULL REFERENCES device(ycsb_key)
);

CREATE TABLE IF NOT EXISTS measurement (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  type VARCHAR(255),
  sensor_id VARCHAR(255) NOT NULL REFERENCES sensor(ycsb_key) NOT NULL,
  values VARCHAR(255),
  create_time VARCHAR(255)
);
