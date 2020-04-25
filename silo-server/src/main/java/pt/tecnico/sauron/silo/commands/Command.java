package pt.tecnico.sauron.silo.commands;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.Gossip;

import java.time.Instant;

public abstract class Command {
    protected Silo silo;

    public Command(Silo silo) { this.silo = silo; }

    public abstract void execute();

    public abstract Gossip.Record commandToGRPC(Gossip.Record record);

    // public abstract Command GRPCToCommand()

    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================

    protected pt.tecnico.sauron.silo.grpc.Silo.Cam camToGRPC(Cam cam) {
        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder()
                .setName(cam.getName())
                .setCoords(coordsToGRPC(cam.getCoords()))
                .build();
    }

    protected LatLng coordsToGRPC(Coords coords) {
        return LatLng.newBuilder()
                .setLatitude(coords.getLat())
                .setLongitude(coords.getLon())
                .build();
    }


    protected pt.tecnico.sauron.silo.grpc.Silo.Observation observationToGRPC(Observation observation) throws SiloInvalidArgumentException {
        return pt.tecnico.sauron.silo.grpc.Silo.Observation.newBuilder()
                .setType(domainObservationToTypeGRPC(observation))
                .setObservationId(observation.getId()).build();
    }

    protected pt.tecnico.sauron.silo.grpc.Silo.ObservationType domainObservationToTypeGRPC(Observation observation) throws SiloInvalidArgumentException {
        if (observation instanceof Car) {
            return pt.tecnico.sauron.silo.grpc.Silo.ObservationType.CAR;
        } else if (observation instanceof Person) {
            return pt.tecnico.sauron.silo.grpc.Silo.ObservationType.PERSON;
        } else {
            throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    protected Timestamp timestampToGRPC(Instant timestamp) {
        return Timestamp.newBuilder()
                .setSeconds(timestamp.getEpochSecond())
                .build();
    }

    private Coords coordsFromGRPC(LatLng coords) {
        return new Coords(coords.getLatitude(), coords.getLongitude());
    }

    protected Cam camFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Cam cam) throws EmptyCameraNameException, InvalidCameraNameException {
        String name = cam.getName();
        Coords coords = coordsFromGRPC(cam.getCoords());
        return new Cam(name, coords);
    }

    protected Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws InvalidCarIdException, InvalidPersonIdException, TypeNotSupportedException {
        pt.tecnico.sauron.silo.grpc.Silo.ObservationType type = observation.getType();
        String id = observation.getObservationId();
        switch (type) {
            case CAR:
                return new Car(id);
            case PERSON:
                return new Person(id);
            default:
                throw new TypeNotSupportedException();
        }
    }

    protected Instant timestampFromGRPC(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds());
    }

}
