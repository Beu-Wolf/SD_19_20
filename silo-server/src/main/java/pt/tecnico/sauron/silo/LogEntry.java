package pt.tecnico.sauron.silo;

public class LogEntry {
    private int instance;
    private int[] ts;
    //private Command command;
    private int[] prev;
    private String opId;

    public LogEntry() {};
    public LogEntry(int instance, int[] ts) {
        this.instance = instance;
        this.ts = ts;
    }

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public int[] getTs() {
        return ts;
    }

    public void setTs(int[] ts) {
        this.ts = ts;
    }

    public int[] getPrev() {
        return prev;
    }

    public void setPrev(int[] prev) {
        this.prev = prev;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }
}
