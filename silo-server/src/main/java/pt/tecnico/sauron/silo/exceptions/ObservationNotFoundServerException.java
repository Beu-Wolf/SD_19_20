package pt.tecnico.sauron.silo.exceptions;

import pt.sauron.silo.contract.domain.exceptions.SiloException;

public class ObservationNotFoundServerException extends SiloException {
    public ObservationNotFoundServerException() { super(ErrorMessages.OBSERVATION_NOT_FOUND); }
}
