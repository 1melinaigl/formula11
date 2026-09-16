package com.formula11.service;

import com.formula11.dto.JugadorResponse;
import com.formula11.persistence.JugadorRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JugadorService {
    private final JugadorRepository jugadorRepository;

    public JugadorService(JugadorRepository jugadorRepository) {
        this.jugadorRepository = jugadorRepository;
    }

    public List<JugadorResponse> listar() {
        return jugadorRepository.findAllByOrderByNombreAsc().stream().map(JugadorResponse::from).toList();
    }
}
