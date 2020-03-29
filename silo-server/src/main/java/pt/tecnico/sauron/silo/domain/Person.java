package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exceptions.InvalidPersonIdException;

public class Person extends Observation {
    public Person(String id) throws InvalidPersonIdException {
        super(id);

        try {
            Long.parseUnsignedLong(id);
        } catch(NumberFormatException e) {
            throw new InvalidPersonIdException();
        }
    }
}
