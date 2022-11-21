package app.manager.transaction.asset_record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvestmentAssetRecordRepository extends JpaRepository<InvestmentAssetRecord, Long> {
    @Query("SELECT r FROM InvestmentAssetRecord r WHERE r.type = 'CASH' AND r.assetId = :cashId")
    Optional<InvestmentAssetRecord> findCashRecordById(@Param("cashId") Long cashId);

    @Query("SELECT r FROM InvestmentAssetRecord r WHERE r.type = 'SECURITY' AND r.assetId = :securityId")
    Optional<InvestmentAssetRecord> findSecurityRecordById(@Param("securityId") Long securityId);
}
