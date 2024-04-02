package searchengine.dto.statistics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
