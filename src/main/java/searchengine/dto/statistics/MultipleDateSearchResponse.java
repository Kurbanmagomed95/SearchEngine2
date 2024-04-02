package searchengine.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MultipleDateSearchResponse {
    private String site;
    private String name;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;


}
