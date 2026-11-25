package com.formula11.persistence.mercado;

import com.formula11.model.mercado.AuditoriaMercado;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Collection;


public interface AuditoriaMercadoRepository extends Repository<AuditoriaMercado, Long> {
    AuditoriaMercado save(AuditoriaMercado auditoria);
    Optional<AuditoriaMercado> findById(Long id);
    List<AuditoriaMercado> findByOperacionIdIn(Collection<Long> operacionIds);
    List<AuditoriaMercado> findAll();
    long count();
}
