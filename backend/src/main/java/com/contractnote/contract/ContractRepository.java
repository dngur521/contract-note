package com.contractnote.contract;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findAllByOrderByCreatedAtDesc();
    List<Contract> findByStatus(ContractStatus status);
}
