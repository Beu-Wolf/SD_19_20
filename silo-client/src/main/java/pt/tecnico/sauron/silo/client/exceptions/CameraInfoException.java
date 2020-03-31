package pt.tecnico.sauron.silo.client.exceptions;

public class CameraInfoException extends FrontendException {
    public CameraInfoException() { super(ErrorMessages.FAILED_TO_RETRIEVE_CAMERA_INFO); }
}
