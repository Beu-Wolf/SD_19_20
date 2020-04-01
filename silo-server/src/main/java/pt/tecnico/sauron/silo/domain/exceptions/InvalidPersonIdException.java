package pt.tecnico.sauron.silo.domain.exceptions;

public class InvalidPersonIdException extends SiloInvalidArgumentException {
    public InvalidPersonIdException() {
        super(ErrorMessages.INVALID_PERSON_ID);
    }
}
