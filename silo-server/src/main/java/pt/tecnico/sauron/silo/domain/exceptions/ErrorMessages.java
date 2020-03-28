package pt.tecnico.sauron.silo.domain.exceptions;

public enum ErrorMessages {

    BLANK_INPUT("Input cannot be blank!");

    private String label;

    ErrorMessages(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
