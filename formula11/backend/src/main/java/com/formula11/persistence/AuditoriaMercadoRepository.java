package com.formula11.persistence;

import com.formula11.model.AuditoriaMercado;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;

// Append-only: solo expone inserción y lectura; no hay update ni delete.
public interface AuditoriaMercadoRepository extends Repository<AuditoriaMercado, Long> {
    AuditoriaMercado save(AuditoriaMercado auditoria);
    Optional<AuditoriaMercado> findById(Long id);
    List<AuditoriaMercado> findAll();
    long count();
}
