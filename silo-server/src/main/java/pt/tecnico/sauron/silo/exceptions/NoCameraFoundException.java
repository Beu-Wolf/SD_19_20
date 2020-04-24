package pt.tecnico.sauron.silo.exceptions;

public class NoCameraFoundException extends SiloException {
    public NoCameraFoundException() { super(ErrorMessages.NO_CAM_FOUND); }
}
