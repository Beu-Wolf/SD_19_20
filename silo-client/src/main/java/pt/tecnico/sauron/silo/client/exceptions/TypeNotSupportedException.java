package pt.tecnico.sauron.silo.client.exceptions;

public class TypeNotSupportedException extends FrontendException {

    public TypeNotSupportedException() {
            super(ErrorMessages.TYPE_NOT_SUPPORTED);
        }

}
