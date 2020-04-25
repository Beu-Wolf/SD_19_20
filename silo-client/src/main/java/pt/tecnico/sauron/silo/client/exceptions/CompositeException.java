package pt.tecnico.sauron.silo.client.exceptions;

import java.util.ArrayList;
import java.util.List;

public class CompositeException extends FrontendException {
    private List<FrontendException> exceptions = new ArrayList<>();

    public void addException(FrontendException e) { this.exceptions.add(e); }

    @Override
    public String getMessage() {
        String res = "";

        for(FrontendException e : this.exceptions) {
            res += e.getMessage() + "\n";
        }
        return res.trim();
    }
}
