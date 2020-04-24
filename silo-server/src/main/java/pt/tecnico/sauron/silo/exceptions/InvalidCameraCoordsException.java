package pt.tecnico.sauron.silo.exceptions;

public class InvalidCameraCoordsException extends SiloException {
    public InvalidCameraCoordsException() { super(ErrorMessages.INVALID_COORDS); }
}
