package pt.tecnico.sauron.silo.domain.exceptions;

public class DuplicateCameraNameException extends SiloException {
    public DuplicateCameraNameException() {
        super(ErrorMessages.DUPLICATE_CAMERA_NAME_EXCEPTION);
    }
}
