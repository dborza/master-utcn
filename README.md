# master-utcn
Source code and documentation for master's degree project.

These instructions are valid for local / desktop development.

# Install YCSB

https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

Add ycsb to your $PATH variable 

Ubuntu - 

`vi ~./profile` 

and add a line like 

`PATH="$HOME/ycsb/bin:$PATH"`

Make sure ycsb is installed and that you can execute the `ycsb` command.

# Install Maven

Ubuntu - `sudo apt install maven`

# Build workflow

```
cd ./src/workflow
mvn clean install
```

# Install Cassandra

https://www.liquidweb.com/kb/install-cassandra-ubuntu-16-04-lts/

# CockroachDB

## Schema

### Create DB Schema

```
cd <PROJECT>/src/workflow/scripts
./create-cockroach-schema.sh
```

## Run workload

`cd <PROJECT>/src/workflow/scripts`

Use cockroach db for load - `./load-cockroach.sh`

Use cockroach db for run - `./run-cockroach.sh`

# Cassandra DB

## Schema

#### Desktop

run `./create-cassandra-schema.sh`

#### Cluster - TODO (needs different REPLICATION strategy).

## Run workload

`cd <PROJECT>/src/workflow`

Use cassandra db for load - `./load-cassandra.sh`

Use cassandra db for run - `./run-cassandra.sh`

Mai multe info despre rulare - https://github.com/brianfrankcooper/YCSB/tree/master/cassandra