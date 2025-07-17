package com.coruja.repositories;

import com.coruja.entities.AlertaPassagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaPassagemRepository extends JpaRepository<AlertaPassagem, Long>, JpaSpecificationExecutor<AlertaPassagem> {
}
