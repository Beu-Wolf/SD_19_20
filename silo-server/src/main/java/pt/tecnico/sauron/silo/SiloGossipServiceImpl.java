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
            mergeLogs(request.getRecordsList());
            //merge receiver replicaTS with senderReplicaTS
            mergeReplicaTS(request.getReplicaTimeStamp());
            //find and apply updates
            applyUpdates();
            // discard updates
            discardUpdates(vectorTimestampFromGRPC(request.getReplicaTimeStamp()), request.getSenderId());
            //maybe discard from execution operations table
            responseObserver.onNext(Gossip.GossipResponse.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
        }
    }


    private void mergeLogs(List<Gossip.Record> records) {

        try {
            for (Gossip.Record record : records) {
                LogEntry logEntry = recordToLogEntry(record);
                if (!logEntry.getTs().lessOrEqualThan(gossipStructures.getReplicaTS())) {
                    gossipStructures.addLogEntry(logEntry);
                }
            }
        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
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

    private void discardUpdates(VectorTimestamp senderReplicaTS, int senderId) {
        // update tableTimestamp[sender]
        gossipStructures.setTimestampTableRow(senderId-1, senderReplicaTS); // We assume an instance starts at one

        // Iterate over the original and remove safely
        for (LogEntry le: new LinkedList<>(gossipStructures.getUpdateLog())) {
            for (int i = 0; i < this.gossipStructures.getTimestampTable().size(); i++) {
                // If the value of the TS of the replica that recorded the update is greater or equal
                // then the records replicaTS at that same index, it's safe to remove
                if (i != this.gossipStructures.getInstance()-1) {
                    if (this.gossipStructures.getTimestampTableRow(i).get(le.getReplicaId()-1) >= le.getTs().get(le.getReplicaId()-1)) {
                        gossipStructures.getUpdateLog().remove(le);
                    }
                }

            }
        }
    }

    //==========================================================
    //                  GRPC to DOMAIN
    //=========================================================
    private VectorTimestamp vectorTimestampFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp timestamp) {
        return new VectorTimestamp(timestamp.getTimestampsList());
    }

    private LogEntry recordToLogEntry(Gossip.Record record) {
        LogEntry le = new LogEntry();
        le.setOpId(record.getOpId());
        le.setReplicaId(record.getReplicaId());
        le.setPrev(vectorTimestampFromGRPC(record.getPrev()));
        le.setTs(vectorTimestampFromGRPC(record.getTs()));
        // get command
        le.setCommand(getCommandFromGRPC(record));
        return le;
    }

    private Command getCommandFromGRPC(Gossip.Record record) {
        try {
            switch (record.getCommandsCase()) {
                case CLEAR:
                    return new ClearCommand(this.silo);
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
