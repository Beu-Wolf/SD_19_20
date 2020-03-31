package pt.tecnico.sauron.silo.client.exceptions;

public class CameraAlreadyExistsException extends FrontendException {
    public CameraAlreadyExistsException() { super(ErrorMessages.CAMERA_ALREADY_EXISTS); }
}
