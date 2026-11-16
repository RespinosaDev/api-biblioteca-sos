package com.biblioteca.model;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@Entity
@Table(name = "prestamos")
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libro_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Libro libro;
    
    @Column(nullable = false)
    private LocalDate fechaInicio;
    
    @Column(nullable = false)
    private LocalDate fechaFin;
    
    @Column(nullable = false)
    private boolean devuelto = false;
    
    @Column(nullable = false)
    private boolean sancionado = false;
    
    // Constructores
    public Prestamo() {}
    
    public Prestamo(Usuario usuario, Libro libro, LocalDate fechaInicio, LocalDate fechaFin) {
        this.usuario = usuario;
        this.libro = libro;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public Usuario getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    public Libro getLibro() {
        return libro;
    }
    
    public void setLibro(Libro libro) {
        this.libro = libro;
    }
    
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDate getFechaFin() {
        return fechaFin;
    }
    
    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
    
    public boolean isDevuelto() {
        return devuelto;
    }
    
    public void setDevuelto(boolean devuelto) {
        this.devuelto = devuelto;
    }
    
    public boolean isSancionado() {
        return sancionado;
    }
    
    public void setSancionado(boolean sancionado) {
        this.sancionado = sancionado;
    }
    
    // Métodos de negocio
    public boolean estaVencido() {
        return LocalDate.now().isAfter(fechaFin) && !devuelto;
    }
    
    public void devolver() {
        // Se comprueba el vencimiento ANTES de marcarlo como devuelto,
        // porque estaVencido() solo es cierto para préstamos sin devolver
        if (estaVencido()) {
            this.sancionado = true;
        }
        this.devuelto = true;
    }
    
    public void ampliarPrestamo(int dias) {
        if (!devuelto && !estaVencido()) {
            this.fechaFin = this.fechaFin.plusDays(dias);
        }
    }
}