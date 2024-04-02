package searchengine.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelevanceDto {
    private Long page;
    private String name;
    private double absolute;
    private double relevance;

    public RelevanceDto(Long page, String name) {
        this.page = page;
        this.name = name;
    }
}
