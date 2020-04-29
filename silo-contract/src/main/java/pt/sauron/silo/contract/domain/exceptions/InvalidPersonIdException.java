package pt.sauron.silo.contract.domain.exceptions;

public class InvalidPersonIdException extends SiloException {
    public InvalidPersonIdException() {
        super(ErrorMessages.INVALID_PERSON_ID);
    }
    public InvalidPersonIdException(String invalidId) {
        super(invalidId + ": " + ErrorMessages.INVALID_PERSON_ID);
    }
}
