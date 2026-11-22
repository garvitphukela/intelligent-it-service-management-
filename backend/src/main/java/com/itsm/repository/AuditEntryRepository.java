package com.itsm.repository;

import com.itsm.domain.audit.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long>, JpaSpecificationExecutor<AuditEntry> {

    List<AuditEntry> findByIncidentIdOrderByTimestampAscIdAsc(Long incidentId);
}
