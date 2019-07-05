These will be generic instructions on how to create an AWS cluster that can be used to install both Cassandra and CockroachDB databases.

The cluster will consist in creating a VPC with 3 subnets in different Availability Zones.

# Setup EC2

## Create a new AWS account.

## In EC2 Create a new VPC with the default CIDR (10.0.0.0/24) and create a new Internet Gateway and attach it to that VPC.

## In EC2 create a new KeyPair that you will use to log into EC2 hosts (Ops hosts, Cassandra & CockroachDB nodes)

## Install Cassandra

# Run the 'Cassandra_EBS_NoVPC.json' template in CloudFormation.

# Log into the ops host and install ycsb

https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

# Install OpenJDK 8

https://www.geofis.org/en/install/install-on-linux/install-openjdk-8-on-ubuntu-trusty/

# Clone the git repo

```sudo apt install maven
git clone https://github.com/dborza/master-utcn.git
cd master-utcn/src/workflow
mvn clean install dependency:copy-dependencies
```

## Install Cockroach DB

# Run the cockroach tempate into your cluster.

https://www.cockroachlabs.com/docs/stable/deploy-a-test-cluster.html





