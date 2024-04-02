package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Page;

import java.util.List;


@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    @Query("SELECT e.page FROM Index e WHERE e.lemma = :foreignKeyValue")
    List<Long> findAllByForeignKey(@Param("foreignKeyValue") Long foreignKeyValue);

    @Query("SELECT e FROM Index e WHERE e.lemma.id IN :stringValues")
    List<Index> findAllByLemmaIn(@Param("stringValues") List<Long> foreignKeys);

}
