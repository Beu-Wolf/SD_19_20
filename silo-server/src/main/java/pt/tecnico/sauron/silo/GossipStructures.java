package pt.tecnico.sauron.silo;


import pt.tecnico.sauron.silo.contract.VectorTimestamp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GossipStructures {

    private static final int NUM_REPLICAS = 3;

    private VectorTimestamp replicaTS = new VectorTimestamp(new int[NUM_REPLICAS]);
    private VectorTimestamp valueTS = new VectorTimestamp(new int[NUM_REPLICAS]) ;
    private ConcurrentLinkedDeque<String> executedOperations = new ConcurrentLinkedDeque<>();
    private ArrayList<VectorTimestamp> timestampTable = new ArrayList<>();

    public GossipStructures() {
        for (int i = 0; i < NUM_REPLICAS; i++) {
            timestampTable.add(new VectorTimestamp(new int[NUM_REPLICAS]));
        }
    }

    public VectorTimestamp getReplicaTS() {
        return replicaTS;
    }

    public void setReplicaTS(VectorTimestamp replicaTS) {
        this.replicaTS = replicaTS;
    }

    public VectorTimestamp getValueTS() {
        return valueTS;
    }

    public void setValueTS(VectorTimestamp valueTS) {
        this.valueTS = valueTS;
    }

    public ConcurrentLinkedDeque<String> getExecutedOperations() {
        return executedOperations;
    }

    public void setExecutedOperations(ConcurrentLinkedDeque<String> executedOperations) {
        this.executedOperations = executedOperations;
    }

    public ArrayList<VectorTimestamp> getTimestampTable() {
        return timestampTable;
    }

    public void setTimestampTable(ArrayList<VectorTimestamp> timestampTable) {
        this.timestampTable = timestampTable;
    }

    public LinkedList<LogEntry> getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(LinkedList<LogEntry> updateLog) {
        this.updateLog = updateLog;
    }

    public void addLogEntry(LogEntry le) {
        this.updateLog.add(le);
    }

    private LinkedList<LogEntry> updateLog = new LinkedList<>();



}