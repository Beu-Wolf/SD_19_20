# Sauron demonstration guide

With the project already installed,

Launch 3 replicas
```
cd silo-server
mvn exec:java -Dinstance=1
mvn exec:java -Dinstance=2
mvn exec:java -Dinstance=3
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
exit
```
 ### Case 6: Replicas updated from previous case
 Execute two new Spotters connected to replicas 2 and 3 
 ```
 ./spotter/target/appassembler/bin/spotter localhost 2181 2
 help
./spotter/target/appassembler/bin/spotter localhost 2181 3
 help
 ```

Verify that both the Spotters have:

 * person 89399 as it's most recent observation at the camera mockCamera2
 * car 20SD21 appears in 2 observations at the cameras MockCamera1 and MockCamera2
 
 ```
 spot person 89399
 trail car 20SD21
 ```

### Case 7: Updates in one replica reflected in another
**TODO: Configure gossip message interval to small value**
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

Execute new Eye connected to replica 3 and verify success in reporting:
```
./eye/target/appassembler/bin/eye localhost 2181 testCam5 32.123456 -52.987654 3
person,89427
person,89399
person,89496
\n
```

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

* person 89427 was spotted by cameras testCam5, testCam4, camera2, camera1
* car 20SD20 was spotted by cameras testCam5, testCam4, camera4, camera3
* person with id 0101 was never spotted

```
trail person 89427
trail car 20SD20
trail person 0101
```


### Case 8: Clear operation propagated

**TODO: Configure gossip message interval to small value**

Connect Spotter to replica 1 and clear
```
./spotter/target/appassembler/bin/spotter localhost 2181 1
help
clear
exit
```


Connect two new Spotters to replicas 2 and 3
```
./spotter/target/appassembler/bin/spotter localhost 2181 2
help
./spotter/target/appassembler/bin/spotter localhost 2181 3
help
```

Verify, in each Spotter, that there are no more people or cars

```
spot person *
spot car *
```

### Case 9: Coherent readings

**TODO: Configure gossip message interval to big value**

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
Then run the same commands and verify the client receives consistent information.

```
spot person 89399
trail car 20SD21
exit
```

### Case 10: Execute more operations with replica 1 still down

**TODO: Set gossip message interval to default value**

Execute a new Eye connected to a random replica
```
./eye/target/appassembler/bin/eye localhost 2181 testCam5 12.456789 -8.987654
person, 9876
car, SDSD20
\n
```

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
 * person 9876 was observed
 * car SDSD20 was observed
 
 ```
spot person 9876
track car SDSD20
exit
```

