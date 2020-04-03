# Sauron

Distributed Systems 2019-2020, 2nd semester project


## Authors

**Group A04**


### Team members


| Number | Name              | User                             | Email                                                     |
| -------|-------------------|----------------------------------| ----------------------------------------------------------|
| 89399  | Afonso Gonçalves  | <https://github.com/afonsocrg>   | <mailto:afonso.corte-real.goncalves@tecnico.ulisboa.pt>   |
| 89427  | Daniel Seara      | <https://github.com/Beu-Wolf>    | <mailto:daniel.g.seara@tecnico.ulisboa.pt>                |
| 89496  | Marcelo Santos    | <https://github.com/tosmarcel>   | <mailto:marcelocmsantos@tecnico.ulisboa.pt>               |

### Task leaders


| Task set | To-Do                         | Leader              |
| ---------|-------------------------------| --------------------|
| core     | protocol buffers, silo-client | _(whole team)_      |
| T1       | cam_join, cam_info, eye       | _Afonso Gonçalves_  |
| T2       | report, spotter               | _Daniel Seara_      |
| T3       | track, trackMatch, trace      | _Marcelo Santos_    |
| T4       | test T1                       | _Daniel Seara_      |
| T5       | test T2                       | _Marcelo Santos_    |
| T6       | test T3                       | _Afonso Gonçalves_  |


## Getting Started

The overall system is composed of multiple modules.
The main server is the _silo_.
The clients are the _eye_ and _spotter_.

See the [project statement](https://github.com/tecnico-distsys/Sauron/blob/master/README.md) for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require the servers to be running.

### Running the program

To begin the server
```
cd silo-server/
mvn exec:java
```

To begin the eye client, open a new terminal and navigate to the project's root directory
```
./eye/target/appassembler/bin/eye host port cameraName latitude longitude [< file.txt]
```
where:
   * host = hostname of machine running the server (example: localhost)
   * port = port where the server is listening (example: 8080)
   * cameraName = name of camera to register 
   * latitude = camera valid latitude in decimal notation (ex: 12.123456)
   * longitude = camera valid longitude in decimal notation (ex:8.980657)
   * file.txt = optional path to file with commands to execute

To begin the spotter client, open a new terminal and navigate to the project's root directory
```
./spotter/target/appassembler/bin/spotter host port [< file.txt]
```
where:
   * host = hostname of machine running the server (example: localhost)
   * port = port where the server is listening (example: 8080)
   * file.txt = optional path to file with commands to execute

##Concurrency

Regarding the concurrent model, we decided to use thread-safe data structures for reading and writing. 
Declaring the functions as `synchronized` was worse because they wouldn't be fine-grained and could
introduce concurrency bottlenecks. It was used a `concurrentLinkedDeque` 
to store the reports and a `concurrentHashMap` to register the cameras.

## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
