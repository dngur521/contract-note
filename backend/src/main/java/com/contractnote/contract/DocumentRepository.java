package com.contractnote.contract;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<ContractDocument, Long> {}
