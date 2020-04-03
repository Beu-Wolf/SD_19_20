package pt.tecnico.sauron.silo.client.exceptions;

public class NotFoundException extends FrontendException {
    public NotFoundException() { super(ErrorMessages.OBSERVATION_NOT_FOUND); }
}
