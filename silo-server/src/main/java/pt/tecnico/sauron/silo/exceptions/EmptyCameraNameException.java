package pt.tecnico.sauron.silo.exceptions;

public class EmptyCameraNameException extends SiloException {
    public EmptyCameraNameException() { super(ErrorMessages.EMPTY_CAMERA_NAME); }
}
