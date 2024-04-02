package searchengine.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StopIndexingResponse {
    private boolean result;
    private String error;

    public StopIndexingResponse(boolean result) {
        this.result = result;
    }
}
