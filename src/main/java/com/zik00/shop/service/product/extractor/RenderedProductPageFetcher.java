package com.zik00.shop.service.product.extractor;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitUntilState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RenderedProductPageFetcher {
    private static final Logger log = LoggerFactory.getLogger(RenderedProductPageFetcher.class);

    private final boolean enabled;

    public RenderedProductPageFetcher(
            @Value("${shop.product-discovery.browser-rendering-enabled:true}") boolean enabled
    ) {
        this.enabled = enabled;
    }

    public synchronized Optional<Document> fetch(String sourceUrl) {
        if (!enabled) return Optional.empty();

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setChannel("chrome")
                    .setArgs(List.of("--disable-blink-features=AutomationControlled"));
            try (Browser browser = playwright.chromium().launch(options)) {
                Page page = browser.newPage(new Browser.NewPageOptions()
                        .setLocale("ko-KR")
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"));
                page.navigate(sourceUrl, new Page.NavigateOptions()
                        .setTimeout(30_000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                waitForProductContent(page);
                annotateOptions(page);
                return Optional.of(Jsoup.parse(page.content(), sourceUrl));
            }
        } catch (PlaywrightException exception) {
            log.info("브라우저 상품 페이지 렌더링에 실패해 일반 HTML 요청을 사용합니다: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private void waitForProductContent(Page page) {
        long deadline = System.currentTimeMillis() + 12_000;
        while (System.currentTimeMillis() < deadline) {
            Object ready = page.evaluate("""
                    () => {
                      const value = selector => (document.querySelector(selector)?.value || '').trim();
                      const meta = property => (document.querySelector(`meta[property="${property}"]`)?.content || '').trim();
                      const hasSiteProductIdentity = value('#itemId') && value('#itemNm');
                      const hasStructuredProduct = [...document.querySelectorAll('script[type="application/ld+json"]')]
                        .some(script => /["']@type["']\\s*:\\s*["']Product["']/i.test(script.textContent || ''));
                      const hasOpenGraphProduct = meta('og:title') && meta('og:image') && (
                        document.querySelector('[itemprop="price"], meta[property="product:price:amount"]')
                        || value('#sellprc')
                        || /[\\d,]+\\s*(?:원|KRW|USD|JPY|EUR|₩|\\$|¥|€)/i.test(document.body?.innerText || '')
                      );
                      return Boolean(hasSiteProductIdentity || hasStructuredProduct || hasOpenGraphProduct);
                    }
                    """);
            if (Boolean.TRUE.equals(ready)) {
                page.waitForTimeout(1_000);
                return;
            }
            page.waitForTimeout(500);
        }
    }

    private void annotateOptions(Page page) {
        page.evaluate("""
                async () => {
                  const clean = value => (value || '').replace(/\\s+/g, ' ').trim();
                  const priceOf = value => {
                    const matches = [...clean(value).matchAll(/([\\d,]+)\\s*원/g)];
                    return matches.length ? Number(matches.at(-1)[1].replaceAll(',', '')) : null;
                  };
                  const optionTypeText = value => /사이즈|색상|컬러|옵션|핏|용량|구성|모델|타입|스타일|향|맛|길이|폭|소재|size|colou?r|option|fit|capacity|volume|style|model|type|material|storage/i.test(clean(value));
                  const labelOf = control => clean(
                    control.getAttribute('aria-label')
                    || control.labels?.[0]?.innerText
                    || control.closest('label')?.innerText
                    || control.nextElementSibling?.innerText
                    || control.value
                  );
                  const typeOf = (control, fallback) => {
                    const title = clean(control.getAttribute('title') || control.getAttribute('data-option-name'));
                    if (title && title.toLowerCase() !== 'undefined' && !/^선택|select$/i.test(title)) {
                      return title.replace(/[:*]/g, '').trim();
                    }
                    const labelledBy = control.getAttribute('aria-labelledby');
                    if (labelledBy) {
                      const value = clean(document.getElementById(labelledBy)?.innerText);
                      if (value) return value;
                    }
                    const id = control.id;
                    if (id) {
                      const value = clean(document.querySelector(`label[for="${CSS.escape(id)}"]`)?.innerText);
                      if (value) return value.replace(/[:*]/g, '').trim();
                    }
                    const legend = clean(control.closest('fieldset')?.querySelector('legend')?.innerText);
                    if (legend) return legend;
                    let node = control.parentElement;
                    for (let depth = 0; node && depth < 6; depth += 1, node = node.parentElement) {
                      const heading = [...node.querySelectorAll(':scope > h1, :scope > h2, :scope > h3, :scope > h4, :scope > legend, :scope > label')]
                        .map(item => clean(item.innerText))
                        .find(optionTypeText);
                      if (heading) return heading.replace(/선택(하기|하세요)?|[:*]/g, '').trim();
                    }
                    return fallback;
                  };
                  const groups = new Map();
                  const visible = element => {
                    if (!element) return false;
                    const style = getComputedStyle(element);
                    return style.display !== 'none' && style.visibility !== 'hidden'
                      && element.getClientRects().length > 0;
                  };
                  const productValue = value => /\\bW\\s*\\d{2,3}\\b|\\b\\d{2,3}\\s*mm\\b|\\b(US|UK|EU|KR)\\s*\\d|\\b(XXS|XS|S|M|L|XL|XXL)\\b|색상|컬러/i.test(clean(value));
                  const sizeValue = value => clean(value) !== '0' && /^(?:W\\s*)?\\d{2,3}(?:\\.5)?(?:\\s*mm)?$|^(?:(?:US|UK|EU|KR)\\s*)?\\d{1,3}(?:\\.5)?(?:\\s*mm)?(?:\\s*\\/\\s*(?:(?:US|UK|EU|KR)\\s*)?\\d{1,3}(?:\\.5)?(?:\\s*mm)?)?(?:\\s*\\((?:US|UK|EU|KR)?\\s*\\d{1,3}(?:\\.5)?\\))?$|^(?:XXS|XS|S|M|L|XL|XXL|XXXL)$/i.test(clean(value));
                  const shippingValue = value => /shipping\\s*to|ship\\s*to|배송\\s*(?:국가|지역|주소)|국가\\s*선택|select\\s*(?:a\\s*)?country/i.test(clean(value));
                  const placeholder = value => /선택.*(?:주세요|하세요|해 주세요)|^옵션을? 선택|^(?:please\\s*)?(?:select|choose)/i.test(clean(value));
                  const normalizeValue = (type, value) => {
                    value = clean(value);
                    if (type === '사이즈' && value.includes('/')) value = value.substring(value.lastIndexOf('/') + 1);
                    return value;
                  };
                  const add = (type, value, available, price, id) => {
                    type = clean(type).slice(0, 40);
                    value = normalizeValue(type, value)
                      .replace(/\\s*\\/\\s*[\\d,]+\\s*원.*$/, '').slice(0, 80);
                    if (!type || !value || placeholder(value)) return;
                    if (!groups.has(type)) groups.set(type, new Map());
                    const existing = groups.get(type).get(value);
                    groups.get(type).set(value, {
                      variantId: id || `${type}-${value}`,
                      attributes: { [type]: value },
                      price: price || existing?.price || null,
                      available: existing ? existing.available || available : available
                    });
                  };

                  const dependentVariants = [];
                  const wait = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
                  const dependentDepths = [...new Set([...document.querySelectorAll('select[data-opt-depth]')]
                    .filter(select => !select.id.startsWith('_bar_'))
                    .map(select => Number(select.getAttribute('data-opt-depth')))
                    .filter(Number.isFinite))].sort((left, right) => left - right);
                  const selectAtDepth = depth => [...document.querySelectorAll(`select[data-opt-depth="${depth}"]`)]
                    .find(select => !select.id.startsWith('_bar_'));
                  const usableOptions = select => [...(select?.options || [])]
                    .filter(option => clean(option.textContent) && !placeholder(option.textContent));
                  const exploreDependentOptions = async (position, attributes) => {
                    if (dependentVariants.length >= 100 || position >= dependentDepths.length) return;
                    const select = selectAtDepth(dependentDepths[position]);
                    if (!select) return;
                    const type = typeOf(select, `상품 옵션 ${position + 1}`);
                    for (const option of usableOptions(select).slice(0, 30)) {
                      const value = clean(option.textContent);
                      const optionPrice = Number(clean(option.getAttribute('data-option-price')).replaceAll(',', '')) || priceOf(value);
                      const remainQuantity = option.getAttribute('data-remain-qty');
                      const available = !option.disabled && (remainQuantity === null || Number(remainQuantity) > 0);
                      const nextAttributes = { ...attributes, [type]: value };
                      add(type, value, available, optionPrice, option.getAttribute('data-uitem-id') || option.value);
                      if (position + 1 < dependentDepths.length) {
                        select.value = option.value;
                        const inputEvent = document.createEvent('Event');
                        inputEvent.initEvent('input', true, true);
                        select.dispatchEvent(inputEvent);
                        const changeEvent = document.createEvent('Event');
                        changeEvent.initEvent('change', true, true);
                        select.dispatchEvent(changeEvent);
                        await wait(600);
                        if (usableOptions(selectAtDepth(dependentDepths[position + 1])).length > 0) {
                          await exploreDependentOptions(position + 1, nextAttributes);
                        }
                      } else {
                        dependentVariants.push({
                          variantId: option.getAttribute('data-uitem-id') || Object.values(nextAttributes).join('-'),
                          attributes: nextAttributes,
                          price: optionPrice || null,
                          available
                        });
                      }
                      if (dependentVariants.length >= 100) break;
                    }
                  };
                  if (dependentDepths.length > 1) await exploreDependentOptions(0, {});

                  [...document.querySelectorAll('select')]
                    .map((select, index) => ({ select, index, texts: [...select.options].map(option => clean(option.textContent)) }))
                    .filter(item => {
                      const meaningful = item.texts.filter(value => value && !placeholder(value));
                      const productCount = meaningful.filter(productValue).length;
                      const shippingCount = meaningful.filter(shippingValue).length;
                      if (shippingCount > 0 && shippingCount >= Math.ceil(meaningful.length / 3)) return false;
                      if (meaningful.length > 40 && productCount === 0) return false;
                      const type = typeOf(item.select, '');
                      const trustedProductSelect = item.select.hasAttribute('data-opt-depth')
                        && !item.select.id.startsWith('_bar_');
                      return productCount > 0 || trustedProductSelect
                        || (visible(item.select) && optionTypeText(type));
                    })
                    .sort((left, right) => Number(right.texts.some(productValue)) - Number(left.texts.some(productValue)))
                    .forEach(({ select, index: selectIndex, texts }) => {
                    const meaningful = texts.filter(value => value && !placeholder(value));
                    if (meaningful.length === 0) return;
                    const inferredType = meaningful.some(value => /\\bW\\s*\\d{2,3}\\b|\\b\\d{2,3}\\s*mm\\b|\\b(US|UK|EU|KR)\\s*\\d/i.test(value))
                      ? '사이즈'
                      : typeOf(select, `상품 옵션 ${selectIndex + 1}`);
                    [...select.options].forEach(option => {
                      const text = clean(option.textContent);
                      const optionPrice = Number(clean(option.getAttribute('data-option-price')).replaceAll(',', '')) || priceOf(text);
                      const remainQuantity = option.getAttribute('data-remain-qty');
                      const available = !option.disabled && (remainQuantity === null || Number(remainQuantity) > 0);
                      add(inferredType, text, available, optionPrice, option.getAttribute('data-uitem-id') || option.value || option.id);
                    });
                  });

                  const choiceControls = [...document.querySelectorAll('input[type="radio"], [role="radio"]')]
                    .filter(control => visible(control) || visible(control.closest('label')));
                  choiceControls.forEach((control, index) => {
                    const text = labelOf(control);
                    const controlName = clean(control.getAttribute('name'));
                    const reviewControl = control.closest('[class*="review" i], [id*="review" i], [class*="rvw_" i]');
                    if (reviewControl || /^(전체|포토|동영상)\\(\\d+\\)$|추천순|최신순|평점(?:높은|낮은)순/.test(text)) return;
                    if (/thumbnail|gallery|image/i.test(controlName) || /^[A-Z]{1,5}\\d{3,}-\\d+-\\d+$/.test(text)) return;
                    if (/^(베이비|리틀키즈|주니어|성인)$/.test(text)) return;
                    const type = /^(\\d{2,3}(?:\\.5)?|[2-9]X?[SL]|X{0,3}[SLM])$/i.test(text)
                      ? '사이즈'
                      : typeOf(control, controlName || `옵션 ${index + 1}`);
                    const disabled = control.disabled || control.getAttribute('aria-disabled') === 'true';
                    add(type, text, !disabled, priceOf(text), control.value || control.id);
                  });

                  let stockCatalog = {};
                  try {
                    const rawStock = window.option_stock_data;
                    stockCatalog = typeof rawStock === 'string' ? JSON.parse(rawStock) : (rawStock || {});
                  } catch (ignored) {
                    stockCatalog = {};
                  }
                  [...document.querySelectorAll('[option_title] > li[option_value], [option_title] li[option_value]')]
                    .filter(control => control.closest('[product_type]')?.getAttribute('product_type') !== 'addproduct_option')
                    .forEach((control, index) => {
                      const container = control.closest('[option_title]');
                      const type = clean(container?.getAttribute('option_title')) || '상품 옵션';
                      const value = clean(control.getAttribute('option_value') || control.getAttribute('title') || control.innerText);
                      const stock = Object.values(stockCatalog).find(item => clean(item?.option_value) === value);
                      const className = typeof control.className === 'string' ? control.className : '';
                      const disabled = /disabled|soldout|sold-out|품절/i.test(className)
                        || stock?.is_selling === 'F' || Number(stock?.stock_number) === 0;
                      add(type, value, !disabled, Number(stock?.option_price) || priceOf(control.innerText), stock?.option_id || `shop-option-${index}`);
                    });

                  const preferredSizeControls = [...document.querySelectorAll('.sizeselector .sizeitem__label')].filter(visible);
                  const customOptionControls = (preferredSizeControls.length > 0 ? preferredSizeControls : [...document.querySelectorAll(
                    '[role="option"], button, [class*="size" i], [id*="size" i], [data-testid*="size" i]'
                  )])
                    .filter(visible)
                    .map((control, index) => ({ control, index, text: clean(control.innerText || control.textContent || control.getAttribute('aria-label')) }))
                    .filter(item => item.text && item.text.length <= 80 && sizeValue(item.text))
                    .slice(0, 100);
                  customOptionControls.forEach(({ control, index, text }) => {
                    const className = typeof control.className === 'string' ? control.className : '';
                    const disabled = control.disabled || control.getAttribute('aria-disabled') === 'true'
                      || /disabled|notavailable|unavailable|soldout|sold-out|품절/i.test(className);
                    add('사이즈', text, !disabled, priceOf(text), control.id || control.getAttribute('data-value') || `size-${index}`);
                  });

                  const options = [...groups].map(([optionType, values]) => ({
                    optionType,
                    values: [...values.keys()]
                  })).filter(group => group.values.length > 0);
                  const variants = dependentVariants.length > 0
                    ? dependentVariants
                    : [...groups.values()].flatMap(values => [...values.values()]);
                  const renderedPrice = Number(clean(
                    document.querySelector('input#sellprc, input[name="sellprc"], input#bestAmt, input[name="sellUnitPrc"], [itemprop="price"]')?.value
                    || document.querySelector('meta[itemprop="price"], meta[property="product:price:amount"]')?.content
                  ).replaceAll(',', '')) || null;
                  document.getElementById('zik00-rendered-options')?.remove();
                  const marker = document.createElement('script');
                  marker.id = 'zik00-rendered-options';
                  marker.type = 'application/json';
                  marker.textContent = JSON.stringify({ options, variants, price: renderedPrice, originalPrice: null });
                  document.head.appendChild(marker);
                }
                """);
    }
}
