package com.biblioteca.repository;

import com.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    Page<Prestamo> findByUsuarioId(Long usuarioId, Pageable pageable);
    Page<Prestamo> findByUsuarioIdAndDevuelto(Long usuarioId, boolean devuelto, Pageable pageable);
    Page<Prestamo> findByFechaInicioBetween(LocalDate inicio, LocalDate fin, Pageable pageable);
    List<Prestamo> findByFechaFinBeforeAndDevueltoFalse(LocalDate fecha);
    boolean existsByLibroIdAndDevueltoFalse(Long libroId);
}