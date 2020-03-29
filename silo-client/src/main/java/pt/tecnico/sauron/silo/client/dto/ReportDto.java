package pt.tecnico.sauron.silo.client.dto;

import java.time.LocalDateTime;

public class ReportDto {
    private CamDto cam;
    private ObservationDto observation;
    private LocalDateTime timestamp;

    public ReportDto(ObservationDto observation, CamDto cam, LocalDateTime timestamp) {
        this.observation = observation;
        this.cam = cam;
        this.timestamp = timestamp;
    }
}
