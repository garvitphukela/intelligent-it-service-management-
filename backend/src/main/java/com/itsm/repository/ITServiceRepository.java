package com.itsm.repository;

import com.itsm.domain.catalog.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    Optional<ITService> findByNameIgnoreCase(String name);

    List<ITService> findByActiveTrueOrderByNameAsc();
}
