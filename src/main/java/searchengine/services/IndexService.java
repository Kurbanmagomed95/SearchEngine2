package searchengine.services;

import searchengine.dto.statistics.SearchResponse;
import searchengine.model.Lemma;

import java.io.IOException;

public interface IndexService {
    void startIndexing();
    boolean stopIndexing();
    Boolean indexPage(String url) throws IOException;
    SearchResponse search(String query) throws IOException;
}
