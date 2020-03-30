package pt.tecnico.sauron.silo.domain.exceptions;

public class SiloException extends Exception {
    private String errorMessage;

    public SiloException(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String getMessage() { return this.errorMessage; }
}
