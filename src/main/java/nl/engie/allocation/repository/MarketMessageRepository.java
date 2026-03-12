package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketMessageRepository extends JpaRepository<MarketMessage, Long> {
    Optional<MarketMessage> findByMessageUuid(String messageUuid);
    List<MarketMessage> findByStatus(MessageStatus status);
    List<MarketMessage> findByStatusOrderByPriorityAscReceivedAtAsc(MessageStatus status);
    boolean existsByMessageUuid(String messageUuid);
}
