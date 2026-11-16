package com.biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nombreUsuario;
    
    @Column(nullable = false, unique = true)
    private String matricula;
    
    @Column(nullable = false)
    private LocalDate fechaNacimiento;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private boolean sancionado = false;

    public boolean isSancionado() {
        return sancionado;
    }

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Prestamo> prestamos = new ArrayList<>();
    
    // Constructores
    public Usuario() {}
    
    public Usuario(String nombreUsuario, String matricula, LocalDate fechaNacimiento, String email) {
        this.nombreUsuario = nombreUsuario;
        this.matricula = matricula;
        this.fechaNacimiento = fechaNacimiento;
        this.email = email;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public String getMatricula() {
        return matricula;
    }
    
    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }
    
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public List<Prestamo> getPrestamos() {
        return prestamos;
    }
    
    public void setPrestamos(List<Prestamo> prestamos) {
        this.prestamos = prestamos;
    }
    

    public void setSancionado(boolean sancionado) {
        this.sancionado = sancionado;
    }
    
    
    // Métodos helper
    public void addPrestamo(Prestamo prestamo) {
        prestamos.add(prestamo);
        prestamo.setUsuario(this);
    }
    
    public void removePrestamo(Prestamo prestamo) {
        prestamos.remove(prestamo);
        prestamo.setUsuario(null);
    }
}