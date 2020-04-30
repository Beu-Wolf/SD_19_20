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
    public LogEntry(int replicaId, String opId, VectorTimestamp prev,  VectorTimestamp ts) {
        this.replicaId = replicaId;
        this.opId = opId;
        this.prev = prev;
        this.ts = ts;
    }

    public LogEntry(int replicaId, String opId, VectorTimestamp prev,  VectorTimestamp ts, Command command) {
        this(replicaId, opId, prev, ts);
        this.command = command;
    }


    public int getReplicaId() {
        return replicaId;
    }

    public VectorTimestamp getTs() {
        return ts;
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

    public String getOpId() {
        return opId;
    }

    public int compareByPrev(LogEntry le) throws InvalidVectorTimestampException {
        return this.prev.lessOrEqualThan(le.getPrev()) ? -1 : // If it is less or equal then it comes first
                this.prev.greaterThan(le.getPrev()) ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
        LogEntry le = (LogEntry) obj;
        return obj instanceof LogEntry &&
                le.opId.equals(this.opId) && le.replicaId == this.replicaId;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "replicaId=" + replicaId +
                ", ts=" + ts +
                ", command=" + command.getClass().getName() +
                ", prev=" + prev +
                ", opId='" + opId + '\'' +
                '}';
    }
}
