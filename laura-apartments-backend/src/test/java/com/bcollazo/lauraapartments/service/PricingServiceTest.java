package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.IvaStatusDTO;
import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// IVA por temporada: 10% en temporada (15/nov -> Domingo de Pascua), exonerado fuera.
// Se aplica por noche; una estadía a caballo del límite se prorratea.
class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    private Apartment apt() {
        return Apartment.builder()
                .name("Test")
                .pricePerNight(new BigDecimal("1000"))
                .available(true)
                .build();
    }

    @Test
    void computesEasterSunday() {
        assertEquals(LocalDate.of(2024, 3, 31), PricingService.easterSunday(2024));
        assertEquals(LocalDate.of(2025, 4, 20), PricingService.easterSunday(2025));
        assertEquals(LocalDate.of(2026, 4, 5), PricingService.easterSunday(2026));
    }

    @Test
    void highSeasonBoundaries() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2025, 11, 15)));  // arranca temporada
        assertTrue(PricingService.isHighSeason(LocalDate.of(2026, 1, 10)));   // pleno verano
        assertTrue(PricingService.isHighSeason(LocalDate.of(2026, 4, 5)));    // Domingo de Pascua (incl.)
        assertFalse(PricingService.isHighSeason(LocalDate.of(2026, 4, 6)));   // lunes post Pascua
        assertFalse(PricingService.isHighSeason(LocalDate.of(2026, 7, 1)));   // invierno
        assertFalse(PricingService.isHighSeason(LocalDate.of(2025, 11, 14))); // víspera de temporada
    }

    @Test
    void fullyInSeasonChargesTenPercent() {
        QuoteResponseDTO q = pricing.calculateQuote(apt(), LocalDate.of(2026, 1, 10), 2);
        assertEquals(2, q.getInSeasonNights());
        assertEquals(0, new BigDecimal("2000.00").compareTo(q.getSubtotal()));
        assertEquals(0, new BigDecimal("200.00").compareTo(q.getIvaAmount()));
        assertEquals(0, new BigDecimal("2200.00").compareTo(q.getTotal()));
    }

    @Test
    void fullyOffSeasonIsExempt() {
        QuoteResponseDTO q = pricing.calculateQuote(apt(), LocalDate.of(2026, 7, 1), 2);
        assertEquals(0, q.getInSeasonNights());
        assertEquals(0, new BigDecimal("0.00").compareTo(q.getIvaAmount()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(q.getTotal()));
    }

    @Test
    void ivaStatusInSeason() {
        IvaStatusDTO s = pricing.getIvaStatus(LocalDate.of(2026, 1, 10));
        assertTrue(s.isApplyingIva());
        assertEquals(LocalDate.of(2025, 11, 15), s.getSeasonStart());
        assertEquals(LocalDate.of(2026, 4, 5), s.getSeasonEnd()); // Pascua 2026
    }

    @Test
    void ivaStatusOffSeason() {
        IvaStatusDTO s = pricing.getIvaStatus(LocalDate.of(2026, 7, 1));
        assertFalse(s.isApplyingIva());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.getIvaRate()));
        assertEquals(LocalDate.of(2026, 11, 15), s.getSeasonStart()); // próxima temporada
        assertEquals(LocalDate.of(2027, 3, 28), s.getSeasonEnd());    // Pascua 2027
    }

    @Test
    void crossingBoundaryProratesIva() {
        // check-in 04/04/2026, 4 noches: 04 y 05 en temporada (Pascua 05/04), 06 y 07 fuera.
        QuoteResponseDTO q = pricing.calculateQuote(apt(), LocalDate.of(2026, 4, 4), 4);
        assertEquals(2, q.getInSeasonNights());
        // subtotal 4000; gravado = 4000 * 2/4 = 2000; IVA = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(q.getIvaAmount()));
        assertEquals(0, new BigDecimal("4200.00").compareTo(q.getTotal()));
    }
}
