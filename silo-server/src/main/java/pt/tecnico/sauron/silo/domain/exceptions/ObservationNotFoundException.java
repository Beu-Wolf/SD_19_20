package pt.tecnico.sauron.silo.domain.exceptions;

public class ObservationNotFoundException extends SiloException {
    public ObservationNotFoundException() { super(ErrorMessages.OBSERVATION_NOT_FOUND); }
}
