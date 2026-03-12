package nl.engie.allocation.repository;

import nl.engie.allocation.model.entity.MarketResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketResponseRepository extends JpaRepository<MarketResponse, Long> {
    Optional<MarketResponse> findByMarketMessageId(Long messageId);
    Optional<MarketResponse> findByResponseUuid(String responseUuid);
}
