package pt.tecnico.sauron.silo.exceptions;

public class InvalidPersonIdException extends SiloInvalidArgumentException {
    public InvalidPersonIdException() {
        super(ErrorMessages.INVALID_PERSON_ID);
    }
    public InvalidPersonIdException(String invalidId) {
        super(invalidId + ": " + ErrorMessages.INVALID_PERSON_ID);
    }
}
