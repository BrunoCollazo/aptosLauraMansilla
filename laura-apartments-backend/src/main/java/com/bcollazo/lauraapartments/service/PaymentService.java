package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.config.FiservConfig;
import com.bcollazo.lauraapartments.dto.request.FiservRequestDTO;
import com.bcollazo.lauraapartments.dto.request.HomologationPaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.request.PaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.response.FiservPaymentResultDTO;
import com.bcollazo.lauraapartments.dto.response.PaymentResponseDTO;
import com.bcollazo.lauraapartments.integration.FiservClient;
import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.entity.Payment;
import com.bcollazo.lauraapartments.entity.PaymentStatus;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import com.bcollazo.lauraapartments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ApartmentRepository apartmentRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final FiservClient fiservClient;
    private final FiservConfig fiservConfig;
    private final FiservSignatureService signatureService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request) {

        Apartment apartment = apartmentRepository
                .findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        if (!apartment.isAvailable()) {
            throw new RuntimeException("Apartment is not available for rental");
        }

        // Mismo cálculo que muestra el endpoint de cotización: subtotal con descuento + 10% IVA.
        // Así lo que ve el cliente en el front y lo que cobramos acá salen de un solo lugar.
        BigDecimal totalAmount = pricingService.calculateQuote(apartment, request.getNights())
                .getTotal();

        long caratAmount = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
        String reference = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();

        // Flujo normal: siempre en pesos y sin devolución de impuestos.
        FiservRequestDTO fiservRequest = buildFiservRequest(
                caratAmount, CURRENCY_UYU, 0, 0L, 0L, reference,
                "Apartment Rental: " + apartment.getName(),
                request.getClientEmail(),
                today, today.plusDays(request.getNights()));

        String token = fiservClient.initiatePayment(fiservRequest);

        Payment payment = Payment.builder()
                .apartment(apartment)
                .nights(request.getNights())
                .totalAmount(totalAmount)
                .clientEmail(request.getClientEmail())
                .status(PaymentStatus.PENDING)
                .fiservToken(token)
                .reference(reference)
                .build();

        paymentRepository.save(payment);

        return PaymentResponseDTO.builder()
                .pageUrl(fiservConfig.getPageUrl())
                .token(token)
                .reference(reference)
                .build();
    }

    // Dispara un pago con monto, moneda e indi a mano. Solo para la homologación: nos deja
    // mandar los montos exactos de la planilla, probar dólares y las leyes de devolución de
    // impuestos. El desglose del IVA se calcula solo cuando indi > 0.
    @Transactional
    public PaymentResponseDTO createHomologationPayment(HomologationPaymentRequestDTO request) {
        String currency = toCurrencyCode(request.getCurrency());
        int indi = request.getIndi() != null ? request.getIndi() : 0;
        BigDecimal amount = request.getAmount();
        String email = request.getClientEmail() != null
                ? request.getClientEmail()
                : "homologacion@lauramansilla.com";

        long caratAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
        long[] tax = computeTaxBreakdown(caratAmount, indi);
        String reference = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();

        FiservRequestDTO fiservRequest = buildFiservRequest(
                caratAmount, currency, indi, tax[0], tax[1], reference,
                "Homologation transaction", email, today, today.plusDays(1));

        String token = fiservClient.initiatePayment(fiservRequest);

        Payment payment = Payment.builder()
                .totalAmount(amount)
                .clientEmail(email)
                .status(PaymentStatus.PENDING)
                .fiservToken(token)
                .reference(reference)
                .build();

        paymentRepository.save(payment);

        log.info("Homologation payment created: reference={}, amount={} {}, indi={}, token={}",
                reference, amount, currency, indi, token);

        return PaymentResponseDTO.builder()
                .pageUrl(fiservConfig.getPageUrl())
                .token(token)
                .reference(reference)
                .build();
    }

    // Anula un pago en Fiserv y, si sale bien, lo deja como VOIDED.
    @Transactional
    public PaymentStatus voidPayment(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        boolean voided = fiservClient.voidPayment(payment.getFiservToken());
        if (voided) {
            payment.setStatus(PaymentStatus.VOIDED);
            paymentRepository.save(payment);
            log.info("Payment {} voided in Fiserv", reference);
        } else {
            log.warn("Fiserv void failed for reference {}", reference);
        }
        return payment.getStatus();
    }

    private static final String CURRENCY_UYU = "858";
    private static final String CURRENCY_USD = "840";

    // Pasa "UYU"/"USD" (o el código numérico) al código de moneda que espera Fiserv.
    private String toCurrencyCode(String currency) {
        if (currency == null) {
            return CURRENCY_UYU;
        }
        switch (currency.trim().toUpperCase()) {
            case "UYU":
            case "858":
                return CURRENCY_UYU;
            case "USD":
            case "840":
                return CURRENCY_USD;
            default:
                throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }

    // Saca el desglose del IVA (22%) que pide Fiserv cuando hay devolución de impuestos.
    // Asumimos que el monto ya trae el IVA adentro: base = monto / 1.22, iva = monto - base.
    // Devuelve {taxedAmount, taxAmount} en centavos (o {0,0} si no hay devolución).
    // OJO: el 22% vale para los indi de IVA (1 Ley 17.934 y 6 Ley 19.210). Para IMESI (indi 2)
    // o Ley 18.999 (indi 4) el impuesto no es 22% — si hay que homologarlos, ajustar acá.
    private long[] computeTaxBreakdown(long amountCents, int indi) {
        if (indi == 0) {
            return new long[]{0L, 0L};
        }
        long taxedAmount = BigDecimal.valueOf(amountCents)
                .divide(BigDecimal.valueOf(1.22), 0, RoundingMode.HALF_UP)
                .longValue();
        long taxAmount = amountCents - taxedAmount;
        return new long[]{taxedAmount, taxAmount};
    }

    // Arma el request de pago para Fiserv. Lo comparten el flujo normal y el de homologación;
    // cambia el monto, la moneda, el indi y los datos de la factura, el resto es igual.
    private FiservRequestDTO buildFiservRequest(long caratAmount, String currency, int indi,
                                                long taxedAmount, long taxAmount, String reference,
                                                String description, String clientEmail,
                                                LocalDate date, LocalDate dueDate) {
        String invoiceNumber = String.valueOf(System.currentTimeMillis() % 1000000000L);
        return FiservRequestDTO.builder()
                .currency(currency)
                .amount(caratAmount)
                .taxedAmount(taxedAmount)
                .taxAmount(taxAmount)
                .indi(indi)
                .reference(reference)
                .invoice(FiservRequestDTO.Invoice.builder()
                        .number(invoiceNumber)
                        .totalAmount(caratAmount)
                        .currency(currency)
                        .date(date.toString())
                        .dueDate(dueDate.toString())
                        .description(description)
                        .finalConsumer(true)
                        .serial("A")
                        .address(FiservRequestDTO.Address.builder()
                                .country("UY")
                                .city("Castillos") // Castillos, Rocha
                                .street("Alfredo Vigliola casi Pintos Diago")
                                // Fiserv revienta la pagina hosted con "S/N" (la barra / no le gusta y no
                                // es numerico) -> IF99. Va "0" de placeholder hasta tener el numero real.
                                .doorNumber("0")
                                .build())
                        .build())
                .client(FiservRequestDTO.Client.builder()
                        .clientIdType("EMAIL")
                        .clientId(clientEmail)
                        .email(clientEmail)
                        .build())
                .config(FiservRequestDTO.Config.builder()
                        .callbackUrl(fiservConfig.getCallbackUrlCode())
                        .useRedirect(true)
                        .build())
                .build();
    }

    @Transactional
    public PaymentStatus processCallbackResult(String dataJson) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Verify the signature against the raw JSON map, not a DTO round-trip: our DTOs
        // don't model every field Fiserv may send, so verifying off the typed object would
        // silently drop fields and break the signature check.
        Map<String, Object> resultMap = objectMapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        FiservPaymentResultDTO result = objectMapper.convertValue(resultMap, FiservPaymentResultDTO.class);

        String reference = result.getReference();
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        if (result.getResponseHeader() == null || result.getResponseHeader().getDigitalSign() == null) {
            log.error("Missing responseHeader or digitalSign in callback for reference: {}", reference);
            throw new RuntimeException("Missing callback signature");
        }

        if (!signatureService.verifySign(resultMap, result.getResponseHeader().getDigitalSign())) {
            log.error("Invalid signature in callback for reference: {}", reference);
            throw new RuntimeException("Invalid callback signature");
        }

        updatePaymentFromDetails(payment, result.getPayment());

        paymentRepository.save(payment);
        return payment.getStatus();
    }

    @Transactional
    public PaymentStatus syncPaymentStatus(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getStatus();
        }

        fiservClient.queryPayment(payment.getFiservToken()).ifPresent(details -> {
            updatePaymentFromDetails(payment, details);
            paymentRepository.save(payment);
            log.info("Synced payment status for reference {}: {}", reference, payment.getStatus());
        });

        return payment.getStatus();
    }

    private static final java.time.format.DateTimeFormatter FISERV_DATE_TIME_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Copies the Fiserv-reported payment details (card, authorization, confirmed amount/currency,
     * confirmation timestamp) onto our record. Shared by the callback, sync and webhook paths so
     * none of them silently drop data Fiserv actually sent.
     */
    public void applyPaymentDetails(Payment payment, FiservPaymentResultDTO.PaymentDetails details) {
        if (details == null) {
            return;
        }
        if (details.getPaymentToken() != null && payment.getPaymentToken() == null) {
            payment.setPaymentToken(details.getPaymentToken());
        }
        if (details.getCard() != null) {
            Object cardBrandName = details.getCard().get("cardBrandName");
            Object cardMask = details.getCard().get("cardMask");
            if (cardBrandName != null) payment.setCardBrand(cardBrandName.toString());
            if (cardMask != null) payment.setCardMask(cardMask.toString());
        }
        if (details.getAuthorizer() != null) {
            Object authorizationCode = details.getAuthorizer().get("authorizationCode");
            if (authorizationCode != null) payment.setAuthorizationCode(authorizationCode.toString());
        }
        if (details.getAmount() > 0) {
            payment.setConfirmedAmount(details.getAmount());
        }
        if (details.getCurrency() != null) {
            payment.setCurrency(details.getCurrency());
        }
        if (details.getDateTime() != null) {
            try {
                payment.setConfirmedAt(LocalDateTime.parse(details.getDateTime(), FISERV_DATE_TIME_FORMATTER));
            } catch (Exception e) {
                log.warn("Could not parse Fiserv payment dateTime '{}': {}", details.getDateTime(), e.getMessage());
            }
        }
    }

    private void updatePaymentFromDetails(Payment payment, FiservPaymentResultDTO.PaymentDetails details) {
        if (details != null) {
            applyPaymentDetails(payment, details);

            if ("PROCESSED".equals(details.getState())) {
                // OJO: cuando la financiera rechaza (ej. fondos insuficientes) Fiserv igual
                // manda state=PROCESSED pero con authorized=false. Sin esto el pago quedaba
                // colgado en PENDING para siempre. authorized=true -> PROCESSED, false -> FAILED.
                payment.setStatus(details.isAuthorized() ? PaymentStatus.PROCESSED : PaymentStatus.FAILED);
            } else if ("CANCELED".equals(details.getState())) {
                payment.setStatus(PaymentStatus.FAILED);
            } else if ("INPROCESS".equals(details.getState())) {
                // Expected transient state (per Fiserv docs): payment not yet confirmed by the
                // client, or still being processed by a collection network. Stays PENDING.
                log.debug("Payment still INPROCESS for reference {}", payment.getReference());
            } else {
                log.warn("Unhandled payment state '{}' for reference {}", details.getState(), payment.getReference());
            }
        }
    }
}