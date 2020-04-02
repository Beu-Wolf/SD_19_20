package pt.tecnico.sauron.silo.domain;

public interface ObservationVisitor {
    boolean visit(Car car);
    boolean visit(Person person);
}
