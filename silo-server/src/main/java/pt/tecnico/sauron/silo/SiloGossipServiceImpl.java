package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.commands.*;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.SiloException;
import pt.tecnico.sauron.silo.grpc.Gossip;
import pt.tecnico.sauron.silo.grpc.GossipServiceGrpc;

import java.util.*;


public class SiloGossipServiceImpl extends GossipServiceGrpc.GossipServiceImplBase {

    private Silo silo;
    GossipStructures gossipStructures;


    SiloGossipServiceImpl(Silo silo, GossipStructures gossipStructures) {
        this.silo = silo;
        this.gossipStructures = gossipStructures;
    }

    @Override
    public void gossip(Gossip.GossipRequest request, StreamObserver<Gossip.GossipResponse> responseObserver) {
        try {
            //merge with receiver update log
            System.out.println("Got gossip from " + request.getSenderId());
            System.out.println("Received " + request.getRecordsList().size() + " updates");
            mergeLogs(request.getRecordsList());
            //merge receiver replicaTS with senderReplicaTS
            mergeReplicaTS(request.getReplicaTimeStamp());
            //find and apply updates
            applyUpdates();
            //Update timestampTable
            this.gossipStructures.setTimestampTableRow(request.getSenderId()-1, vectorTimestampFromGRPC(request.getReplicaTimeStamp()));

            responseObserver.onNext(Gossip.GossipResponse.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
        }
    }


    private void mergeLogs(List<Gossip.Record> records) {
        for (Gossip.Record record : records) {
            gossipStructures.addLogEntry(recordToLogEntry(record));

        }
    }

    private void mergeReplicaTS(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp senderReplicaVecTS) throws InvalidVectorTimestampException {
        VectorTimestamp senderReplicaTS = vectorTimestampFromGRPC(senderReplicaVecTS);
        gossipStructures.getReplicaTS().merge(senderReplicaTS);
    }

    private void applyUpdates() {
        // Order updates by prev, and execute stable ones
        try {
            this.gossipStructures.getUpdateLog().sort((logEntry, logEntry2) -> {
                try {
                    return logEntry.compareByPrev(logEntry2);
                } catch (InvalidVectorTimestampException e) {
                    System.out.println(e.getMessage()); // We need to this because compare doesn't allow any other exceptions to be thrown
                    throw new ClassCastException(); // If the sizes are different, they are not comparable
                }
            });
            // for each update, merge structures and execute
            for(LogEntry logEntry: this.gossipStructures.getUpdateLog()) {
                if (logEntry.getPrev().lessOrEqualThan(this.gossipStructures.getValueTS())) {
                    this.gossipStructures.updateStructuresAndExec(logEntry);
                }
            }
        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
        } catch (SiloException e) {
            // should never happen, all updates should be good
            System.out.println(e.getMessage());
        }
    }

    //==========================================================
    //                  GRPC to DOMAIN
    //=========================================================
    private VectorTimestamp vectorTimestampFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp timestamp) {
        return new VectorTimestamp(timestamp.getTimestampsList());
    }

    private LogEntry recordToLogEntry(Gossip.Record record) {
        return new LogEntry(record.getReplicaId(),
                record.getOpId(),
                vectorTimestampFromGRPC(record.getPrev()),
                vectorTimestampFromGRPC(record.getTs()),
                getCommandFromGRPC(record));
    }

    private Command getCommandFromGRPC(Gossip.Record record) {
        try {
            switch (record.getCommandsCase()) {
                case REPORT:
                    return new ReportCommand(this.silo, record.getReport());
                case CAMJOIN:
                    return new CamJoinCommand(this.silo, record.getCamJoin());
                case INITCAMS:
                    return new InitCamsCommand(this.silo, record.getInitCams());
                case INITOBSERVATIONS:
                    return new InitObsCommand(this.silo, record.getInitObservations());
                default:
                    return null;
            }
        } catch (SiloException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
