# Sauron demonstration guide

With the project already installed,

Run the server:
```
cd silo-server
mvn exec:java
```

## Case 1: Load test data using Spotter

```
./spotter/target/appassembler/bin/spotter localhost 8080 < demo/initSilo.txt
```

## Case 2: Register observations using the Eye client

Verify success in reporting:
```
./eye/target/appassembler/bin/eye localhost 8080 testCam2 12.456789 -8.987654
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

## Case 3: Verify Eye's sleep operations

Verify 10 second pause until observation reported:

```
./eye/target/appassembler/bin/eye localhost 8080 testCam3 12.987654 -8.123456
zzz,10000
person,7777
\n
```

Press `^C` to exit client Eye

## Case 4: Usage of Spotter to execute some queries

Verify that the help screen is displayed:

```
./spotter/target/appassembler/bin/spotter localhost 8080
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

## Case 5: Usage of Spotter for control operations

Execute new Spotter:
```
./spotter/target/appassembler/bin/spotter localhost 8080
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