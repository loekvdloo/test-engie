package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.BrpRegisterEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrpRegisterRepository extends JpaRepository<BrpRegisterEntry, Long> {
    Optional<BrpRegisterEntry> findByEanCode(String eanCode);
    boolean existsByEanCodeAndIsActiveTrue(String eanCode);
}
