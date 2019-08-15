These will be generic instructions on how to create an AWS cluster that can be used to install both cockroach and CockroachDB databases.

The cluster will consist in creating a VPC with 3 subnets in different Availability Zones.

# Setup EC2

## Create a new AWS account.

In EC2 create a new KeyPair that you will use to log into EC2 hosts (Ops hosts, cockroach & CockroachDB nodes)

## Create the Cockroach cluster

### Demo Cluster with CloudFormation

Make sure you use the 'Oregon' region in AWS from now on.

Run the './scripts/cloudformation/cockroachdb-kubernetes-cluster-with-new-vpc.template' template in CloudFormation.

### Final Test cluster (manually)

Create VPC, 3 Subnets, Add an Internet Gateway, Configure Security Groups, etc. Launch an EC2 ops host and 6 EC2 instance, 2 for each AZ.
Make sure the security groups for all instances have outgoing Internet access and that they allow access to ports 8080 and 

https://www.cockroachlabs.com/docs/stable/deploy-cockroachdb-on-premises-insecure.html

Below steps are for the insecure cluster

1. Synchronize clocks on each Cockroach host

NTP is configured on Amazon Linux 2 AMIs (https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/set-time.html). 

* Install Cockroach Db on each EC2 host

ssh into the host

wget -qO- https://binaries.cockroachdb.com/cockroach-v19.1.4.linux-amd64.tgz | tar  xvz
sudo cp -i cockroach-v19.1.4.linux-amd64/cockroach /usr/local/bin

cockroach start \
--insecure \
--advertise-addr=10.0.228.161 \
--join=10.0.118.87,10.0.11.14,10.0.228.161 \
--cache=.25 \
--max-sql-memory=.25 \
--background

Once you're done, check the cluster status with

cockroach node status --insecure

If everything looks alright, go ahead and create Load Balancer: Network LB, Internal and add all 3 AZs to it. The input port of your LB can be 80 and it can forward requests to port 26257 (the default CockroachDB port)

You should add a health check to port 8080 with url '/health?ready=1'

Make sure you can connect to the Load Balancer

`cockroach sql --insecure --host=<LOAD_BALANCER_DNS_NAME> --port=80`

## Log into the Ops host and install YCSB

Log into the ssh host using the key pair and the ops public DNS name (which you can find in the EC2 -> Instances tab)

`ssh -i "<PATH_TO_KEY_PAIR>" ubuntu@<PUBLIC_OPS_INSTANCE_DNS_NAME>`

Original instructions https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

Run the script `./scripts/install-ycsb-ubuntu.sh`

Now make sure you can run YCSB on the machine by typing `ycsb`. The output should be an error message of the form `ycsb: error: too few arguments`
 
# In the Ops host install OpenJDK 8 if not already installed

Type `java -version` and if you see an output like `openjdk version "1.8.0_212"` the you can skip this step.

Original instructions https://www.geofis.org/en/install/install-on-linux/install-openjdk-8-on-ubuntu-trusty/

# In the Ops host clone the git repo


Make sure you have git installed, if not execute `sudo apt install git` (Ubuntu) or `sudo yum install git` on CentOS (EC2 Linux 2 AMI)

Next execute the following command line:

Ubuntu:
```
sudo apt install maven && \
git clone https://github.com/dborza/master-utcn.git && \
cd master-utcn/src/workflow && \
mvn clean install dependency:copy-dependencies
```

CentOS (EC2 Linux 2 AMI)
```
sudo yum install maven && \
git clone https://github.com/dborza/master-utcn.git && \
cd master-utcn/src/workflow && \
mvn clean install dependency:copy-dependencies
```

# In the ops host install Cockroach in order to use the command line tool

https://www.cockroachlabs.com/docs/stable/install-cockroachdb-linux.html

wget -qO- https://binaries.cockroachdb.com/cockroach-v19.1.3.linux-amd64.tgz | tar  xvz
sudo cp -i cockroach-v19.1.3.linux-amd64/cockroach /usr/local/bin


# In the Ops host make sure you can connect to the LB

cockroach sql --insecure --url="postgresql://root@Cockroach-ApiLoadB-1R16VR9U78Y7X-1015037803.us-west-2.elb.amazonaws.com"


# Create the cockroach database schema 

 
export COCKROACH_PORT=<ELB_PORT>
export COCKROACH_HOST=<ELB_DNS_NAME>
export COCKROACH_USER=root
export COCKROACH_DB=master

./create-cockroach-schema.sh

# Finally execute the scripts

Run a test to make sure the load script works

`./load-cockroach.sh`

Then you can execute the actual load test by overriding the default values found in the work load `./workloads/workflow1`
`./load-cockroach.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

`/run-cockroach.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100`

The predefined suggested set of test suites is

Sample run

10 threads, with data integrity check

```
./create-cockroach-schema.sh
./load-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

10 threads, without data integrity check


```
./create-cockroach-schema.sh
./load-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```

100 threads, with data integrity check

```
./create-cockroach-schema.sh
./load-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

100 threads, without data integrity check

```
./create-cockroach-schema.sh
./load-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```
