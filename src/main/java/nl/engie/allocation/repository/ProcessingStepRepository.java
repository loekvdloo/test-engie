package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.ProcessingStep;
import nl.engie.allocation.model.enums.StepCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingStepRepository extends JpaRepository<ProcessingStep, Long> {
    List<ProcessingStep> findByMarketMessageIdOrderByStepOrderAsc(Long messageId);
    Optional<ProcessingStep> findByMarketMessageIdAndStepCode(Long messageId, StepCode stepCode);
}
