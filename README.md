# master-utcn
Codul sursa si documentatia pentru proiectul de master.

# Build workflow

`cd ./src/workflow`
`mvn clean install`

# CockroachDB

## Schema

### Create DB Schema

run `./create-cockroach-schema.sh`

## Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

Use basic db - 
`mvn clean install && ycsb load basic -P  workloads/workload1 -cp ./target/core-0.16.0-SNAPSHOT.jar`

Use cockroach db for load - `mvn clean install && ycsb load jdbc -P workloads/workload1 -p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root -cp ./target/core-0.16.0-SNAPSHOT.jar`

Use cockroach db for run - `mvn clean install && ycsb run jdbc -P workloads/workload1 -p db.driver=org.postgresql.Driver -p db.url=jdbc:postgresql://localhost:26257/master -p db.user=root -cp ./target/core-0.16.0-SNAPSHOT.jar`

# Cassandra DB

## Schema

#### Desktop

run `./create-cassandra-schema.sh`

#### Cluster - TODO (needs different REPLICATION strategy).

## Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

Use cassandra db for load - `mvn clean install && ycsb load cassandra-cql -P workloads/workload1 -p hosts="localhost" -p cassandra.keyspace="master" -cp ./target/core-0.16.0-SNAPSHOT.jar:./lib/slf4j-simple-1.7.26.jar`

Use cassandra db for run - `mvn clean install && ycsb run cassandra-cql -P workloads/workload1 -p hosts="localhost" -p cassandra.keyspace="master" -cp ./target/core-0.16.0-SNAPSHOT.jar:./lib/slf4j-simple-1.7.26.jar`

Mai multe info despre rulare - https://github.com/brianfrankcooper/YCSB/tree/master/cassandra