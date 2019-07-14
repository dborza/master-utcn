These will be generic instructions on how to create an AWS cluster that can be used to install both Cassandra and CockroachDB databases.

The cluster will consist in creating a VPC with 3 subnets in different Availability Zones.

# Setup EC2

## Create a new AWS account.

In EC2 create a new KeyPair that you will use to log into EC2 hosts (Ops hosts, Cassandra & CockroachDB nodes)

## Create the Cockroach cluster

Make sure you use the 'Oregon' region in AWS from now on.

Run the './scripts/cloudformation/cockroachdb-kubernetes-cluster-with-new-vpc.template' template in CloudFormation.

## Log into the Ops host and install YCSB

Log into the ssh host using the key pair and the ops public DNS name (which you can find in the EC2 -> Instances tab)

`ssh -i "<PATH_TO_KEY_PAIR>" ubuntu@<PUBLIC_OPS_INSTANCE_DNS_NAME>

Original instructions https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

Run the script `./scripts/install-ycsb-ubuntu.sh'

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

# In the ops host install Cockroach in order to use the command line tool

TODO: add instructions.

# Create the Cassandra database schema 

 
TODO: add instructions

# Finally execute the scripts

Run a test to make sure the load script works

`./load-cockroach.sh`

Then you can execute the actual load test by overriding the default values found in the work load `./workloads/workflow1`
`./load-cockroacj.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

`/run-cockroach.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

The predefined suggested set of test suites is

```
./create-cockroach-schema.sh && ./load-cockroach.sh && ./run-cockroach.sh -P ../workloads/workload1
./run-cockroach.sh -P ../workloads/workload2
./run-cockroach.sh -P ../workloads/workload3
./run-cockroach.sh -P ../workloads/workload4
./run-cockroach.sh -P ../workloads/workload5
```
