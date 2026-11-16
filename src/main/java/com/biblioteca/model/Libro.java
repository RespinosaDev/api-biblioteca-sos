package com.biblioteca.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "libros")
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String titulo;
    
    @Column(nullable = false)
    private String autor;
    
    private String edicion;
    
    @Column(unique = true)
    private String isbn;
    
    private String editorial;
    
    @Column(nullable = false)
    private boolean disponible = true;
    
    @OneToMany(mappedBy = "libro", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore 
    private List<Prestamo> prestamos = new ArrayList<>();
    
    // Constructores
    public Libro() {}
    
    public Libro(String titulo, String autor, String edicion, String isbn, String editorial) {
        this.titulo = titulo;
        this.autor = autor;
        this.edicion = edicion;
        this.isbn = isbn;
        this.editorial = editorial;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public String getTitulo() {
        return titulo;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    
    public String getAutor() {
        return autor;
    }
    
    public void setAutor(String autor) {
        this.autor = autor;
    }
    
    public String getEdicion() {
        return edicion;
    }
    
    public void setEdicion(String edicion) {
        this.edicion = edicion;
    }
    
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public String getEditorial() {
        return editorial;
    }
    
    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }
    
    public boolean isDisponible() {
        return disponible;
    }
    
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
    
    public List<Prestamo> getPrestamos() {
        return prestamos;
    }
    
    public void setPrestamos(List<Prestamo> prestamos) {
        this.prestamos = prestamos;
    }
    
    // Métodos helper
    public void addPrestamo(Prestamo prestamo) {
        prestamos.add(prestamo);
        prestamo.setLibro(this);
    }
    
    public void removePrestamo(Prestamo prestamo) {
        prestamos.remove(prestamo);
        prestamo.setLibro(null);
    }
}