package searchengine.model.dto;

import searchengine.dto.statistics.ESiteStatus;

import java.time.LocalDateTime;

public class DetailedDto {
    private String url;
    private String name;
    private ESiteStatus status;
    private LocalDateTime statusTime;
    private Integer pages;
    private Integer lemmas;
}
