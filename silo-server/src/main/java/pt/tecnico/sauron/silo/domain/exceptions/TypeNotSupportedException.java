package pt.tecnico.sauron.silo.domain.exceptions;

public class TypeNotSupportedException extends SiloException {
    public TypeNotSupportedException() {
        super(ErrorMessages.TYPE_NOT_SUPPORTED);
    }
}
