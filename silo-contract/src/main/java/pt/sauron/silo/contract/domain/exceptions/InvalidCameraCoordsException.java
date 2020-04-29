package pt.sauron.silo.contract.domain.exceptions;

public class InvalidCameraCoordsException extends SiloException {
    public InvalidCameraCoordsException() { super(ErrorMessages.INVALID_COORDS); }
}
