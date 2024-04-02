package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    boolean existsBySiteAndPath(Site site, String path);
    Page getByPath(String path);
    @Query("SELECT e FROM Page e WHERE e.site.id = :foreignKey")
    List<Page> findAllByForeignKey(@Param("foreignKey") Long foreignKey);
}
