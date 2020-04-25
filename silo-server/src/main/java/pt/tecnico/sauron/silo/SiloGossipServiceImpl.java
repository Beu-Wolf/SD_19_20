package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.grpc.Gossip;
import pt.tecnico.sauron.silo.grpc.GossipServiceGrpc;

public class SiloGossipServiceImpl extends GossipServiceGrpc.GossipServiceImplBase {

    private Silo silo;

    SiloGossipServiceImpl(Silo silo) { this.silo = silo; }

    @Override
    public void gossip(Gossip.GossipRequest request, StreamObserver<Gossip.GossipResponse> responseObserver) {

    }
}
