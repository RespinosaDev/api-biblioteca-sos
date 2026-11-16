package com.biblioteca.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoTest {

    private Prestamo prestamo(LocalDate inicio, LocalDate fin) {
        return new Prestamo(new Usuario(), new Libro(), inicio, fin);
    }

    @Test
    void devolverEnPlazoNoSanciona() {
        Prestamo p = prestamo(LocalDate.now().minusDays(3), LocalDate.now().plusDays(5));
        p.devolver();
        assertTrue(p.isDevuelto());
        assertFalse(p.isSancionado());
    }

    @Test
    void devolverFueraDePlazoSanciona() {
        Prestamo p = prestamo(LocalDate.now().minusWeeks(3), LocalDate.now().minusDays(1));
        p.devolver();
        assertTrue(p.isDevuelto());
        assertTrue(p.isSancionado(), "Devolver tarde debe sancionar el préstamo");
    }

    @Test
    void ampliarPrestamoAlargaFechaFin() {
        LocalDate fin = LocalDate.now().plusDays(2);
        Prestamo p = prestamo(LocalDate.now(), fin);
        p.ampliarPrestamo(7);
        assertEquals(fin.plusDays(7), p.getFechaFin());
    }
}
