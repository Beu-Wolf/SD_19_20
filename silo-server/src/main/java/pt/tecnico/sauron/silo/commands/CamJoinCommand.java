package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.DuplicateCameraNameException;
import pt.tecnico.sauron.silo.exceptions.EmptyCameraNameException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraCoordsException;
import pt.tecnico.sauron.silo.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.grpc.Gossip;

public class CamJoinCommand extends Command {

    private Cam cam;

    public CamJoinCommand(Silo silo, Cam cam) {
        super(silo);
        this.cam = cam;
    }

    public CamJoinCommand(Silo silo, Gossip.CamJoinCommand command) {
        super(silo);
        try {
            this.cam = camFromGRPC(command.getRequest().getCam());
        } catch (EmptyCameraNameException | InvalidCameraNameException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void execute() {
        try {
            this.silo.registerCam(this.cam);
        } catch (DuplicateCameraNameException | InvalidCameraCoordsException e) {
            e.printStackTrace(); // Should we change this??
        }
    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        pt.tecnico.sauron.silo.grpc.Silo.JoinRequest joinRequest = pt.tecnico.sauron.silo.grpc.Silo.JoinRequest.newBuilder().setCam(camToGRPC(this.cam)).build();
        Gossip.CamJoinCommand camJoinCommand = Gossip.CamJoinCommand.newBuilder().setRequest(joinRequest).build();
        return Gossip.Record.newBuilder(record).setCamJoin(camJoinCommand).build();
    }
}