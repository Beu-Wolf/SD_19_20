package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.exceptions.InvalidPersonIdException;

public class Person extends Observation {
    public Person(String id) throws InvalidPersonIdException {
        super(id);

        try {
            Long.parseUnsignedLong(id);
        } catch(NumberFormatException e) {
            throw new InvalidPersonIdException(id);
        }
    }

    public boolean equals(Object o) {
        return o instanceof Person && ((Person) o).getId().equals(this.getId());
    }

    public boolean matches(ObservationVisitor o) {
        return o.visit(this);
    }
}
