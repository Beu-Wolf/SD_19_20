# Sauron demonstration guide

With the project already installed,

Launch 3 replicas
```
cd silo-server
mvn exec:java -Dinstance=1
mvn exec:java -Dinstance=2
mvn exec:java -Dinstance=3
```

Load some data in Replica 1

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
 * car 20SD20 appears in 2 observations at 
 
 ```
 spot person 89399
 trail car 20SD21
exit
 ```

### Case 2: Updates in one replica reflected in another
Kill all 3 replicas, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 5;

Launch the 3 replicas, exactly the same way as in the beginning.

Connect a new Spotter to replica 1 and load some data
```
./spotter/target/appassembler/bin/spotter localhost 2181 1 < demo/initSilo.txt
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
exit
```

Verify that:

* person 89427 was spotted by cameras testCam5, testCam4, camera2, camera1
* car 20SD20 was spotted by cameras testCam5, testCam4, camera4, camera3
* person with id 0101 was never spotted

```
trail person 89427
trail car 20SD20
trail person 0101
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

Kill all 3 replicas, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 100;

Launch the 3 replicas, exactly the same way as in the beginning.

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

Kill all remaining replicas, navigate to `silo-server/src/main/resources/server.properties` and set the `gossipMessageInterval` 
to 30;

This time, launch only 2 replicas

```
cd silo-server
mvn exec:java -Dinstance=1
mvn exec:java -Dinstance=2
```

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
ping good morning
```

Verify that
 * person 9876 was observed by testCam5
 * car SDSD20 was observed by testCam5
 
 ```
spot person 9876
trail car SDSD20
exit
```

