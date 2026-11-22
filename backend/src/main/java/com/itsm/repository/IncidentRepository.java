package com.itsm.repository;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Repository pattern: persistence is hidden behind an interface; Spring Data generates the implementation. */
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    List<Incident> findByCreatedAtAfterAndStatusNot(Instant after, IncidentStatus excluded);

    List<Incident> findByAssignedTechnicianIdAndStatusIn(Long technicianId, Collection<IncidentStatus> statuses);

    List<Incident> findByMasterIncidentId(Long masterId);

    long countByAssignedTechnicianIdAndStatusIn(Long technicianId, Collection<IncidentStatus> statuses);

    @Query("select i.assignedTechnician.id, count(i) from Incident i " +
           "where i.status in :statuses and i.assignedTechnician is not null group by i.assignedTechnician.id")
    List<Object[]> countByTechnicianForStatuses(@Param("statuses") Collection<IncidentStatus> statuses);

    @Query("select i.assignedTechnician.id, count(i) from Incident i where i.service.id = :serviceId " +
           "and i.resolution.summary is not null and i.assignedTechnician is not null group by i.assignedTechnician.id")
    List<Object[]> resolvedCountByTechnicianForService(@Param("serviceId") Long serviceId);

    @Query("select i from Incident i where i.resolution.summary is not null and i.status in :statuses")
    List<Incident> findResolvedWithResolution(@Param("statuses") Collection<IncidentStatus> statuses);

    @Query("select i from Incident i where i.resolution.summary is not null and i.category.id = :categoryId")
    List<Incident> findResolvedInCategory(@Param("categoryId") Long categoryId);

    @Query("select i from Incident i where i.category is not null and i.status <> com.itsm.domain.incident.IncidentStatus.CANCELLED")
    List<Incident> findAllCategorised();
}
