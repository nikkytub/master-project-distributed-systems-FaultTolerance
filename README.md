# Fault tolerance in distributed heterogeneous environment

This script contains Java package to trigger Forest temperature alerts and 
python file to create heterogeneous env using docker containers.

### Installing

Clone this repository in your local machine via
```
$ git clone git@gitlab.tubit.tu-berlin.de:nikhil.singh/master-project-distributed-systems-FaultTolerance.git
```

## Deployment

To create Jobmanager
```
$ python3 create-flink-cluster.py --jobmanager
```

To create Taskmanager
```
$ python3 create-flink-cluster.py --taskmanager
```

To create taskmanager with different cpu and memory limitations. For example,
```
$ python3 create-flink-cluster.py --taskmanager --cpu=0.70 memory=600m
```

To scale the cluster. For example,
```
$ python3 create-flink-cluster.py --taskmanager --cpus=0.75 --memory=700m --scale=5
```

To remove the cluster
```
$ python3 create-flink-cluster.py --remove
```

For Help
```
$ python3 create-flink-cluster.py --help
```

To check cpu and memory utilization and limitation
```
$ docker stats
```

From Java package please build the jar which will be stored in 
'wiki-edits/classes/artifacts/wiki_edits_jar/'. Copy it to Jobmanager's 
running container using
```
$ docker cp wiki-edits.jar jobmanager:/opt/flink
```

Then use
```
$ docker exec -it jobmanager sh
```

Open a new terminal window and run netcat server
```
$ nc -l 9000
```

Check your IP address to pass into host. To run the job with checkpoints 
stored in local file system. Please run for example,
```
$ bin/flink run -c wikiedits.ForestTemperatureWithCheckpoints wiki-edits.jar --host 192.168.178.27 --port 9000
```

To run the job with checkpoints stored in Jobmanager's memory. Please run for 
example,
```
$ bin/flink run -c wikiedits.ForestTempWithCheckpointsMem wiki-edits.jar --host 192.168.178.27 --port 9000
```

##HDFS Deployment
#For more detailed commands look in listings.txt

For Docker set up
```
$ mvn clean package -DskipTests
$ ./build-sh --from-local-dist
```

create network
```
$ docker network create flink_net --opt com.docker.network.bridge.name=flink_net --opt com.docker.network.bridge.host_binding_ipv4=0.0.0.0 --opt com.docker.network.bridge.enable_icc=true --opt com.docker.network.bridge.enable_ip_masquerade=true
$ iptables -A INPUT -i flink_net -j ACCEPT
```

start hadoop 
```
$ docker run -d --net=flink_net --name hadoop --expose 9000 --expose 8020 -p 50070:50070 -p 50010:50010 -p 8088:8088 -p 9000:9000 -p 8020:8020 sequenceiq/hadoop-docker:2.7.1
```

start external scheduler
```
Did this in intellij on localhost
Make sure to change jobmanager.scheduler.external.rpc.address: in flink.conf from <localhost> to <Docker Host IP> (where the scheduler resides)
```

start zookeeper quorum
```
$ docker run -d --net=flink_net --name=zookeeper -p 2181:2181 -e ZOOKEEPER_CLIENT_PORT=2181 -e ZOOKEEPER_TICK_TIME=2000 -e ZOOKEEPER_SYNC_LIMIT=2 -e ZOO_SERVER_ID=1 zookeeper:latest
```

start docker containers
```
$ docker-compose up
```

Install Pumba
```
$ sudo curl -L https://github.com/alexei-led/pumba/releases/download/0.5.2/pumba_linux_amd64 -o /usr/bin/pumba
$ sudo chmod +x /usr/bin/pumba
```

Install weavescope for monitoring
```
$ wget -O scope git.io/scope
$ chmod a+x ./scope
$ ./scope launch
```

Run program
```
$ sudo docker exec -it $(sudo docker ps --filter name=flink_jobmanager --format={{.ID}}) /bin/sh
$ cd opt/flink
$ ./bin/flink run examples/edge/ForestTemperatureWithCheckpoints.jar --checkpoint true --external true --operatorChain false --host <host_ip> --port 9999
OR
$ ./bin/flink run examples/edge/AsyncIOExample.jar --failRatio 0 --external true --operatorChain false --checkpointMode exactly_once
```

