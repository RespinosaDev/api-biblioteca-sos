package com.biblioteca.service;

import com.biblioteca.exception.ConflictoException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    private static final String PATRON_NOMBRE_USUARIO = "^[a-zA-Z][a-zA-Z0-9_]*$";
    private static final String PATRON_EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario crearUsuario(Usuario usuario) {
        validarUsuario(usuario);

        if (usuarioRepository.existsByNombreUsuario(usuario.getNombreUsuario())) {
            throw new ConflictoException("Nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByMatricula(usuario.getMatricula())) {
            throw new ConflictoException("Matrícula ya registrada");
        }
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ConflictoException("Email ya registrado");
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        validarUsuario(usuarioActualizado);
        Usuario usuario = obtenerUsuarioPorId(id);

        if (!usuario.getNombreUsuario().equals(usuarioActualizado.getNombreUsuario()) &&
            usuarioRepository.existsByNombreUsuario(usuarioActualizado.getNombreUsuario())) {
            throw new ConflictoException("Nombre de usuario ya existe");
        }
        if (!usuario.getMatricula().equals(usuarioActualizado.getMatricula()) &&
            usuarioRepository.existsByMatricula(usuarioActualizado.getMatricula())) {
            throw new ConflictoException("Matrícula ya registrada");
        }
        if (!usuario.getEmail().equals(usuarioActualizado.getEmail()) &&
            usuarioRepository.existsByEmail(usuarioActualizado.getEmail())) {
            throw new ConflictoException("Email ya registrado");
        }

        usuario.setNombreUsuario(usuarioActualizado.getNombreUsuario());
        usuario.setMatricula(usuarioActualizado.getMatricula());
        usuario.setFechaNacimiento(usuarioActualizado.getFechaNacimiento());
        usuario.setEmail(usuarioActualizado.getEmail());

        return usuarioRepository.save(usuario);
    }

    /** Marca a un usuario como sancionado (lo usa la revisión diaria de préstamos). */
    @Transactional
    public void sancionarUsuario(Usuario usuario) {
        usuario.setSancionado(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);

        if (!usuario.getPrestamos().isEmpty()) {
            throw new ConflictoException("No se puede eliminar un usuario con préstamos asociados");
        }

        usuarioRepository.delete(usuario);
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario.getNombreUsuario() == null
                || !usuario.getNombreUsuario().matches(PATRON_NOMBRE_USUARIO)) {
            throw new IllegalArgumentException(
                    "Formato de nombre de usuario inválido (letra inicial, luego letras, dígitos o _)");
        }
        if (usuario.getMatricula() == null || usuario.getMatricula().trim().isEmpty()) {
            throw new IllegalArgumentException("La matrícula es obligatoria");
        }
        if (usuario.getFechaNacimiento() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        if (usuario.getEmail() == null || !usuario.getEmail().matches(PATRON_EMAIL)) {
            throw new IllegalArgumentException("Email inválido");
        }
    }
}
