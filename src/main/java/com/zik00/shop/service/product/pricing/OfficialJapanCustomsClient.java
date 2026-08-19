package com.zik00.shop.service.product.pricing;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OfficialJapanCustomsClient {
    private static final Pattern RATE_PDF = Pattern.compile(
            "kouji-rate-english(\\d{8})-(\\d{8})\\.pdf", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final String exchangeIndexUrl;
    private final String simplifiedTariffUrl;
    private final String consumptionTaxUrl;
    private final BigDecimal fallbackRate;

    public OfficialJapanCustomsClient(
            @Value("${shop.customs.japan.exchange-index-url:https://www.customs.go.jp/english/kawase/index_e.htm}")
            String exchangeIndexUrl,
            @Value("${shop.customs.japan.simplified-tariff-url:https://www.customs.go.jp/english/c-answer_e/imtsukan/1001_e.htm}")
            String simplifiedTariffUrl,
            @Value("${shop.customs.japan.consumption-tax-url:https://www.customs.go.jp/english/c-answer_e/imtsukan/1111_e.htm}")
            String consumptionTaxUrl,
            @Value("${shop.customs.japan.fallback-krw-to-jpy-rate:0}") BigDecimal fallbackRate
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ZIK00-CustomsUpdater/1.0")
                .build();
        this.exchangeIndexUrl = exchangeIndexUrl;
        this.simplifiedTariffUrl = simplifiedTariffUrl;
        this.consumptionTaxUrl = consumptionTaxUrl;
        this.fallbackRate = fallbackRate == null ? BigDecimal.ZERO : fallbackRate;
    }

    public JapanCustomsSnapshot refresh() {
        RatePeriod rate = fetchExchangeRate();
        Map<String, BigDecimal> tariffs = fetchSimplifiedTariffs();
        return new JapanCustomsSnapshot(
                rate.rate(), rate.from(), rate.to(), tariffs, fetchStandardConsumptionTaxRate(),
                Instant.now(), false
        );
    }

    public JapanCustomsSnapshot fallback() {
        return new JapanCustomsSnapshot(
                fallbackRate, null, null, defaultTariffs(), Instant.now(), true
        );
    }

    private RatePeriod fetchExchangeRate() {
        String indexHtml = requireText(exchangeIndexUrl);
        Document document = Jsoup.parse(indexHtml, exchangeIndexUrl);
        LocalDate today = LocalDate.now();
        PdfPeriod selected = null;
        for (Element link : document.select("a[href]")) {
            String href = link.absUrl("href");
            Matcher matcher = RATE_PDF.matcher(href);
            if (!matcher.find()) continue;
            LocalDate from = LocalDate.parse(matcher.group(1), BASIC_DATE);
            LocalDate to = LocalDate.parse(matcher.group(2), BASIC_DATE);
            PdfPeriod candidate = new PdfPeriod(href, from, to);
            if (!today.isBefore(from) && !today.isAfter(to)) {
                selected = candidate;
                break;
            }
            if (!from.isAfter(today) && (selected == null || from.isAfter(selected.from()))) {
                selected = candidate;
            }
        }
        if (selected == null) throw new IllegalStateException("현재 적용 가능한 일본 세관 환율표를 찾지 못했습니다.");

        byte[] pdf = restClient.get().uri(URI.create(selected.url())).retrieve().body(byte[].class);
        if (pdf == null || pdf.length == 0) throw new IllegalStateException("일본 세관 환율표가 비어 있습니다.");
        BigDecimal rate = parseKoreanWonRate(pdf);
        return new RatePeriod(rate, selected.from(), selected.to());
    }

    private Map<String, BigDecimal> fetchSimplifiedTariffs() {
        Document document = Jsoup.parse(requireText(simplifiedTariffUrl), simplifiedTariffUrl);
        Map<String, BigDecimal> rates = new HashMap<>(defaultTariffs());
        for (Element row : document.select("tr")) {
            String text = row.text().replace('\u00a0', ' ').trim();
            Matcher number = Pattern.compile("^(2|3|4|5|6|7)\\s").matcher(text + " ");
            if (!number.find()) continue;
            String group = number.group(1);
            if (text.toLowerCase().contains("duty free")) {
                rates.put(group, BigDecimal.ZERO);
                continue;
            }
            Matcher percent = Pattern.compile("(20|15|10|5|3)%").matcher(text);
            BigDecimal last = null;
            while (percent.find()) last = new BigDecimal(percent.group(1)).movePointLeft(2);
            if (last != null) rates.put(group, last);
        }
        return Map.copyOf(rates);
    }

    private BigDecimal fetchStandardConsumptionTaxRate() {
        String text = Jsoup.parse(requireText(consumptionTaxUrl), consumptionTaxUrl).text();
        Matcher totalRate = Pattern.compile("(?:Total|total)[^%]{0,40}(10(?:\\.0)?)%")
                .matcher(text);
        if (!totalRate.find()) {
            throw new IllegalStateException("일본 세관 자료에서 표준 소비세율을 찾지 못했습니다.");
        }
        return new BigDecimal(totalRate.group(1)).movePointLeft(2);
    }

    private BigDecimal parseKoreanWonRate(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document).replace('\u00a0', ' ');
            String[] lines = text.split("\\R");
            for (int index = 0; index < lines.length; index++) {
                if (!lines[index].toLowerCase().contains("korea")) continue;
                StringBuilder context = new StringBuilder(lines[index]);
                for (int offset = 1; offset <= 3 && index + offset < lines.length; offset++) {
                    context.append(' ').append(lines[index + offset]);
                }
                BigDecimal rate = rateCandidate(context.toString());
                if (rate != null) return rate;
            }
            throw new IllegalStateException("일본 세관 환율표에서 한국 원화 환율을 찾지 못했습니다.");
        } catch (IOException exception) {
            throw new IllegalStateException("일본 세관 환율표를 읽지 못했습니다.", exception);
        }
    }

    private BigDecimal rateCandidate(String context) {
        Matcher matcher = NUMBER.matcher(context);
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group());
            if (between(value, "0.03", "0.30")) return value;
            if (between(value, "3", "30")) return value.movePointLeft(2);
        }
        return null;
    }

    private boolean between(BigDecimal value, String minimum, String maximum) {
        return value.compareTo(new BigDecimal(minimum)) >= 0
                && value.compareTo(new BigDecimal(maximum)) <= 0;
    }

    private String requireText(String url) {
        String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        if (body == null || body.isBlank()) throw new IllegalStateException("일본 세관 자료가 비어 있습니다.");
        return body;
    }

    private Map<String, BigDecimal> defaultTariffs() {
        return Map.ofEntries(
                Map.entry("2", new BigDecimal("0.20")),
                Map.entry("3", new BigDecimal("0.15")),
                Map.entry("4", new BigDecimal("0.10")),
                Map.entry("5", new BigDecimal("0.03")),
                Map.entry("6", BigDecimal.ZERO),
                Map.entry("7", new BigDecimal("0.05")),
                Map.entry("GENERAL:KNIT", new BigDecimal("0.109")),
                Map.entry("GENERAL:APPAREL", new BigDecimal("0.106")),
                Map.entry("GENERAL:HANDBAG", new BigDecimal("0.12")),
                Map.entry("GENERAL:JEWELRY", new BigDecimal("0.053")),
                Map.entry("GENERAL:FOOTWEAR", new BigDecimal("0.30"))
        );
    }

    private record PdfPeriod(String url, LocalDate from, LocalDate to) {}
    private record RatePeriod(BigDecimal rate, LocalDate from, LocalDate to) {}
}
