package com.logistics.ai.etl.repository;

import com.logistics.ai.etl.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentReport, Long> {
}
