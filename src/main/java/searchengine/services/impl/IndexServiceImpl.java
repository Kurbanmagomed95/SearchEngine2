package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.LemmaFinder;
import searchengine.config.Site;
import searchengine.dto.statistics.ESiteStatus;
import searchengine.dto.statistics.MultipleDateSearchResponse;
import searchengine.dto.statistics.RelevanceDto;
import searchengine.dto.statistics.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


@Service
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class IndexServiceImpl implements IndexService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final JdbcTemplate template;
    private volatile boolean methodRunning = true;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();


    @Scheduled(fixedRate = 6000)
    public void stopMethod() {
        if (stopIndexing()) {
            methodRunning = false;
        }
    }
    
    @Override
    public boolean stopIndexing(){
        return true;
    }

    @Override
    public void startIndexing() {
       methodRunning = true;
            List<Site> sites = new ArrayList<>();
            Collections.addAll(sites,
                    new Site("https://www.lenta.ru", "Лента.ру"),
                    new Site("https://www.skillbox.ru", "Skillbox"),
                    new Site("https://www.playback.ru", "PlayBack.Ru")
            );
            for (Site site : sites) {
                searchengine.model.Site entity = siteRepository.save(
                        new searchengine.model.Site(
                                null,
                                ESiteStatus.INDEXING,
                                LocalDateTime.now(),
                                null,
                                site.getUrl(),
                                site.getName()
                        )
                );
                Threadinfo threadinfo = new Threadinfo(entity);
                forkJoinPool.invoke(threadinfo);
                searchengine.model.Site site1 = siteRepository.findById(entity.getId()).get();
                if (site1 != null) {
                    threadinfo.fork();
                }
            }
    }

    private int getStatusCode(String url) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        connection.disconnect();

        return statusCode;
    }


    class Threadinfo extends RecursiveTask<Void> {
        private searchengine.model.Site site;


        public Threadinfo(searchengine.model.Site site) {
            this.site = site;
        }

        @Override
        protected Void compute() {
            try {
                if (!pageRepository.existsBySiteAndPath(site, site.getUrl())) {
                    Document document = Jsoup.connect(site.getUrl())
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com")
                            .get();
                    Elements links = document.select("a[href]");
                    for (Element link : links) {
                        String url = link.absUrl("href");
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            String content = Jsoup.connect(url).get().text();
                            int statusCode = getStatusCode(url);

                            Page page = new Page();
                            page.setSite(site);
                            page.setPath(url);
                            page.setCode(statusCode);
                            page.setContent(content);

                            pageRepository.save(page);
                            site.setDateTime(LocalDateTime.now());
                            siteRepository.save(site);
                        }
                    }
                    site.setStatus(ESiteStatus.INDEXED);
                    siteRepository.save(site);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Transactional
    public Boolean indexPage(String url) throws IOException {
        Page page = pageRepository.getByPath(url);
        searchengine.model.Site site = page.getSite();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Map<String, Integer> map = lemmaFinder.collectLemmas(page.getContent());
        for (Map.Entry<String, Integer> i : map.entrySet()) {
            Optional<String> optionalLemmaField = lemmaRepository.findFirstByLemma(i.getKey()).map(Lemma::getLemma);
            String lemmaField = optionalLemmaField.orElse(null);
            if (lemmaField == null) {
                Lemma lemma = lemmaRepository.save
                        (
                                new Lemma
                                        (
                                                null,
                                                site,
                                                i.getKey(),
                                                1
                                        )
                        );
                indexRepository.save
                        (
                                new Index
                                        (
                                                null,
                                                page,
                                                lemma,
                                                (double) countWordOccurrencesInHtml(page.getContent(), i.getKey())
                                        )
                        );
            } else {
                int max = 0;
                List<Lemma> lemma = lemmaRepository.findAll();
                for (Lemma j : lemma) {
                    if (j.getLemma().equals(i.getKey())) {
                        if (j.getFrequency() > max) {
                            max = j.getFrequency();
                        }
                    }
                }
                ++max;
                Lemma lemma1 = lemmaRepository.save
                        (
                                new Lemma
                                        (
                                                null,
                                                site,
                                                i.getKey(),
                                                max
                                        )
                        );
                indexRepository.save
                        (
                                new Index
                                        (
                                                null,
                                                page,
                                                lemma1,
                                                (double) countWordOccurrencesInHtml(page.getContent(), i.getKey())
                                        )
                        );
            }
        }
        return true;
    }


    @Transactional
    public SearchResponse search(String query) throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        List<String> map = lemmaFinder.collectLemmasList(query.toLowerCase());
        List<Lemma> lemma = new ArrayList<>();
        for (Lemma i : lemmaRepository.findAll()) {
            for (String j : map) {
                if (i.getLemma().equals(j)) {
                    lemma.add(i);
                }
            }
        }
        List<Index> indices = indexRepository.findAllByLemmaIn(lemma.stream().map(Lemma::getId).distinct().toList());
        List<MultipleDateSearchResponse> multipleDateSearchResponses = new ArrayList<>();
        for (Index i : indices) {
            Page page = i.getPage();
            Lemma lemma1 = i.getLemma();
            log.warn("lemma {}, {}", lemma1.getLemma(), lemma1.getFrequency());
            searchengine.model.Site site = page.getSite();
            multipleDateSearchResponses.add(new MultipleDateSearchResponse
                    (
                            site.getUrl(),
                            site.getName(),
                            findByUri(site.getUrl(), page.getPath()),
                            extractTitleFromHtml(page.getContent()),
                            findAndHighlightWordInHtml(lemma1.getLemma(), page.getContent()),
                            findEntitiesByPageIdAndLemmaId(page.getId(), lemma.stream().map(Lemma::getId).toList())
                    )
            );


        }
        return new SearchResponse
                (
                        true,
                        multipleDateSearchResponses.size(),
                        multipleDateSearchResponses
                );
    }


    public Double findEntitiesByPageIdAndLemmaId(Long pageId, List<Long> lemmaIds) {
        String sql = "WITH PageRelevance AS ( " +
                "    SELECT " +
                "        i.page_id, " +
                "        i.lemma_id, " +
                "        SUM(i.rank) AS absolute_relevance " +
                "    FROM " +
                "        index i " +
                "    WHERE " +
                "        i.page_id = ? AND i.lemma_id IN (";

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < lemmaIds.size(); i++) {
            placeholders.append("?");
            if (i < lemmaIds.size() - 1) {
                placeholders.append(",");
            }
        }

        sql += placeholders.toString() + " ) " +
                "    GROUP BY " +
                "        i.page_id, i.lemma_id " +
                "), " +
                "MaxAbsoluteRelevance AS ( " +
                "    SELECT " +
                "        MAX(pr.absolute_relevance) AS max_absolute_relevance " +
                "    FROM " +
                "        PageRelevance pr " +
                ") " +
                "SELECT " +
                "    pr.page_id, " +
                "    pr.lemma_id, " +
                "    pr.absolute_relevance, " +
                "    pr.absolute_relevance / mar.max_absolute_relevance AS relative_relevance " +
                "FROM " +
                "    PageRelevance pr " +
                "CROSS JOIN " +
                "    MaxAbsoluteRelevance mar";

        Object[] params = new Object[lemmaIds.size() + 1];
        params[0] = pageId;
        for (int i = 0; i < lemmaIds.size(); i++) {
            params[i + 1] = lemmaIds.get(i);
        }

        int[] types = new int[lemmaIds.size() + 1];
        types[0] = Types.BIGINT;
        for (int i = 0; i < lemmaIds.size(); i++) {
            types[i + 1] = Types.BIGINT;
        }
        List<Double> list = new ArrayList<>();

        template.query(sql, params, types,
                (resultSet, rowNum) -> {
                    Index index = new Index();
                    list.add(resultSet.getDouble("relative_relevance"));
                    return index;
                });
        return list.get(0);
    }


    public static String findByUri(String url, String oldChar) {
        int index = url.lastIndexOf(".");
        return oldChar.substring(index);
    }

    public static String extractTitleFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element titleElement = doc.select("title").first();
        if (titleElement != null) {
            return titleElement.text();
        } else {
            return "Заголовок не найден";
        }
    }

    public static String findAndHighlightWordInHtml(String word, String html) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.getElementsContainingText(word);
        for (Element element : elements) {
            String elementHtml = element.html();
            String highlightedHtml = elementHtml.replaceAll("(?i)" + word, "<b>$0</b>");
            element.html(highlightedHtml);
        }
        return doc.outerHtml();
    }

    public static int countWordOccurrencesInHtml(String htmlContent, String word) {
        String textOnly = htmlContent.replaceAll("\\<([a-zA-Z]+)[^>]*>.*?\\</\\1\\>", " ");
        String[] words = textOnly.split("\\s+");
        int count = 0;
        for (String w : words) {
            if (w.equalsIgnoreCase(word)) {
                count++;
            }
        }
        return count;
    }
}


