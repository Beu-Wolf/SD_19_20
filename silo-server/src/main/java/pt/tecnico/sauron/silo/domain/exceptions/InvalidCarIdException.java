package pt.tecnico.sauron.silo.domain.exceptions;

public class InvalidCarIdException extends SiloInvalidArgumentException {
    public InvalidCarIdException() {
        super(ErrorMessages.INVALID_CAR_ID);
    }
}
