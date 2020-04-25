package pt.tecnico.sauron.silo.exceptions;

import java.util.ArrayList;
import java.util.List;

public class CompositeSiloException extends SiloException {
    private List<SiloException> exceptions = new ArrayList<>();

    public void addException(SiloException e) {
        exceptions.add(e);
    }

    public boolean isEmpty() { return this.exceptions.isEmpty(); }

    @Override
    public String getMessage() {
        String res = "";
        for(SiloException e : this.exceptions) {
            res += e.getMessage() + "\n";
        }
        return res.trim();
    }
}
