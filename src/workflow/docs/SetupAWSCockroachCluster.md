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


wget -qO- https://binaries.cockroachdb.com/cockroach-v19.1.4.linux-amd64.tgz | tar  xvz
sudo cp -i cockroach-v19.1.4.linux-amd64/cockroach /usr/local/bin

ssh into the host

```sh
export HOST1_PRIVATE=10.0.11.52
export HOST2_PRIVATE=10.0.37.29
export HOST3_PRIVATE=10.0.64.35
export HOST4_PRIVATE=10.0.75.76
export HOST5_PRIVATE=10.0.201.247
export HOST6_PRIVATE=10.0.199.233

cockroach start \
--insecure \
--advertise-addr=`hostname -I` \
--join=$HOST1_PRIVATE,$HOST3_PRIVATE,$HOST5_PRIVATE \
--cache=.25 \
--max-sql-memory=.25 \
--background

ps ax | grep cockroach


```

Once you're done, in any host init the cluster and check its status:

```sh
cockroach init --insecure
cockroach node status --insecure
```

The output should be something like

```
[ec2-user@ip-10-0-11-52 ~]$ cockroach node status --insecure
  id |      address       |  build  |            started_at            |            updated_at            | is_available | is_live
+----+--------------------+---------+----------------------------------+----------------------------------+--------------+---------+
   1 | 10.0.11.52:26257   | v19.1.3 | 2019-08-16 03:49:41.440779+00:00 | 2019-08-16 03:49:45.993379+00:00 | true         | true
   2 | 10.0.201.247:26257 | v19.1.3 | 2019-08-16 03:49:41.898822+00:00 | 2019-08-16 03:49:46.452402+00:00 | true         | true
   3 | 10.0.64.35:26257   | v19.1.3 | 2019-08-16 03:49:42.307713+00:00 | 2019-08-16 03:49:42.364779+00:00 | true         | true
   4 | 10.0.37.29:26257   | v19.1.3 | 2019-08-16 03:49:42.531168+00:00 | 2019-08-16 03:49:42.570485+00:00 | true         | true
   5 | 10.0.75.76:26257   | v19.1.3 | 2019-08-16 03:49:42.851063+00:00 | 2019-08-16 03:49:42.907291+00:00 | true         | true
   6 | 10.0.199.233:26257 | v19.1.3 | 2019-08-16 03:49:43.182342+00:00 | 2019-08-16 03:49:43.237501+00:00 | true         | true
```

If everything looks alright, go ahead and create Load Balancer: Network LB, Internal and add all 3 AZs to it. The input port of your LB can be 80 and it can forward requests to port 26257 (the default CockroachDB port)

You should add a health check to port 8080 with url '/health?ready=1'

Make sure you can connect to the Load Balancer

`cockroach sql --insecure --host=<LOAD_BALANCER_DNS_NAME> --port=80`

## Log into the Ops host and install YCSB

Log into the ssh host using the key pair and the ops public DNS name (which you can find in the EC2 -> Instances tab)

`ssh -i "<PATH_TO_KEY_PAIR>" ubuntu@<PUBLIC_OPS_INSTANCE_DNS_NAME>`

Original instructions https://github.com/brianfrankcooper/YCSB/wiki/Getting-Started

Run the script `./scripts/install-ycsb-ubuntu.sh`

```sh
#!/usr/bin/env bash
curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.14.0/ycsb-0.14.0.tar.gz
tar xfvz ycsb-0.14.0.tar.gz
cd ycsb-0.14.0
echo "PATH=\"\$PATH:`pwd`/bin\"" >> ~/.profile
source ~/.profile
cd ~

```
Now make sure you can run YCSB on the machine by typing `ycsb`. The output should be an error message of the form `ycsb: error: too few arguments`
 
# In the Ops host install OpenJDK 8 if not already installed

Type `java -version` and if you see an output like `openjdk version "1.8.0_212"` the you can skip this step.

Original instructions https://www.geofis.org/en/install/install-on-linux/install-openjdk-8-on-ubuntu-trusty/

# In the Ops host clone the git repo


Make sure you have git installed, if not execute `sudo apt install git` (Ubuntu) or `sudo yum install git` on CentOS (EC2 Linux 2 AMI)

Next install git, maven, pull and build the repo by executing the following command line:

Ubuntu:

```sh
sudo apt install maven && \
sudo apt install git && \
git clone https://github.com/dborza/master-utcn.git && \
cd master-utcn/src/workflow && \
mvn clean install dependency:copy-dependencies
```

CentOS (EC2 Linux 2 AMI)

```sh
sudo yum install git && \
sudo yum install maven && \
git clone https://github.com/dborza/master-utcn.git && \
cd master-utcn/src/workflow && \
mvn clean install dependency:copy-dependencies
```

# In the ops host install Cockroach in order to use the command line tool

https://www.cockroachlabs.com/docs/stable/install-cockroachdb-linux.html

```sh
wget -qO- https://binaries.cockroachdb.com/cockroach-v19.1.3.linux-amd64.tgz | tar  xvz
sudo cp -i cockroach-v19.1.3.linux-amd64/cockroach /usr/local/bin

```


# In the Ops host make sure you can connect to the LB

cockroach sql --insecure --url="postgresql://root@Cockroach-ApiLoadB-1R16VR9U78Y7X-1015037803.us-west-2.elb.amazonaws.com"


# Create the cockroach database schema 

 
```sh
export COCKROACH_PORT=<ELB_PORT>
export COCKROACH_HOST=<ELB_DNS_NAME>
export COCKROACH_USER=root
export COCKROACH_DB=master
```

For localhost

```sh
export COCKROACH_PORT=26257
export COCKROACH_HOST=localhost
export COCKROACH_USER=root
export COCKROACH_DB=master
```

For load balancer

```sh
export COCKROACH_PORT=80
export COCKROACH_HOST=cockroach-network-lb-c26bf4c5ca9b2f07.elb.us-west-2.amazonaws.com 
export COCKROACH_USER=root
export COCKROACH_DB=master
```

./create-cockroach-schema.sh

# Finally execute the scripts

Run a test to make sure the load script works

```sh
./load-cockroach.sh
```

Then you can execute the actual load test by overriding the default values found in the work load `./workloads/workflow1` like

```sh
./load-cockroach.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100
```

```sh
/run-cockroach.sh -p recordcount=100000 -p operationcount=1000000 -p threadcount=100
```

The predefined suggested set of test suites is

Sample run

10 threads, with data integrity check

```sh
./create-cockroach-schema.sh
# Cockroach is slow therefore 1000 threads for INSERT statements.
./load-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cockroach.sh -p threadcount=10 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

100 threads, with data integrity check

```sh
./create-cockroach-schema.sh
# Cockroach is slow therefore 1000 threads for INSERT statements.
./load-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload1
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload2
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload3
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload4
./run-cockroach.sh -p threadcount=100 -p recordcount=100000 -p operationcount=100000 -P ../workloads/workload5
```

1000 threads, without data integrity check

```sh
./create-cockroach-schema.sh
# Cockroach is slow therefore 1000 threads for INSERT statements.
./load-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cockroach.sh -p threadcount=1000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```

5000 threads, without data integrity check

```sh
./create-cockroach-schema.sh
# Cockroach is slow therefore 1000 threads for INSERT statements.
./load-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000
./run-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload1
./run-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload2
./run-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload3
./run-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload4
./run-cockroach.sh -p threadcount=3000 -p recordcount=100000 -p operationcount=100000 -p dataintegrity=false -P ../workloads/workload5
```
