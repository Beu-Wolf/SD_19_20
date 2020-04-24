package pt.tecnico.sauron.silo.exceptions;

public class ObservationNotFoundException extends SiloException {
    public ObservationNotFoundException() { super(ErrorMessages.OBSERVATION_NOT_FOUND); }
}
