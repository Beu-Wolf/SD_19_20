package pt.tecnico.sauron.silo.domain.exceptions;

public class InvalidPersonIdException extends SiloException {
    public InvalidPersonIdException() {
        super(ErrorMessages.INVALID_PERSON_ID);
    }
}
