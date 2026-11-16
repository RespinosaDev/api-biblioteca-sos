package com.biblioteca.service;

import com.biblioteca.exception.ConflictoException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.PrestamoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService {
    private static final int SEMANAS_PRESTAMO = 2;

    private final PrestamoRepository prestamoRepository;
    private final UsuarioService usuarioService;
    private final LibroService libroService;

    public PrestamoService(PrestamoRepository prestamoRepository,
                         UsuarioService usuarioService,
                         LibroService libroService) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioService = usuarioService;
        this.libroService = libroService;
    }

    @Transactional
    public Prestamo crearPrestamo(Long usuarioId, Long libroId) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(usuarioId);
        Libro libro = libroService.obtenerLibroPorId(libroId);

        if (usuario.isSancionado()) {
            throw new ConflictoException("Usuario sancionado, no puede realizar préstamos");
        }

        if (!libro.isDisponible() || prestamoRepository.existsByLibroIdAndDevueltoFalse(libroId)) {
            throw new ConflictoException("Libro no disponible para préstamo");
        }

        Prestamo prestamo = new Prestamo(usuario, libro,
                LocalDate.now(), LocalDate.now().plusWeeks(SEMANAS_PRESTAMO));
        libro.setDisponible(false);

        return prestamoRepository.save(prestamo);
    }

    @Transactional(readOnly = true)
    public Prestamo obtenerPrestamoPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));
    }

    @Transactional
    public Prestamo devolverPrestamo(Long prestamoId) {
        Prestamo prestamo = obtenerPrestamoPorId(prestamoId);

        if (prestamo.isDevuelto()) {
            throw new ConflictoException("El libro ya fue devuelto");
        }

        prestamo.devolver(); // si se devuelve fuera de plazo, el préstamo queda sancionado
        prestamo.getLibro().setDisponible(true);

        if (prestamo.isSancionado()) {
            usuarioService.sancionarUsuario(prestamo.getUsuario());
        }
        return prestamoRepository.save(prestamo);
    }

    @Transactional
    public Prestamo ampliarPrestamo(Long prestamoId, int dias) {
        if (dias <= 0) {
            throw new IllegalArgumentException("Los días de ampliación deben ser positivos");
        }
        Prestamo prestamo = obtenerPrestamoPorId(prestamoId);

        if (prestamo.isDevuelto()) {
            throw new ConflictoException("No se puede ampliar un préstamo ya devuelto");
        }
        if (prestamo.estaVencido()) {
            throw new ConflictoException("No se puede ampliar un préstamo vencido");
        }

        prestamo.ampliarPrestamo(dias);
        return prestamoRepository.save(prestamo);
    }

    @Transactional(readOnly = true)
    public Page<Prestamo> obtenerPrestamosActivosPorUsuario(Long usuarioId, Pageable pageable) {
        usuarioService.obtenerUsuarioPorId(usuarioId); // 404 si el usuario no existe
        return prestamoRepository.findByUsuarioIdAndDevuelto(usuarioId, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prestamo> obtenerHistorialPrestamosPorUsuario(Long usuarioId, Pageable pageable) {
        usuarioService.obtenerUsuarioPorId(usuarioId);
        return prestamoRepository.findByUsuarioIdAndDevuelto(usuarioId, true, pageable);
    }

    /** Revisión diaria: sanciona los préstamos vencidos sin devolver y a sus usuarios. */
    @Transactional
    public void verificarSanciones() {
        List<Prestamo> prestamosVencidos =
                prestamoRepository.findByFechaFinBeforeAndDevueltoFalse(LocalDate.now());

        for (Prestamo prestamo : prestamosVencidos) {
            prestamo.setSancionado(true);
            prestamoRepository.save(prestamo);
            usuarioService.sancionarUsuario(prestamo.getUsuario());
        }
    }
}
