package pt.tecnico.sauron.silo.client.exceptions;

public class ErrorMessages {
    public static final String BLANK_PING_INPUT = "Input cannot be blank!";
    public static final String CAMERA_ALREADY_EXISTS = "Camera name already exists in a different location";
    public static final String CAMERA_NOT_FOUND = "Camera not found";
    public static final String FAILED_TO_REGISTER_CAMERA = "Could not register camera";
    public static final String FAILED_TO_RETRIEVE_CAMERA_INFO = "Could not retrieve camera info";
    public static final String TYPE_NOT_SUPPORTED = "Type to observe not supported!";
    public static final String WAITING_THREAD_INTERRUPT = "Waiting for the thread to interrupt!";
    public static final String GENERIC_QUERY_ERROR = "Error executing query!";
    public static final String OBSERVATION_NOT_FOUND = "Observation not found!";
    public static final String NO_CONNECTION = "Server is down, attempting new connection...";
    public static final String NO_ONLINE_SERVERS = "No servers online!";
    public static final String INVALID_VECTOR_TIMESTAMP = "Frontend and Replica timestamps' size inconsistent";
}
