# master-utcn
Codul sursa si documentatia pentru proiectul de master.

# Build workflow

Instalati maven si rulati `mvn clean install` din ./src/workflow

# Rulare workflow

Asigurati-va ca ati instalat YCSB local si ca puteti executa comanda `ycsb`

`cd <PROJECT>/src/workflow`

`mvn clean install`

`ycsb load basic -P  workloads/workload1 -cp ./target/core-0.16.0-SNAPSHOT.jar`