package pt.sauron.silo.contract.domain.exceptions;

public class SiloException extends Exception {
    public SiloException() {super(); }
    public SiloException(String errorMessage) {super(errorMessage); }
}
