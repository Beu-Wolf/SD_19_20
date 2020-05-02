# Sauron demonstration guide

## Part 1

With the project already installed,
make sure ZooKeeper is running and,

Run replica 1
```
cd silo-server
mvn exec:java -Dinstance=1
```

### Case 1: Load test data using Spotter

```
./spotter/target/appassembler/bin/spotter localhost 2181 1 < demo/initSilo.txt
```

### Case 2: Register observations using the Eye client

Verify success in reporting:
```
./eye/target/appassembler/bin/eye localhost 2181 testCam2 12.456789 -8.987654 1
person,89427
person,89399
person,89496
\n
```
Verify invalid person ID response:

```
person,R4a_
\n
```
Verify success in reporting:

```
car,20SD20
car,AA00AA
\n
```
Verify invalid car ID response:

```
car,124_87
\n
```

Press `^C` to exit client Eye

### Case 3: Verify Eye's sleep operations

Verify 10 second pause until observation reported:

```
./eye/target/appassembler/bin/eye localhost 2181 testCam3 12.987654 -8.123456 1
zzz,10000
person,7777
\n
```

Press `^C` to exit client Eye

### Case 4: Usage of Spotter to execute some queries

Verify that the help screen is displayed:

```
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
```

Verify that:

* Person 1234 was observed
* Car 20SD20 was observed
* Person 0101 was not observed
* Spotting car 7T_Ea2 is invalid

```
spot person 1234
spot car 20SD20
spot person 0101
spot car 7T_Ea2
```

Verify that

* all people shown are ordered by their id
* all people whose id starts with 89 are shown, ordered by their id
* all people whose id ends in 7 are shown, ordered by their id
* all cars whose license plate starts with 20 are shown, ordered by their id
* There are no observations of cars with license plate starting with NE

```
spot person *
spot person 89*
spot person *7
spot car 20*
spot car NE*
```

Verify that:

* person 89427 was spotted by cameras testCam2, camera2, camera1
* car 20SD20 was spotted by cameras testCam2, camera4, camera3
* person with id 0101 was never spotted
* car to spot as invalid license plate

```
trail person 89427
trail car 20SD20
trail person 0101
trail car 7T_Ea2
```
```
exit 
```

### Case 5: Usage of Spotter for control operations

Execute new Spotter:
```
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
```
Verify that the server answers with "Hello friend!":

```
ping friend
```
Verify there is no longer any car or person in the server:

```
clear
spot person *
spot car *
```

Verify success in registering cameras:

```
init cams
$ mockCamera1,14.645678,8.534568
$ mockCamera2,19.994536,7.789765
$ done
```
Verify success in registering observations:

```
init obs
$ mockCamera1,person,89399
$ mockCamera2,car,20SD21
$ mockCamera1,car,20SD21
$ mockCamera2,person 89399
$ done
```
Verify that

* person 89399 as it's most recent observation at the camera mockCamera2
* car 20SD21 appears in 2 observations at the cameras MockCamera1 and MockCamera2

```
spot person 89399
trail car 20SD21
```
```
exit 
```

Exit server by pressing `Enter`

## Part 2
 
With the project already installed,

Launch 3 replicas
```
cd silo-server
mvn exec:java -Dinstance=1
mvn exec:java -Dinstance=2
mvn exec:java -Dinstance=3
```

Load some data in replica 1

```
./spotter/target/appassembler/bin/spotter localhost 2181 1 < demo/initSilo.txt
```


 ### Case 1: Replicas updated
 Execute two new Spotters connected to replicas 2 and 3 
 ```
 ./spotter/target/appassembler/bin/spotter localhost 2181 2
 help
./spotter/target/appassembler/bin/spotter localhost 2181 3
 help
 ```

Wait until you see that spotter 1 has sent its updates.

```
Sending to #2
Sending 2 updates
Sending to #3
Sending 2 updates
```

```
Got gossip from 1
Received 2 updates
```

Verify that both the Spotters have:

 * person 89427 as it's most recent observation at the camera2
 * car 20SD20 appears in 2 observations at camera4 and camera 3
 
 ```
 spot person 89427
 trail car 20SD20
exit
 ```

### Case 2: Updates in one replica reflected in another
Kill all 3 replicas, by pressing `Enter`, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 5;

Launch the 3 replicas, with the `clean` and `compile` commands

```
cd silo-server
mvn clean compile exec:java -Dinstance=1
mvn clean compile exec:java -Dinstance=2
mvn clean compile exec:java -Dinstance=3
```

