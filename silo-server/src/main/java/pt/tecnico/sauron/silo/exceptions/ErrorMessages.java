package pt.tecnico.sauron.silo.exceptions;

public class ErrorMessages {
    public static String BLANK_INPUT = "Input cannot be blank!";
    public static String INVALID_PERSON_ID = "Person ID must be an unsigned long!";
    public static String INVALID_CAR_ID = "Car ID must be a valid portuguese license plate!";
    public static String UNIMPLEMENTED_OBSERVATION_TYPE = "Can't handle observation type!";
    public static String OBSERVATION_NOT_FOUND = "Observation not found!";
    public static String NO_CAM_FOUND = "Camera not found!";
    public static String DUPLICATE_CAMERA_NAME_EXCEPTION = "There already exists a camera with that name!";
    public static String INVALID_CAMERA_NAME_SIZE = "Camera names must be between 3 and 15 characters long!";
    public static String TYPE_NOT_SUPPORTED = "Type to observe not supported!";
    public static String EMPTY_CAMERA_NAME = "Camera name is empty!";
    public static String INVALID_COORDS = "Invalid Camera coordinates!";
}
