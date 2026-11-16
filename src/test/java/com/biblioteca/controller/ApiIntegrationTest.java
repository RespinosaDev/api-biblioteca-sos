package com.biblioteca.controller;

import com.biblioteca.model.Prestamo;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.service.PrestamoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de extremo a extremo de la API sobre una base de datos H2 en memoria.
 * Los tests no son transaccionales a propósito: así cada petición se comporta
 * igual que en producción (entidades lazy, transacciones por petición...).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private PrestamoService prestamoService;

    // ---------- utilidades ----------

    private long crearUsuario() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String body = """
                {"nombreUsuario":"usuario_%d","matricula":"M%d",
                 "fechaNacimiento":"2002-05-14","email":"u%d@example.com"}""".formatted(n, n, n);
        return idDe(mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn());
    }

    private long crearLibro(String titulo) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String body = """
                {"titulo":"%s","autor":"Miguel de Cervantes","isbn":"ISBN-%d","editorial":"Austral"}"""
                .formatted(titulo, n);
        return idDe(mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long crearPrestamo(long usuarioId, long libroId) throws Exception {
        return idDe(mvc.perform(post("/api/prestamos")
                        .param("usuarioId", String.valueOf(usuarioId))
                        .param("libroId", String.valueOf(libroId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long idDe(MvcResult r) throws Exception {
        JsonNode nodo = json.readTree(r.getResponse().getContentAsString());
        return nodo.get("id").asLong();
    }

    // ---------- libros ----------

    @Test
    void tituloConEspaciosYTildesEsValido() throws Exception {
        long id = crearLibro("Don Quijote de la Mancha, 2ª parte");
        mvc.perform(get("/api/libros/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Don Quijote de la Mancha, 2ª parte")));
    }

    @Test
    void libroSinTituloDevuelve400() throws Exception {
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Anónimo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El título del libro es obligatorio")));
    }

    @Test
    void libroInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/libros/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Libro no encontrado")));
    }

    @Test
    void isbnDuplicadoDevuelve409() throws Exception {
        String body = "{\"titulo\":\"Libro\",\"autor\":\"Autor\",\"isbn\":\"DUP-1\"}";
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void buscarLibrosPorTituloPaginado() throws Exception {
        crearLibro("Rayuela");
        mvc.perform(get("/api/libros").param("titulo", "rayu").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo", is("Rayuela")));
    }

    // ---------- usuarios ----------

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/usuarios/{id}", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void usuarioConEmailInvalidoDevuelve400() throws Exception {
        String body = """
                {"nombreUsuario":"pepe","matricula":"X1","fechaNacimiento":"2000-01-01","email":"no-es-email"}""";
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
    }

    // ---------- préstamos ----------

    @Test
    void flujoCompletoDePrestamo() throws Exception {
        long usuario = crearUsuario();
        long libro = crearLibro("Cien años de soledad");
        long prestamo = crearPrestamo(usuario, libro);

        // GET de un préstamo (incluye usuario y libro cargados de forma lazy)
        mvc.perform(get("/api/prestamos/{id}", prestamo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(usuario))
                .andExpect(jsonPath("$.libro.disponible", is(false)))
                .andExpect(jsonPath("$.fechaFin", is(LocalDate.now().plusWeeks(2).toString())));

        // El mismo libro no se puede volver a prestar
        mvc.perform(post("/api/prestamos").param("usuarioId", "" + usuario).param("libroId", "" + libro))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/prestamos/usuario/{id}/activos", usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        // Ampliar y devolver
        mvc.perform(put("/api/prestamos/{id}/ampliar", prestamo).param("dias", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin", is(LocalDate.now().plusWeeks(3).toString())));

        mvc.perform(put("/api/prestamos/{id}/devolver", prestamo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devuelto", is(true)))
                .andExpect(jsonPath("$.sancionado", is(false)));

        mvc.perform(get("/api/libros/{id}", libro)).andExpect(jsonPath("$.disponible", is(true)));
        mvc.perform(get("/api/prestamos/usuario/{id}/historial", usuario))
                .andExpect(jsonPath("$.totalElements", is(1)));

        // Devolver dos veces es un conflicto
        mvc.perform(put("/api/prestamos/{id}/devolver", prestamo)).andExpect(status().isConflict());
    }

    @Test
    void prestamoSinParametrosDevuelve400() throws Exception {
        mvc.perform(post("/api/prestamos")).andExpect(status().isBadRequest());
    }

    @Test
    void devolucionTardiaSancionaAlUsuarioYBloqueaPrestamos() throws Exception {
        long usuario = crearUsuario();
        long libro = crearLibro("La sombra del viento");
        long prestamoId = crearPrestamo(usuario, libro);

        // Simulamos que el préstamo venció ayer
        Prestamo p = prestamoRepository.findById(prestamoId).orElseThrow();
        p.setFechaFin(LocalDate.now().minusDays(1));
        prestamoRepository.save(p);

        mvc.perform(put("/api/prestamos/{id}/ampliar", prestamoId)).andExpect(status().isConflict());

        mvc.perform(put("/api/prestamos/{id}/devolver", prestamoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sancionado", is(true)));

        mvc.perform(get("/api/usuarios/{id}", usuario)).andExpect(jsonPath("$.sancionado", is(true)));

        long otroLibro = crearLibro("Otro libro");
        mvc.perform(post("/api/prestamos").param("usuarioId", "" + usuario).param("libroId", "" + otroLibro))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Usuario sancionado, no puede realizar préstamos")));
    }

    @Test
    void revisionDiariaSancionaPrestamosVencidos() throws Exception {
        long usuario = crearUsuario();
        long prestamoId = crearPrestamo(usuario, crearLibro("Ficciones"));

        Prestamo p = prestamoRepository.findById(prestamoId).orElseThrow();
        p.setFechaFin(LocalDate.now().minusDays(2));
        prestamoRepository.save(p);

        prestamoService.verificarSanciones();

        mvc.perform(get("/api/prestamos/{id}", prestamoId)).andExpect(jsonPath("$.sancionado", is(true)));
        mvc.perform(get("/api/usuarios/{id}", usuario)).andExpect(jsonPath("$.sancionado", is(true)));
    }

    @Test
    void noSePuedeBorrarUsuarioConPrestamos() throws Exception {
        long usuario = crearUsuario();
        crearPrestamo(usuario, crearLibro("Pedro Páramo"));
        mvc.perform(delete("/api/usuarios/{id}", usuario)).andExpect(status().isConflict());
    }

    @Test
    void borrarUsuarioSinPrestamosDevuelve204() throws Exception {
        long usuario = crearUsuario();
        mvc.perform(delete("/api/usuarios/{id}", usuario)).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/{id}", usuario)).andExpect(status().isNotFound());
    }
}
