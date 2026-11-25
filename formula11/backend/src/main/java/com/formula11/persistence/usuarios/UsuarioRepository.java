package com.formula11.persistence.usuarios;

import com.formula11.model.usuarios.Usuario;
import com.formula11.model.usuarios.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Usuario> findByRol(Rol rol);
}
