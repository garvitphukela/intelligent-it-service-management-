package com.itsm.repository;

import com.itsm.domain.user.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCode(String code);

    @Query("select t from Technician t where t.active = true order by t.id")
    List<Technician> findActiveTechnicians();

    @Query("select t from Technician t order by t.id")
    List<Technician> findAllTechnicians();

    @Query("select l from TeamLead l where l.active = true order by l.id")
    List<TeamLead> findActiveTeamLeads();

    /** Exact type match so IT managers (a subclass) are not included. */
    @Query("select u from User u where type(u) = Administrator and u.active = true")
    List<User> findActiveAdministrators();

    @Query("select m from ITManager m where m.active = true")
    List<ITManager> findActiveManagers();

    @Query("select count(u) from User u where type(u) = :type")
    long countByType(Class<? extends User> type);
}
