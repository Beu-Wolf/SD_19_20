package pt.tecnico.sauron.silo.exceptions;

import pt.sauron.silo.contract.domain.exceptions.SiloException;

public class DuplicateCameraNameServerException extends SiloException {
    public DuplicateCameraNameServerException() {
        super(ErrorMessages.DUPLICATE_CAMERA_NAME_EXCEPTION);
    }
}
