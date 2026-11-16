package com.biblioteca.config;

import com.biblioteca.service.PrestamoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SancionScheduler {
    private final PrestamoService prestamoService;
    
    public SancionScheduler(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }
    
    @Scheduled(cron = "0 0 0 * * ?") // Ejecuta todos los días a medianoche
    public void verificarPrestamosVencidos() {
        prestamoService.verificarSanciones();
    }
}
