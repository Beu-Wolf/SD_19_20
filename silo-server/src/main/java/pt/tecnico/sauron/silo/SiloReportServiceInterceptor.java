package pt.tecnico.sauron.silo;

import io.grpc.*;

public class SiloReportServiceInterceptor implements ServerInterceptor {
    public static final Context.Key<String> CAM_NAME = Context.key("name");
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String name = metadata.get(METADATA_CAM_NAME);
        Context context = Context.current().withValue(CAM_NAME, name);
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
    }

}