Load some data in replica 1 and connect a new Spotter to it
```
./spotter/target/appassembler/bin/spotter localhost 2181 1 < demo/initSilo.txt
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
```

Execute new Eye connected to replica 2 and verify success in reporting:
```
./eye/target/appassembler/bin/eye localhost 2181 testCam4 12.456789 -8.987654 2
person,89427
person,89399
person,89496
\n
```

Exit Eye (`^C`)

Execute new Eye connected to replica 3 and verify success in reporting:
```
./eye/target/appassembler/bin/eye localhost 2181 testCam5 32.123456 -52.987654 3
person,89427
person,89399
person,89496
\n
```

Exit Eye (`^C`)

In Spotter, verify that:

* Person 1234 was observed
* Car 20SD20 was observed
* Person 0101 was not observed

```
spot person 1234
spot car 20SD20
spot person 0101
```

Verify that:

* person 89427 was spotted by cameras testCam5, testCam4, camera2, camera1
* car 20SD20 was spotted by cameras testCam5, testCam4, camera4, camera3
* person with id 0101 was never spotted

```
trail person 89427
trail car 20SD20
trail person 0101
exit
```


### Case 3: Clear operation and recovery

Connect Spotter to replica 1 and clear
```
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
clear
```

Wait for replicas 2 and 3 to send updates to 1

```
Got gossip from #2
Received 6 updates
Got gossip from #3
Receive 6 updates
```

Verify that:
 * person 89399 was observed by testCam5
 * car 20SD20 was observed by camera4
 
 ```
spot person 89399
spot car 20SD20
exit
```

### Case 4: Coherent readings

Kill all 3 replicas, by pressing `Enter`, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 100;

Launch the 3 replicas, with the `clean` and `compile` commands.

Connect Spotter to replica 1
```
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
```

Add a Camera and an Observation
```
init cams
$ mockCamera1,14.645678,8.534568
$ done
init obs
$ mockCamera1,person,89399
$ mockCamera1,car,20SD21
$ done
```


Verify that:

* person 89399 was observed
* car 20SD21 was observed

```
spot person 89399
trail car 20SD21
```


Disconnect replica 1 by doing `^C` in its terminal.

Run the commands again and verify the client prints a message saying it can't connect to replica 1.
Verify it then returns that:
 * person 89399 was observed
 * car 20SD21 was observed

```
exit
```

### Case 5: Execute more operations with replica 1 still down

Kill all remaining replicas , by pressing `Enter`, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 30;

This time, launch only 2 replicas, replica 2 and replica 3

```
cd silo-server
mvn clean compile exec:java -Dinstance=2
mvn clean compile exec:java -Dinstance=3  
```

Verify that replicas 2 and 3 say `Could not connect to replica #1`

Execute a new Eye connected to a random replica
```
./eye/target/appassembler/bin/eye localhost 2181 testCam5 12.456789 -8.987654
person,9876
car,SDSD20
\n
```
Exit Eye(`^C`)

Execute a new Spotter connected to a random replica
```
./spotter/target/appassembler/bin/spotter localhost 2181
help
```

Ping the replica
```
ping amazing
```

Verify that
 * person 9876 was observed by testCam5
 * car SDSD20 was observed by testCam5
 
 ```
spot person 9876
trail car SDSD20
exit
```

### Case 6: Change number of replicas

Kill all replicas, navigate to `silo-contract/src/resources/main.properties` and change the number of replicas to 4.
In the root of the project run `mvn install -DskipTests` again to recompile the project.

Instantiate 4 new replicas 

```
cd silo-server
mvn exec:java -Dinstance=1
mvn exec:java -Dinstance=2
mvn exec:java -Dinstance=3
mvn exec:java -Dinstance=4  
```

Connect a new Eye to replica 1
```
./eye/target/appassembler/bin/eye localhost 2181 testCam5 12.456789 -8.987654 1
person,9876
car,SDSD20
\n
```
Exit Eye(`^C`)

Verify that replica 1 now sends to other 3 replicas

```
Sending to #2
Sending 2 updates
Sending to #3
Sending 2 updates
Sending to #4
Sending 2 updates
```


Execute a new Spotter connected to a random replica
```
./spotter/target/appassembler/bin/spotter localhost 2181
help
```

Verify that
 * person 9876 was observed by testCam5
 * car SDSD20 was observed by testCam5
 
 ```
spot person 9876
trail car SDSD20
exit
```

Kill all replicas and stop zooKeeper
