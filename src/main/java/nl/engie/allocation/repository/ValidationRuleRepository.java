package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {
    List<ValidationRule> findByIsActiveTrue();
    List<ValidationRule> findByMessageTypeAndIsActiveTrue(String messageType);
    Optional<ValidationRule> findByRuleCode(String ruleCode);
}
