package pt.tecnico.sauron.silo;


import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.exceptions.SiloException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GossipStructures {

    private static final int NUM_REPLICAS = 3;

    private VectorTimestamp replicaTS = new VectorTimestamp(new int[NUM_REPLICAS]);
    private VectorTimestamp valueTS = new VectorTimestamp(new int[NUM_REPLICAS]) ;
    private ConcurrentLinkedDeque<String> executedOperations = new ConcurrentLinkedDeque<>();
    // Maybe change to Map<instance, VectorTimestamp>
    private ArrayList<VectorTimestamp> timestampTable = new ArrayList<>();
    private LinkedList<LogEntry> updateLog = new LinkedList<>();
    private int instance;

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

    public VectorTimestamp getTimestampTableRow(int index) {return this.timestampTable.get(index);}

    public void setTimestampTable(ArrayList<VectorTimestamp> timestampTable) {
        this.timestampTable = timestampTable;
    }

    public void setTimestampTableRow(int index, VectorTimestamp newTS) {  this.timestampTable.set(index, newTS); }

    public LinkedList<LogEntry> getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(LinkedList<LogEntry> updateLog) {
        this.updateLog = updateLog;
    }

    public void addLogEntry(LogEntry le) {
        this.updateLog.add(le);
    }

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public void updateStructuresAndExec(LogEntry stableLogEntry) throws SiloException {
        // aplly update to silo
        if (!this.executedOperations.contains(stableLogEntry.getOpId())) {
            stableLogEntry.getCommand().execute();
            updateStructures(stableLogEntry);
        }
    }

    public void updateStructures(LogEntry stableLogEntry) {
        try {
            if (!this.executedOperations.contains(stableLogEntry.getOpId())) {
                // merge valueTS
                this.valueTS.merge(stableLogEntry.getTs());
                // add the opId to the table
                this.executedOperations.add(stableLogEntry.getOpId());
            }
        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
        }
    }

    public void clearAll() {
        this.setReplicaTS(new VectorTimestamp(new int[NUM_REPLICAS]));
        this.setValueTS(new VectorTimestamp(new int[NUM_REPLICAS]));
        this.setExecutedOperations(new ConcurrentLinkedDeque<>());
        this.setUpdateLog( new LinkedList<>());
        this.setTimestampTable(new ArrayList<>());
    }



}
