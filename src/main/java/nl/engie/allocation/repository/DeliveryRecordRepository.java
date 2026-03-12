package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {
    Optional<DeliveryRecord> findByMarketMessageId(Long messageId);
}
