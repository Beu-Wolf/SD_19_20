package pt.tecnico.sauron.silo.domain.exceptions;

public class EmptyCameraNameException extends SiloException {
    public EmptyCameraNameException() { super(ErrorMessages.EMPTY_CAMERA_NAME); }
}
