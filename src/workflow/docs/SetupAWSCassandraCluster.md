These will be generic instructions on how to create an AWS cluster that can be used to install both Cassandra and CockroachDB databases.

The cluster will consist in creating a VPC with 3 subnets in different Availability Zones.

# Setup EC2

## Create a new AWS account.

In EC2 create a new KeyPair that you will use to log into EC2 hosts (Ops hosts, Cassandra & CockroachDB nodes)

## Create the Cassandra cluster

Make sure you use the 'Oregon' region in AWS from now on, Cassandra schema will use that for the replication factor 
(check out `./scripts/cassandra-schema-cloud.cql`)

Run the './scripts/cloudformation/quickstart-datastax-master.template' template in CloudFormation.

## Log into the Ops host and install YCSB

Log into the ssh host using the key pair and the ops public DNS name (which you can find in the EC2 -> Instances tab)

`ssh -i "<PATH_TO_KEY_PAIR>" ubuntu@<PUBLIC_OPS_INSTANCE_DNS_NAME>

Original instructions https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

Or run the script `./scripts/install-ycsb-ubuntu.sh'

Now make sure you can run YCSB on the machine by typing `ycsb`. The output should be an error message of the form `ycsb: error: too few arguments`
 
# In the Ops host install OpenJDK 8 if not already installed

Type `java -version` and if you see an output like `openjdk version "1.8.0_212"` the you can skip this step.

Original instructions https://www.geofis.org/en/install/install-on-linux/install-openjdk-8-on-ubuntu-trusty/

# In the Ops host clone the git repo

Make sure you have git installed, if not execute `sudo apt install git`.

Next execute the following command line:

```
sudo apt install maven && \
git clone https://github.com/dborza/master-utcn.git && \
cd master-utcn/src/workflow && \
mvn clean install dependency:copy-dependencies
```

# In the ops host install Cassandra in order to use the command line tool

You won't actually need to start the cassandra server. Just follow the instructions up until that point at https://www.vultr.com/docs/how-to-install-apache-cassandra-3-11-x-on-ubuntu-16-04-lts

To make sure installation was successful type `cqlsh`.

Set the host `cqlsh` will connect to `export CQLSH_HOST=<CASSANDRA_EC2_INSTANCE_IP>`.

In order to connect to the Cassandra cluster first connect to one of the hosts `cqlsh -u cassandra -p cassandra` 

# Create the Cassandra database schema 

Edit the `./scripts/cassandra-schema-cloud.cql` file and replace `DC0` in the replication strategy with the name you've passed 
into CloudFormation for the parameter `DC0Name`
 
```
cd ./scripts
export CASSANDRA_USER=cassandra
export CASSANDRA_PASS=cassandra
export CQLSH_HOST=10.0.27.247
export CASSANDRA_KEYSPACE=master
./create-cassandra-schema-cloud.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS $CQLSH_HOST
```

# Finally execute the scripts

Run a test to make sure the load script works

`./load-cassandra.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS`

Then you can execute the actual load test by overriding the default values found in the work load `./workloads/workflow1`
`./load-cassandra.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

`/run-cassandra.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

Sample run

10 threads, with data integrity check

```
./create-cassandra-schema-cloud.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS $CQLSH_HOST
./load-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

10 threads, without data integrity check


```
./create-cassandra-schema-cloud.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS $CQLSH_HOST
./load-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```

100 threads, with data integrity check

```
./create-cassandra-schema-cloud.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS $CQLSH_HOST
./load-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

100 threads, without data integrity check

```
./create-cassandra-schema-cloud.sh -u $CASSANDRA_USER -p $CASSANDRA_PASS $CQLSH_HOST
./load-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cassandra.sh -p hosts=10.0.27.247,10.0.75.196,10.0.4.215,10.0.47.63,10.0.37.232,10.0.73.10 -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```

-p dataintegrity=false
