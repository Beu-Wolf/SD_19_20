package pt.tecnico.sauron.silo.exceptions;

import pt.sauron.silo.contract.domain.exceptions.SiloException;

public class NoCameraFoundServerException extends SiloException {
    public NoCameraFoundServerException() { super(ErrorMessages.NO_CAM_FOUND); }
}
