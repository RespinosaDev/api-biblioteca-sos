package com.biblioteca.service;

import com.biblioteca.exception.ConflictoException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.model.Libro;
import com.biblioteca.repository.LibroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibroService {
    // Letras (con tildes), dígitos, espacios y signos de puntuación habituales
    private static final String PATRON_TITULO = "^[\\p{L}\\d\\s.,;:!?¡¿'\"()-]+$";
    private static final String PATRON_AUTOR = "^[\\p{L}\\s.'-]+$";

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional
    public Libro crearLibro(Libro libro) {
        validarLibro(libro);

        if (libro.getIsbn() != null && libroRepository.existsByIsbn(libro.getIsbn())) {
            throw new ConflictoException("ISBN ya existe");
        }
        return libroRepository.save(libro);
    }

    @Transactional(readOnly = true)
    public Page<Libro> buscarLibros(String titulo, Boolean disponible, Pageable pageable) {
        if (titulo != null && disponible != null) {
            return libroRepository.findByTituloContainingIgnoreCaseAndDisponible(titulo, disponible, pageable);
        } else if (titulo != null) {
            return libroRepository.findByTituloContainingIgnoreCase(titulo, pageable);
        } else if (disponible != null) {
            return libroRepository.findByDisponible(disponible, pageable);
        }
        return libroRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Libro obtenerLibroPorId(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));
    }

    @Transactional
    public Libro actualizarLibro(Long id, Libro libroActualizado) {
        Libro libro = obtenerLibroPorId(id);
        validarLibro(libroActualizado);

        if (libroActualizado.getIsbn() != null &&
            !libroActualizado.getIsbn().equals(libro.getIsbn()) &&
            libroRepository.existsByIsbn(libroActualizado.getIsbn())) {
            throw new ConflictoException("ISBN ya existe");
        }

        libro.setTitulo(libroActualizado.getTitulo());
        libro.setAutor(libroActualizado.getAutor());
        libro.setEdicion(libroActualizado.getEdicion());
        libro.setIsbn(libroActualizado.getIsbn());
        libro.setEditorial(libroActualizado.getEditorial());

        return libroRepository.save(libro);
    }

    @Transactional
    public void eliminarLibro(Long id) {
        Libro libro = obtenerLibroPorId(id);

        if (!libro.getPrestamos().isEmpty()) {
            throw new ConflictoException("No se puede eliminar un libro con préstamos asociados");
        }

        libroRepository.delete(libro);
    }

    @Transactional
    public void marcarComoDisponible(Long libroId, boolean disponible) {
        Libro libro = obtenerLibroPorId(libroId);
        libro.setDisponible(disponible);
        libroRepository.save(libro);
    }

    private void validarLibro(Libro libro) {
        if (libro.getTitulo() == null || libro.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del libro es obligatorio");
        }
        if (libro.getAutor() == null || libro.getAutor().trim().isEmpty()) {
            throw new IllegalArgumentException("El autor del libro es obligatorio");
        }
        if (!libro.getTitulo().matches(PATRON_TITULO)) {
            throw new IllegalArgumentException("El título contiene caracteres no permitidos");
        }
        if (!libro.getAutor().matches(PATRON_AUTOR)) {
            throw new IllegalArgumentException("El nombre del autor solo puede contener letras y espacios");
        }
    }
}
