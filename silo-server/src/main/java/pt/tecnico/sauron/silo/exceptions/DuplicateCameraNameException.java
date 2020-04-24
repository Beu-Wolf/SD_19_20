package pt.tecnico.sauron.silo.exceptions;

public class DuplicateCameraNameException extends SiloException {
    public DuplicateCameraNameException() {
        super(ErrorMessages.DUPLICATE_CAMERA_NAME_EXCEPTION);
    }
}
