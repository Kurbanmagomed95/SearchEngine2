package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Optional<Lemma> findFirstByLemma(String lemma);
    List<Lemma> findAllByLemma(String lemma);
    @Query("SELECT e FROM Lemma e WHERE e.lemma = :lemmaValue")
    List<Lemma> findMaxFrequencyByLemma(@Param("lemmaValue") String field2Value);

    @Query("SELECT e FROM Lemma e WHERE e.site.id = :foreignKey")
    List<Lemma> findAllByForeignKey(@Param("foreignKey") Long foreignKey);
}
