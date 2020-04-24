package pt.tecnico.sauron.silo.exceptions;

public class InvalidCameraNameException extends SiloException {
    public InvalidCameraNameException() { super(ErrorMessages.INVALID_CAMERA_NAME_SIZE); }
}
