package pt.tecnico.sauron.silo.client.exceptions;

public class CameraRegisterException extends FrontendException {
    public CameraRegisterException(String m) { super(m); }
    public CameraRegisterException() { super(ErrorMessages.FAILED_TO_REGISTER_CAMERA); }
}
