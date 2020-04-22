package pt.tecnico.sauron.silo.domain.exceptions;

public class InvalidCameraCoordsException extends SiloException {
    public InvalidCameraCoordsException() { super(ErrorMessages.INVALID_COORDS); }
}
