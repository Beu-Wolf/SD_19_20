package pt.tecnico.sauron.silo.contract;

import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;

import java.util.ArrayList;
import java.util.Collection;

public class VectorTimestamp {
    // TODO: Maybe change to Map<instance:value> ?
    ArrayList<Integer> values = new ArrayList<>();

    public VectorTimestamp(int size) {
        this(new int[size]);
    }

    public VectorTimestamp(Collection<Integer> l) {
        this.values.addAll(l);
    }

    public VectorTimestamp(int[] l) {
        for (Integer i : l) {
            values.add(i);
        }
    }

    public void set(int index, int value) {
        this.values.set(index, value);
    }

    public Integer get(int index) {
        return values.get(index);
    }

    public ArrayList<Integer> getValues() { return values; }

    public void merge(VectorTimestamp vec) throws InvalidVectorTimestampException {
        ArrayList<Integer> vals = vec.getValues();

        if (this.values.size() != vals.size()) {
            throw new InvalidVectorTimestampException();
        }

        int lim = this.values.size();
        for (int i = 0; i < lim; i++) {
            int val = vals.get(i);
            if (val > this.values.get(i)) {
                this.values.set(i, val);
            }
        }
    }

    public boolean lessOrEqualThan(VectorTimestamp vec) throws InvalidVectorTimestampException {
        if (this.values.size() != vec.getValues().size()) {
            throw new InvalidVectorTimestampException();
        }

        int lim = this.values.size();
        for (int i = 0; i < lim; i++) {
            if (this.values.get(i) > vec.get(i)) return false;
        }

        return true;
    }

    public boolean greaterThan(VectorTimestamp vec) throws InvalidVectorTimestampException {
        if (this.values.size() != vec.getValues().size()) {
            throw new InvalidVectorTimestampException();
        }

        int lim = this.values.size();
        for (int i = 0; i < lim; i++) {
            if (this.values.get(i) <= vec.get(i)) return false;
        }

        return true;
    }

    public boolean greaterOrEqualThan(VectorTimestamp vec) throws InvalidVectorTimestampException {
        if (this.values.size() != vec.getValues().size()) {
            throw new InvalidVectorTimestampException();
        }

        int lim = this.values.size();
        for (int i = 0; i < lim; i++) {
            if (this.values.get(i) < vec.get(i)) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.values.toString();
    }
}
