package pt.tecnico.sauron.silo.commands;

import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.Gossip;

import java.util.LinkedList;

public class InitCamsCommand extends Command {

    private LinkedList<Cam> camList;

    public InitCamsCommand(Silo silo, LinkedList<Cam> camList) {
        super(silo);
        this.camList = camList;
    }

    public InitCamsCommand(Silo silo, Gossip.InitCamsCommand command) throws SiloException {
        super(silo);
        LinkedList<Cam> newCamList = new LinkedList<>();
        for (pt.tecnico.sauron.silo.grpc.Silo.Cam c : command.getRequest().getCamsList()) {
            newCamList.add(camFromGRPC(c));
        }
        this.camList = newCamList;
    }

    @Override
    public void execute() throws SiloException{
        for (Cam cam: camList) {
            silo.registerCam(cam);
        }

    }

    @Override
    public Gossip.Record commandToGRPC(Gossip.Record record) {
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.Cam> siloCam = new LinkedList<>();
        for (Cam c : camList) {
            siloCam.add(camToGRPC(c));
        }
        pt.tecnico.sauron.silo.grpc.Silo.InitCamsRequest initCamsRequest = pt.tecnico.sauron.silo.grpc.Silo.InitCamsRequest.newBuilder()
                .addAllCams(siloCam)
                .build();
        Gossip.InitCamsCommand initCamsCommand = Gossip.InitCamsCommand.newBuilder()
                .setRequest(initCamsRequest)
                .build();
        return Gossip.Record.newBuilder(record)
                .setInitCams(initCamsCommand)
                .build();
    }

    @Override
    public String toString() {
        return "InitCamsCommand{" +
                "camList=" + camList +
                '}';
    }
}
