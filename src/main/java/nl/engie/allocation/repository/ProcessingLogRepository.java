package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {
    List<ProcessingLog> findByMarketMessageIdOrderByLoggedAtAsc(Long messageId);
}
