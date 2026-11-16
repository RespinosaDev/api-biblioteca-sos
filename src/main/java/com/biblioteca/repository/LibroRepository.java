package com.biblioteca.repository;

import com.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {
    Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
    Page<Libro> findByDisponible(boolean disponible, Pageable pageable);
    Page<Libro> findByTituloContainingIgnoreCaseAndDisponible(String titulo, boolean disponible, Pageable pageable);
    boolean existsByIsbn(String isbn);
}