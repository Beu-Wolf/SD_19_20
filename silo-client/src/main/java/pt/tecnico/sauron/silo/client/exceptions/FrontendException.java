package pt.tecnico.sauron.silo.client.exceptions;

public class FrontendException extends Exception {
    private String errorMessage;

    public FrontendException(String errorMessage) { this.errorMessage = errorMessage; }
}
