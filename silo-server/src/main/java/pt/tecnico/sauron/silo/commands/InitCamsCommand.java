package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraCoordsException;

import java.util.LinkedList;

public class InitCamsCommand extends Command {

    private LinkedList<Cam> camList;

    InitCamsCommand(Silo silo, LinkedList<Cam> camList) {
        super(silo);
        this.camList = camList;
    }

    @Override
    public void execute() {
        try {
            for (Cam cam: camList) {
                silo.registerCam(cam);
            }
        } catch (DuplicateCameraNameException | InvalidCameraCoordsException e) {
            e.printStackTrace();
        }


    }
}
