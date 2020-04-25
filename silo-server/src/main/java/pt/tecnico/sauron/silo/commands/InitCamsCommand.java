package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.exceptions.EmptyCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraCoordsException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.grpc.Gossip;

import java.util.LinkedList;

public class InitCamsCommand extends Command {

    private LinkedList<Cam> camList;

    public InitCamsCommand(Silo silo, LinkedList<Cam> camList) {
        super(silo);
        this.camList = camList;
    }

    public InitCamsCommand(Silo silo, Gossip.InitCamsCommand command) {
        super(silo);
        try {
            LinkedList<Cam> newCamList = new LinkedList<>();
            for (pt.tecnico.sauron.silo.grpc.Silo.Cam c : command.getRequest().getCamsList()) {
                newCamList.add(camFromGRPC(c));
            }
            this.camList = newCamList;
        } catch (InvalidCameraNameException | EmptyCameraNameException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void execute() {
        try {
            for (Cam cam: camList) {
                silo.registerCam(cam);
            }
        } catch (DuplicateCameraNameException | InvalidCameraCoordsException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.Cam> siloCam = new LinkedList<>();
        for (Cam c : camList) {
            siloCam.add(camToGRPC(c));
        }
        Gossip.InitCamsRequest initCamsRequest = Gossip.InitCamsRequest.newBuilder()
                .addAllCams(siloCam)
                .build();
        Gossip.InitCamsCommand initCamsCommand = Gossip.InitCamsCommand.newBuilder()
                .setRequest(initCamsRequest)
                .build();
        return Gossip.Record.newBuilder(record)
                .setInitCams(initCamsCommand)
                .build();
    }
}
