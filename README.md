# master-utcn
Codul sursa si documentatia pentru proiectul de master.

# Build workflow

`cd ./src/workflow`
`mvn clean install`

# CockroachDB

## Schema

### Create DB Schema

```
CREATE DATABASE master;
USE master;
```
### Create Device table

```
CREATE TABLE IF NOT EXISTS device (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255)
);

```

### Create Sensor table
```
CREATE TABLE IF NOT EXISTS sensor (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255),
  device_id VARCHAR(255) NOT NULL REFERENCES device(ycsb_key)
);
```

### Create Measurement table

```
CREATE TABLE IF NOT EXISTS measurement (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  type VARCHAR(255),
  sensor_id VARCHAR(255) NOT NULL REFERENCES sensor(ycsb_key) NOT NULL,
  values VARCHAR(255),
  create_time VARCHAR(255)
);
```
### Delete all tables
```
DELETE FROM master.measurement WHERE 1=1;
DELETE FROM master.sensor WHERE 1=1;
DELETE FROM master.device WHERE 1=1;

```
## Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

Use basic db - 
`mvn clean install && ycsb load basic -P  workloads/workload1 -cp ./target/core-0.16.0-SNAPSHOT.jar`

Use cockroach db for load - `mvn clean install && ycsb load jdbc -P workloads/workload1 -p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root -cp ./target/core-0.16.0-SNAPSHOT.jar`

Use cockroach db for run - `mvn clean install && ycsb run jdbc -P workloads/workload1 -p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root -cp ./target/core-0.16.0-SNAPSHOT.jar`

# Cassandra DB

## Schema

### Create Keyspace

#### Desktop
```
CREATE KEYSPACE IF NOT EXISTS master WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
USE master;
```

#### Cluster - TODO (needs different REPLICATION strategy).

### Create Device table

```
CREATE TABLE master.device (
  y_id text PRIMARY KEY,
  name text
);

```

### Create Sensor table
```
CREATE TABLE master.sensor (
  y_id text PRIMARY KEY,
  name text,
  device_id text
);
```

### Create Measurement table

```
CREATE TABLE IF NOT EXISTS measurement (
  y_id text PRIMARY KEY,
  type text,
  sensor_id text,
  values text,
  create_time text
);
```
### Delete all tables
```
TRUNCATE master.measurement;
TRUNCATE master.sensor;
TRUNCATE master.device;
```

## Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

Use cassandra db for load - `mvn clean install && ycsb load cassandra-cql -P workloads/workload1 -p hosts="localhost" -p cassandra.keyspace="master" -cp ./target/core-0.16.0-SNAPSHOT.jar:./lib/slf4j-simple-1.7.26.jar`

Use cassandra db for run - `mvn clean install && ycsb run cassandra-cql -P workloads/workload1 -p hosts="localhost" -p cassandra.keyspace="master" -cp ./target/core-0.16.0-SNAPSHOT.jar:./lib/slf4j-simple-1.7.26.jar`

Mai multe info despre rulare - https://github.com/brianfrankcooper/YCSB/tree/master/cassandra