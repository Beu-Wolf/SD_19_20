package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraCoordsException;

public class CamJoinCommand extends Command {

    private Cam cam;

    public CamJoinCommand(Silo silo, Cam cam) {
        super(silo);
        this.cam = cam;
    }

    @Override
    public void execute() {
        try {
            this.silo.registerCam(this.cam);
        } catch (DuplicateCameraNameException | InvalidCameraCoordsException e) {
            e.printStackTrace(); // Should we change this??
        }


    }

}
