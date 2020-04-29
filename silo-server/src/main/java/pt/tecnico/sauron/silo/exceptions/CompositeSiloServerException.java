package pt.tecnico.sauron.silo.exceptions;

import pt.sauron.silo.contract.domain.exceptions.SiloException;

import java.util.ArrayList;
import java.util.List;

public class CompositeSiloServerException extends Exception {
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
