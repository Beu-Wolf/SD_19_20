package pt.tecnico.sauron.silo.exceptions;

import pt.sauron.silo.contract.domain.exceptions.SiloException;

public class TypeNotSupportedServerException extends SiloException {
    public TypeNotSupportedServerException() {
        super(ErrorMessages.TYPE_NOT_SUPPORTED);
    }
}
