package com.biblioteca.controller;

import com.biblioteca.model.Prestamo;
import com.biblioteca.service.PrestamoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {
    private final PrestamoService prestamoService;
    
    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }
    
    @PostMapping
    public ResponseEntity<Prestamo> crearPrestamo(
            @RequestParam Long usuarioId,
            @RequestParam Long libroId) {
        Prestamo nuevoPrestamo = prestamoService.crearPrestamo(usuarioId, libroId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(nuevoPrestamo.getId())
                .toUri();
        return ResponseEntity.created(location).body(nuevoPrestamo);
    }
    
    @PutMapping("/{id}/devolver")
    public ResponseEntity<Prestamo> devolverPrestamo(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolverPrestamo(id));
    }
    
    @PutMapping("/{id}/ampliar")
    public ResponseEntity<Prestamo> ampliarPrestamo(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(prestamoService.ampliarPrestamo(id, dias));
    }
    
    @GetMapping("/usuario/{usuarioId}/activos")
    public ResponseEntity<Page<Prestamo>> obtenerPrestamosActivos(
            @PathVariable Long usuarioId,
            Pageable pageable) {
        return ResponseEntity.ok(prestamoService.obtenerPrestamosActivosPorUsuario(usuarioId, pageable));
    }
    
    @GetMapping("/usuario/{usuarioId}/historial")
    public ResponseEntity<Page<Prestamo>> obtenerHistorialPrestamos(
            @PathVariable Long usuarioId,
            Pageable pageable) {
        return ResponseEntity.ok(prestamoService.obtenerHistorialPrestamosPorUsuario(usuarioId, pageable));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> obtenerPrestamoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.obtenerPrestamoPorId(id));
    }
}