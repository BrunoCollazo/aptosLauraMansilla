package com.bcollazo.lauraapartments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Estado del IVA por temporada para mostrarle a Laura en el admin: si hoy se cobra IVA y las
// fechas de la temporada vigente (o la próxima si estamos fuera de temporada).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IvaStatusDTO {
    private LocalDate today;
    private boolean applyingIva;   // ¿hoy se está cobrando IVA?
    private BigDecimal ivaRate;    // 0.10 en temporada, 0 fuera
    private LocalDate seasonStart; // inicio (15/nov) de la temporada vigente o próxima
    private LocalDate seasonEnd;   // fin (Domingo de Pascua) de esa temporada
}
