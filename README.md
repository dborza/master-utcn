# master-utcn
Codul sursa si documentatia pentru proiectul de master.

# Schema DB

# Create DB

```
CREATE DATABASE master;
USE master;
```
## Device

```
CREATE TABLE IF NOT EXISTS device (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255)
);

```

## Sensor
```
CREATE TABLE IF NOT EXISTS sensor (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255),
  deviceId VARCHAR(255) NOT NULL REFERENCES device(ycsb_key)
);
```

### Measurement

```
CREATE TABLE IF NOT EXISTS measurement (
  ycsb_key VARCHAR(255) PRIMARY KEY,
  type VARCHAR(255),
  sensorId VARCHAR(255) NOT NULL REFERENCES sensor(ycsb_key) NOT NULL,
  values VARCHAR(255),
  create_time VARCHAR(255)
);
```
### Stergere tabele
```
DELETE FROM measurement WHERE 1=1;
DELETE FROM sensor WHERE 1=1;
DELETE FROM device WHERE 1=1;

```
# Build workflow

Instalati maven si rulati `mvn clean install` din ./src/workflow

# Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

Use basic db - 
`mvn clean install && ycsb load basic -P  workloads/workload1 -cp ./target/core-0.16.0-SNAPSHOT.jar`

Use cockroach db - `mvn clean install && ycsb load jdbc -P workloads/workload1 -p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root -cp ./target/core-0.16.0-SNAPSHOT.jar`
