package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.config.FiservConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FiservSignatureServiceTest {

    private final FiservSignatureService service = new FiservSignatureService(new FiservConfig());

    @Test
    void buildSignatureText_matchesFiservDocExample() {
        Map<String, Object> bComplejo = new LinkedHashMap<>();
        bComplejo.put("b1", "ValorB1");
        bComplejo.put("b3", "ValorB3");
        bComplejo.put("b2", "Valor B2 ConCaractér.E$p");

        Map<String, Object> x1 = new LinkedHashMap<>();
        x1.put("c", "ValorC1");
        x1.put("a", "ValorA1");

        Map<String, Object> x2 = new LinkedHashMap<>();
        x2.put("c", "ValorC2");
        x2.put("a", "ValorA2");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("az", "ValorAZ");
        root.put("ab", "ValorAB");
        root.put("c", 999);
        root.put("bComplejo", bComplejo);
        root.put("x", List.of(x1, x2));
        root.put("z", "ValorZ");

        // Expected text per "EJEMPLO PARA GENERAR FIRMA" (Carat Gateway doc v3.02, pag. 59),
        // corrected for an apparent typo in the doc's second array element ("ValorC2" repeated
        // instead of "ValorA2").
        String expected = "ABVALORABAZVALORAZBCOMPLEJOB1VALORB1B2VALORB2CONCARACTR.E$PB3VALORB3" +
                "C999XAVALORA1CVALORC1AVALORA2CVALORC2ZVALORZ";

        String actual = service.buildSignatureText(root).toUpperCase();

        assertEquals(expected, actual);
    }
}
