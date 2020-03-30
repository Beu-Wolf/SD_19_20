package pt.tecnico.sauron.silo.domain.exceptions;

public class NoCameraFoundException extends SiloException {
    public NoCameraFoundException() { super(ErrorMessages.NO_CAM_FOUND); }
}
