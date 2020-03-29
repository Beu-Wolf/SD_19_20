package pt.tecnico.sauron.silo.domain.exceptions;

public class InvalidCarIdException extends SiloException {
    public InvalidCarIdException() {
        super(ErrorMessages.INVALID_CAR_ID);
    }
}
