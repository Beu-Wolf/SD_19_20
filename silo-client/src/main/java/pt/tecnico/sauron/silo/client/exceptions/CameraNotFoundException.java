package pt.tecnico.sauron.silo.client.exceptions;

public class CameraNotFoundException extends FrontendException {
    public CameraNotFoundException() { super(ErrorMessages.CAMERA_NOT_FOUND); }
}
