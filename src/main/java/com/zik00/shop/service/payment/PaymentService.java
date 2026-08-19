package com.zik00.shop.service.payment;

import com.zik00.shop.domain.Purchase;
import com.zik00.shop.domain.User;
import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.cart.CartItem;
import com.zik00.shop.dto.payment.PaymentItemResponse;
import com.zik00.shop.dto.payment.PaymentPrepareRequest;
import com.zik00.shop.dto.payment.PaymentPrepareResponse;
import com.zik00.shop.dto.payment.PaymentStartResponse;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.dto.product.pricing.LandedPriceEstimateRequest;
import com.zik00.shop.dto.product.pricing.LandedPriceEstimateResponse;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.PurchaseRepository;
import com.zik00.shop.repository.cart.CartRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.product.ProductDetailService;
import com.zik00.shop.service.product.pricing.LandedPriceService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    private static final int MAX_ITEMS = 50;

    private final AuthenticatedUserService authenticatedUserService;
    private final CartRepository cartRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final PurchaseRepository purchaseRepository;
    private final PendingPaymentStore pendingPaymentStore;
    private final SbPaymentGateway paymentGateway;
    private final OperatingExchangeRateService exchangeRateService;
    private final LandedPriceService landedPriceService;
    private final ProductDetailService productDetailService;
    private final long defaultDomesticShippingFeeKrw;
    private final ObjectMapper objectMapper;

    public PaymentService(
            AuthenticatedUserService authenticatedUserService,
            CartRepository cartRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            PurchaseRepository purchaseRepository,
            PendingPaymentStore pendingPaymentStore,
            SbPaymentGateway paymentGateway,
            OperatingExchangeRateService exchangeRateService,
            LandedPriceService landedPriceService,
            ProductDetailService productDetailService,
            @Value("${shop.pricing.default-domestic-shipping-fee-krw:3000}") long defaultDomesticShippingFeeKrw,
            ObjectMapper objectMapper
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.cartRepository = cartRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.purchaseRepository = purchaseRepository;
        this.pendingPaymentStore = pendingPaymentStore;
        this.paymentGateway = paymentGateway;
        this.exchangeRateService = exchangeRateService;
        this.landedPriceService = landedPriceService;
        this.productDetailService = productDetailService;
        this.defaultDomesticShippingFeeKrw = Math.max(0, defaultDomesticShippingFeeKrw);
        this.objectMapper = objectMapper;
    }

    public PaymentPrepareResponse prepare(PaymentPrepareRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        List<Long> requestedIds = new ArrayList<>(new LinkedHashSet<>(request.cartItemIds()));
        if (requestedIds.isEmpty() || requestedIds.size() > MAX_ITEMS) {
            throw bad("한 번에 결제할 상품은 1개 이상 50개 이하여야 합니다.");
        }

        List<CartItem> foundItems = cartRepository.findByIdInAndUserId(requestedIds, user.getMemberId());
        Map<Long, CartItem> itemsById = new LinkedHashMap<>();
        foundItems.forEach(item -> itemsById.put(item.getId(), item));
        List<CartItem> items = requestedIds.stream().map(itemsById::get).toList();
        if (items.stream().anyMatch(item -> item == null)) {
            throw bad("선택한 장바구니 상품을 찾을 수 없습니다.");
        }

        String currency = "JPY";

        DeliveryAddress address = deliveryAddressRepository
                .findUserAddress(request.deliveryAddressId(), user.getMemberId())
                .orElseThrow(() -> bad("배송지를 찾을 수 없습니다."));

        List<PendingPaymentStore.PendingItem> pendingItems = new ArrayList<>();
        long productAmount = 0;
        long domesticShippingFee = 0;
        long agencyFee = 0;
        long shippingMin = 0;
        long shippingMax = 0;
        long estimatedShippingFee = 0;
        long estimatedDuty = 0;
        long estimatedConsumptionTax = 0;
        long estimatedImportCharges = 0;
        boolean customsFinalizationRequired = false;
        LinkedHashSet<String> domesticFeeAppliedProducts = new LinkedHashSet<>();

        for (CartItem item : items) {
            ProductDetailResponse detail = productDetailService.findById(item.getProductId()).orElse(null);
            Map<String, String> selectedOptions = readOptions(item.getOptionData());
            // The cart snapshot is the price the customer confirmed after choosing options.
            // Product detail can change or become temporarily unavailable before checkout.
            long sourceUnitPrice = item.getUnitPrice();
            String sourceCurrency = item.getCurrency();
            long sourceDomesticFee = domesticFeeAppliedProducts.add(item.getProductId())
                    ? detail == null ? defaultDomesticShippingFeeKrw : detail.domesticShippingFee()
                    : 0;
            String domesticFeeCurrency = detail == null ? "KRW" : detail.currency();
            long normalizedDomesticFee = sourceCurrency.equalsIgnoreCase(domesticFeeCurrency)
                    ? sourceDomesticFee
                    : toSourceCurrency(sourceDomesticFee, domesticFeeCurrency, sourceCurrency);
            LandedPriceEstimateResponse estimate = landedPriceService.estimate(
                    new LandedPriceEstimateRequest(
                            item.getProductName(), detail == null || detail.category().isBlank() ? "카테고리 미확인" : detail.category(),
                            sourceUnitPrice, sourceCurrency, item.getQuantity(), normalizedDomesticFee
                    )
            );

            productAmount = Math.addExact(productAmount, estimate.convertedProductPrice());
            domesticShippingFee = Math.addExact(domesticShippingFee, estimate.convertedLocalDistributionFee());
            agencyFee = Math.addExact(agencyFee, estimate.agencyFee());
            shippingMin = Math.addExact(shippingMin, estimate.estimatedInternationalShippingMin());
            shippingMax = Math.addExact(shippingMax, estimate.estimatedInternationalShippingMax());
            estimatedShippingFee = Math.addExact(estimatedShippingFee, estimate.estimatedInternationalShippingFee());
            if (estimate.estimatedDuty() == null || estimate.estimatedConsumptionTax() == null
                    || estimate.estimatedImportCharges() == null) {
                customsFinalizationRequired = true;
            } else {
                estimatedDuty = Math.addExact(estimatedDuty, estimate.estimatedDuty());
                estimatedConsumptionTax = Math.addExact(
                        estimatedConsumptionTax, estimate.estimatedConsumptionTax()
                );
                estimatedImportCharges = Math.addExact(
                        estimatedImportCharges, estimate.estimatedImportCharges()
                );
            }

            pendingItems.add(new PendingPaymentStore.PendingItem(
                    item.getId(), item.getProductId(), item.getProductName(),
                    toJpy(sourceUnitPrice, sourceCurrency), item.getQuantity(),
                    estimate.convertedProductPrice(), selectedOptions
            ));
        }
        long totalAmount = Math.addExact(
                Math.addExact(Math.addExact(productAmount, domesticShippingFee), agencyFee),
                Math.addExact(estimatedShippingFee, estimatedImportCharges)
        );
        if (totalAmount <= 0 || totalAmount > Integer.MAX_VALUE) {
            throw bad("결제 금액을 확인해주세요.");
        }

        String paymentId = "payment" + UUID.randomUUID().toString().replace("-", "");
        String orderName = orderName(items);
        pendingPaymentStore.save(new PendingPaymentStore.PendingPayment(
                paymentId, user.getMemberId(), address.getId(), orderName, totalAmount, currency,
                pendingItems, productAmount, domesticShippingFee, agencyFee, estimatedShippingFee,
                estimatedDuty, estimatedConsumptionTax, estimatedImportCharges, customsFinalizationRequired
        ));

        List<PaymentItemResponse> responses = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            CartItem item = items.get(index);
            PendingPaymentStore.PendingItem pending = pendingItems.get(index);
            responses.add(new PaymentItemResponse(
                    item.getId(), item.getProductId(), item.getProductName(), item.getBrand(), item.getImageUrl(),
                    pending.unitPrice(), item.getQuantity(), pending.subtotal(), pending.selectedOptions()
            ));
        }

        return new PaymentPrepareResponse(
                paymentId, orderName, totalAmount, currency,
                paymentGateway.configured(), "SBPS", paymentGateway.paymentMethods(),
                productAmount, domesticShippingFee, agencyFee, estimatedShippingFee, shippingMin, shippingMax,
                estimatedDuty, estimatedConsumptionTax, estimatedImportCharges, customsFinalizationRequired,
                new PaymentPrepareResponse.Address(
                        address.getId(), address.getAddressName(), address.getReceiverName(),
                        address.getReceiverPhone(), address.getZipCode(), address.getProvince(),
                        address.getDetailAddress()
                ),
                responses
        );
    }

    public PaymentStartResponse start(String paymentId, String paymentMethod) {
        User user = authenticatedUserService.getCurrentUser();
        PendingPaymentStore.PendingPayment pending = pendingPaymentStore.find(paymentId)
                .orElseThrow(() -> bad("결제 준비 정보가 만료되었거나 존재하지 않습니다."));
        if (pending.memberId() != user.getMemberId()) throw bad("본인의 결제만 시작할 수 있습니다.");
        return paymentGateway.createRequest(pending, paymentMethod);
    }

    public String resultUrl(String paymentId, SbPaymentGateway.PaymentState state, String message) {
        return paymentGateway.clientResultUrl(paymentId, state, message);
    }

    @Transactional
    public synchronized SbpsProcessingResult processSbpsResult(Map<String, String> fields) {
        SbPaymentGateway.Result result = paymentGateway.verify(fields);
        SbPaymentGateway.PaymentState state = paymentState(result);
        if (purchaseRepository.existsByOrderNumber(result.paymentId())) {
            SbPaymentGateway.PaymentState existingState = state == SbPaymentGateway.PaymentState.FAILED
                    ? state : SbPaymentGateway.PaymentState.PAID;
            return new SbpsProcessingResult(result.paymentId(), existingState, result.errorCode());
        }
        PendingPaymentStore.PendingPayment pending = pendingPaymentStore.find(result.paymentId())
                .orElseThrow(() -> bad("결제 준비 정보가 만료되었거나 존재하지 않습니다."));
        if (result.amount() != pending.totalAmount() || !"JPY".equalsIgnoreCase(pending.currency())) {
            throw bad("SBPS 결제 금액 또는 통화가 주문 정보와 일치하지 않습니다.");
        }

        if (state != SbPaymentGateway.PaymentState.PAID) {
            return new SbpsProcessingResult(result.paymentId(), state, result.errorCode());
        }
        if (purchaseRepository.existsByOrderNumberAndMemberId(result.paymentId(), pending.memberId())) {
            pendingPaymentStore.delete(result.paymentId());
            return new SbpsProcessingResult(result.paymentId(), state, "");
        }

        List<Purchase> purchases = pending.items().stream().map(item -> new Purchase(
                0L, pending.memberId(), result.paymentId(), item.productName(), item.quantity(),
                Math.toIntExact(item.subtotal()), "결제완료", LocalDate.now()
        )).toList();
        List<Purchase> purchaseLines = new ArrayList<>(purchases);
        if (pending.estimatedShippingFee() > 0) {
            purchaseLines.add(new Purchase(
                    0L, pending.memberId(), result.paymentId(), "국제배송비(예상)", 1,
                    Math.toIntExact(pending.estimatedShippingFee()), "입고 후 정산", LocalDate.now()
            ));
        }
        if (pending.domesticShippingFee() > 0) {
            purchaseLines.add(new Purchase(
                    0L, pending.memberId(), result.paymentId(), "국내 배송비", 1,
                    Math.toIntExact(pending.domesticShippingFee()), "결제완료", LocalDate.now()
            ));
        }
        if (pending.agencyFee() > 0) {
            purchaseLines.add(new Purchase(
                    0L, pending.memberId(), result.paymentId(), "구매대행 수수료", 1,
                    Math.toIntExact(pending.agencyFee()), "결제완료", LocalDate.now()
            ));
        }
        if (pending.estimatedImportCharges() > 0) {
            purchaseLines.add(new Purchase(
                    0L, pending.memberId(), result.paymentId(), "관부가세(예상)", 1,
                    Math.toIntExact(pending.estimatedImportCharges()), "통관 후 정산", LocalDate.now()
            ));
        }
        purchaseRepository.saveAll(purchaseLines);
        List<Long> cartItemIds = pending.items().stream().map(PendingPaymentStore.PendingItem::cartItemId).toList();
        cartRepository.deleteAll(cartRepository.findByIdInAndUserId(cartItemIds, pending.memberId()));
        pendingPaymentStore.delete(result.paymentId());
        return new SbpsProcessingResult(result.paymentId(), state, "");
    }

    private SbPaymentGateway.PaymentState paymentState(SbPaymentGateway.Result result) {
        String status = result.result().toUpperCase();
        if ("NG".equals(status) || "CN".equals(status)) return SbPaymentGateway.PaymentState.FAILED;
        if ("PY".equals(status)) return SbPaymentGateway.PaymentState.PAID;
        if (!"OK".equals(status)) return SbPaymentGateway.PaymentState.PENDING;

        String method = result.paymentMethod().toLowerCase();
        if (List.of("webcvs", "payeasy", "banktransfer").contains(method)) {
            return SbPaymentGateway.PaymentState.PENDING;
        }
        if ("rakutenv2".equals(method) && !"R03".equalsIgnoreCase(result.paymentInfoKey())) {
            return SbPaymentGateway.PaymentState.PENDING;
        }
        if ("nppostpay".equals(method)
                && !List.of("N02", "N03").contains(result.paymentInfoKey().toUpperCase())) {
            return SbPaymentGateway.PaymentState.PENDING;
        }
        return SbPaymentGateway.PaymentState.PAID;
    }

    private String orderName(List<CartItem> items) {
        String value = items.getFirst().getProductName();
        if (items.size() > 1) value += " 외 " + (items.size() - 1) + "건";
        return value.length() > 35 ? value.substring(0, 35) : value;
    }

    private Map<String, String> readOptions(String value) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(value, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            parsed.forEach((key, option) -> result.put(String.valueOf(key), String.valueOf(option)));
            return result;
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private long toJpy(long amount, String currency) {
        try {
            return exchangeRateService.toJpy(amount, currency);
        } catch (IllegalStateException exception) {
            throw bad(exception.getMessage());
        }
    }

    private long toSourceCurrency(long amount, String fromCurrency, String targetCurrency) {
        if (fromCurrency.equalsIgnoreCase(targetCurrency)) return amount;
        if ("KRW".equalsIgnoreCase(fromCurrency) && "JPY".equalsIgnoreCase(targetCurrency)) {
            return toJpy(amount, "KRW");
        }
        if ("JPY".equalsIgnoreCase(fromCurrency) && "KRW".equalsIgnoreCase(targetCurrency)) {
            throw bad("국내 배송비 통화를 상품 통화로 변환할 수 없습니다.");
        }
        throw bad("지원하지 않는 배송비 통화입니다.");
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record SbpsProcessingResult(
            String paymentId,
            SbPaymentGateway.PaymentState state,
            String errorCode
    ) {}
}
