package com.crm.ai.parser.repository;

import com.crm.ai.parser.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentReport, Long> {
}
