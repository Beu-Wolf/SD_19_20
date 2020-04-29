package pt.sauron.silo.contract.domain.exceptions;

public class InvalidCarIdException extends SiloException {
    public InvalidCarIdException() {
        super(ErrorMessages.INVALID_CAR_ID);
    }

    public InvalidCarIdException(String invalidId) {
        super(invalidId + ": " + ErrorMessages.INVALID_CAR_ID);
    }
}
