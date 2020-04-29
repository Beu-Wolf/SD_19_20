package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.commands.Command;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;

public class LogEntry {
    private int replicaId;
    private VectorTimestamp ts;
    private Command command;
    private VectorTimestamp prev;
    private String opId;

    public LogEntry() {}
    public LogEntry(int replicaId, VectorTimestamp ts) {
        this.replicaId = replicaId;
        this.ts = ts;
    }

    public int getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(int replicaId) {
        this.replicaId = replicaId;
    }

    public VectorTimestamp getTs() {
        return ts;
    }

    public void setTs(VectorTimestamp ts) {
        this.ts = ts;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public VectorTimestamp getPrev() {
        return prev;
    }

    public void setPrev(VectorTimestamp prev) {
        this.prev = prev;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    public int compareByPrev(LogEntry le) throws InvalidVectorTimestampException {
        return this.prev.lessOrEqualThan(le.getPrev()) ? -1 : // If it is less or equal then it comes first
                this.prev.greaterThan(le.getPrev()) ? 1 : 0;
    }
}
