package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.*;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;


    @Override
    @Transactional
    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        boolean indexing = sites.stream().allMatch(s -> s.getStatus().equals(ESiteStatus.INDEXING));
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites((int) siteRepository.count());
        totalStatistics.setPages((int) siteRepository.count());
        totalStatistics.setLemmas((int) lemmaRepository.count());
        totalStatistics.setIndexing(indexing);
        List<Site> sites1 = siteRepository.findAll();
        List<DetailedStatisticsItem> detailedStatisticsItems = sites1.stream().map(s -> new DetailedStatisticsItem
                (
                        s.getUrl(),
                        s.getName(),
                        s.getStatus().name(),
                        mapperLocalDateTime(s.getDateTime()),
                        s.getLastError(),
                        getFilterPages(s.getUrl()),
                        getFilterLemma(s.getUrl())
                )
        ).toList();

        StatisticsData statisticsData = new StatisticsData
                (
                        totalStatistics,
                        detailedStatisticsItems
                );
        return new StatisticsResponse
                (
                        true,
                        statisticsData
                );
    }

    @Transactional
    public int getFilterPages(String url) {
        Site site = siteRepository.getByUrl(url);
        List<Page> pages = pageRepository.findAllByForeignKey(site.getId());
        return pages.size();
    }

    @Transactional
    public int getFilterLemma(String url) {
        Site site = siteRepository.getByUrl(url);
        List<Lemma> lemma = lemmaRepository.findAllByForeignKey(site.getId());
        return lemma.size();
    }

    @Transactional
    public long mapperLocalDateTime(LocalDateTime localDateTime) {
        long epochSeconds = localDateTime.toEpochSecond(ZoneOffset.UTC);
        long nanosOfDay = localDateTime.toLocalTime().toNanoOfDay();
        long result = epochSeconds * 1000000000 + nanosOfDay;
        return result;
    }
}
