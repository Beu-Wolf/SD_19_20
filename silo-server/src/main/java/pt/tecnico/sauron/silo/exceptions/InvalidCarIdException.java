package pt.tecnico.sauron.silo.exceptions;

public class InvalidCarIdException extends SiloInvalidArgumentException {
    public InvalidCarIdException() {
        super(ErrorMessages.INVALID_CAR_ID);
    }
}
