package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exceptions.InvalidCarIdException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Car extends Observation {
    public Car(String id) throws InvalidCarIdException {
        super(id);

        checkLicense(id);
    }

    public boolean equals(Object o) {
        return o instanceof Car && ((Car) o).getId().equals(this.getId());
    }

    private void checkLicense(String id) throws InvalidCarIdException {
        int charGroupCount = 0;
        int numGroupCount = 0;
        Pattern charPattern = Pattern.compile("^[A-Z]{2}$");
        Pattern numPattern = Pattern.compile("^[0-9]{2}$");;

        boolean valid = false;

        if (id.length() == 6) {
            String[] subgroups = id.split("(?<=\\G.{2})");

            for (String subgroup : subgroups) {
                Matcher charMatcher = charPattern.matcher(subgroup);
                Matcher numMatcher = numPattern.matcher(subgroup);

                if (charMatcher.find()) charGroupCount++;
                if (numMatcher.find()) numGroupCount++;
            }

            if (numGroupCount == 2 && charGroupCount == 1 ||
                    numGroupCount == 1 && charGroupCount == 2) {
                valid = true;
            }
        }

        if (!valid) throw new InvalidCarIdException();
    }

    public boolean matches(ObservationVisitor o) {
        return o.visit(this);
    }
}
