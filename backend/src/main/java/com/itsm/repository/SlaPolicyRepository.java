package com.itsm.repository;

import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByServiceIdAndPriority(Long serviceId, Priority priority);

    Optional<SlaPolicy> findByServiceIsNullAndPriority(Priority priority);
}
