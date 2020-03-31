package pt.tecnico.sauron.silo.client.exceptions;

public class QueryException extends FrontendException {
    public QueryException() {
        super(ErrorMessages.GENERIC_QUERY_ERROR);
    }

    public QueryException(String message) {
        super(message);
    }
}
