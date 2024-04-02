package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public static LemmaFinder getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new LemmaFinder(morphology);
    }

    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    private LemmaFinder(){
        throw new RuntimeException("Disallow construct");
    }

    public static void main(String[] args) throws IOException {
        Map<String, Double> pageRanks = new HashMap<>();
        pageRanks.put("1", 7.3);
        pageRanks.put("2", 2.5);
        pageRanks.put("3", 10.3);

        calculateRelevance(pageRanks);




    }

    public static void calculateRelevance(Map<String, Double> pageRanks) {
        double maxRelevance = 0;

        // Находим сумму rank для каждой страницы
        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            maxRelevance += entry.getValue();
        }

        // Рассчитываем относительную релевантность для каждой страницы
        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            double absoluteRelevance = entry.getValue();
            double relativeRelevance = absoluteRelevance / maxRelevance;

            System.out.println("Страница " + entry.getKey());
            System.out.println("Абсолютная релевантность: " + absoluteRelevance);
            System.out.println("Относительная релевантность: " + relativeRelevance);
            System.out.println();
        }
    }


    public static String findByUri(String url, String oldChar){
        int index = url.lastIndexOf(".");
        return oldChar.substring(index);
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }

    public List<String> collectLemmasList(String text) {
        String[] words = arrayContainsRussianWords(text);
       List<String> list = new ArrayList<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            list.add(normalWord);
        }

        return list;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }
}
