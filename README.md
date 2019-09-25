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