package pt.sauron.silo.contract.domain;

public interface ObservationVisitor {
    boolean visit(Car car);
    boolean visit(Person person);
}
