package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.ValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {
    List<ValidationResult> findByMarketMessageId(Long messageId);
    List<ValidationResult> findByMarketMessageIdAndIsValidFalse(Long messageId);
}
