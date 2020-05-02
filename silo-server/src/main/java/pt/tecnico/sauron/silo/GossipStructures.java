package pt.tecnico.sauron.silo;


import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.exceptions.SiloException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GossipStructures {


    private VectorTimestamp replicaTS;
    private VectorTimestamp valueTS;
    private ConcurrentLinkedDeque<String> executedOperations = new ConcurrentLinkedDeque<>();
    private ArrayList<VectorTimestamp> timestampTable = new ArrayList<>();
    private LinkedList<LogEntry> updateLog = new LinkedList<>();
    private int instance;
    private int numReplicas;

    public GossipStructures(int numReplicas) {
        this.numReplicas = numReplicas;
        this.replicaTS = new VectorTimestamp(this.numReplicas);
        this.valueTS = new VectorTimestamp(this.numReplicas) ;
        for (int i = 0; i < this.numReplicas; i++) {
            timestampTable.add(new VectorTimestamp(this.numReplicas));
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
        if (!this.updateLog.contains(le))
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

    public boolean logContainsOp(String opId) {
        for (LogEntry le : this.updateLog) {
            if (le.getOpId().equals(opId))
                return true;
        }
        return false;
    }

    public void clearAll() {
        this.setReplicaTS(new VectorTimestamp(this.numReplicas));
        this.setValueTS(new VectorTimestamp(this.numReplicas));
        this.setExecutedOperations(new ConcurrentLinkedDeque<>());
        this.setUpdateLog( new LinkedList<>());
        for (int i = 0; i < this.timestampTable.size(); i++) {
            this.timestampTable.set(i, new VectorTimestamp(this.numReplicas));
        }
    }



}
