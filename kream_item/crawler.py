from __future__ import annotations

import asyncio
import json
import random
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, AsyncIterator, Callable, Iterable
from urllib.parse import quote, urljoin

from categories import Category, classify
from models import KreamProduct


BASE_URL = "https://kream.co.kr"
PRODUCT_ID_RE = re.compile(r"/products/(\d+)")
MONEY_RE = re.compile(r"^([\d,]+)원$")
REVIEW_RE = re.compile(r"^리뷰\s*([\d,]+)$")


@dataclass(frozen=True, slots=True)
class ProductRef:
    product_id: str
    url: str
    seed: Category


def _integer(value: Any, default: int = 0) -> int:
    if isinstance(value, (int, float)):
        return int(value)
    match = re.search(r"[\d,]+", str(value or ""))
    return int(match.group().replace(",", "")) if match else default


def _images(value: Any) -> list[str]:
    values = value if isinstance(value, list) else [value]
    return list(dict.fromkeys(str(item) for item in values if item))


def product_from_payload(payload: dict[str, Any], seed: Category, crawled_at: str) -> KreamProduct:
    ld = payload["product"]
    offers = ld.get("offers") or {}
    product_id = str(ld.get("productID") or payload["productId"])
    current_price = _integer(offers.get("price"))
    original_price = _integer(payload.get("originalPrice"), current_price)
    if original_price < current_price:
        original_price = current_price
    discount_rate = round((original_price - current_price) * 100 / original_price) if original_price else 0
    availability = str(offers.get("availability", "")).lower()
    available = availability.endswith("instock") or payload.get("availability") == "in stock"
    brand_value = ld.get("brand")
    brand = brand_value.get("name") if isinstance(brand_value, dict) else brand_value
    images = _images(ld.get("image"))
    source_categories = [str(x) for x in payload.get("sourceCategories", []) if x]
    classification_text = " ".join([ld.get("name", ""), ld.get("description", ""), *source_categories])
    category = classify(classification_text, seed)
    tags = list(dict.fromkeys([*(source_categories), *([str(brand)] if brand else [])]))
    display_name = str(payload.get("displayName") or ld.get("name") or "").strip()
    return KreamProduct(
        productId=f"KREAM-{product_id}",
        name=display_name,
        category=category.path,
        brand=str(brand).strip() if brand else None,
        description=str(ld.get("description")).strip() if ld.get("description") else None,
        price=original_price,
        currency=str(offers.get("priceCurrency") or "KRW"),
        discountRate=discount_rate,
        discountedPrice=current_price,
        isAvailable=available,
        stockCount=None,
        thumbnailUrl=images[0] if images else None,
        images=images,
        options=[],
        variants=[],
        rating=float(payload["rating"]) if payload.get("rating") is not None else None,
        reviewCount=_integer(payload.get("reviewCount")),
        tags=tags,
        createdAt=None,
        updatedAt=crawled_at,
    )


class KreamCrawler:
    def __init__(self, *, headless: bool = True, delay: float = 1.0, retries: int = 2) -> None:
        self.headless = headless
        self.delay = max(delay, 0.5)
        self.retries = max(retries, 0)

    async def discover_batches(
        self, page: Any, category: Category, limit: int
    ) -> AsyncIterator[list[ProductRef]]:
        url = f"{BASE_URL}/search?keyword={quote(category.leaf)}"
        await page.goto(url, wait_until="domcontentloaded", timeout=45_000)
        await page.wait_for_selector('a[href*="/products/"]', state="attached", timeout=20_000)
        seen: set[str] = set()
        unchanged_rounds = 0
        while unchanged_rounds < 3:
            links: list[str] = await page.locator('a[href*="/products/"]').evaluate_all(
                "els => els.map(el => el.getAttribute('href')).filter(Boolean)"
            )
            batch: list[ProductRef] = []
            for link in links:
                match = PRODUCT_ID_RE.search(link)
                if not match or match.group(1) in seen:
                    continue
                seen.add(match.group(1))
                batch.append(ProductRef(match.group(1), urljoin(BASE_URL, link), category))
                if limit > 0 and len(seen) >= limit:
                    break
            if batch:
                unchanged_rounds = 0
                yield batch
            else:
                unchanged_rounds += 1
            if limit > 0 and len(seen) >= limit:
                return
            if unchanged_rounds >= 3:
                return
            await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            await page.wait_for_timeout(1_000)

    async def fetch_one(self, page: Any, ref: ProductRef) -> KreamProduct:
        last_error: Exception | None = None
        for attempt in range(self.retries + 1):
            try:
                await page.goto(ref.url, wait_until="domcontentloaded", timeout=45_000)
                # JSON-LD script tags are intentionally hidden; wait for DOM attachment.
                await page.wait_for_selector("script#Product", state="attached", timeout=20_000)
                payload = await page.evaluate(
                    r"""
                    () => {
                      const product = JSON.parse(document.querySelector('script#Product').textContent);
                      const texts = [...document.querySelectorAll('p, span')]
                        .map(el => (el.textContent || '').trim()).filter(Boolean);
                      const originals = [...document.querySelectorAll('p.strikethrough')]
                        .map(el => (el.textContent || '').trim()).filter(v => /^[\\d,]+원$/.test(v));
                      const review = texts.find(v => /^리뷰\\s*[\\d,]+$/.test(v));
                      const ratingNode = [...document.querySelectorAll('.rating-number span, p.semibold')]
                        .map(el => (el.textContent || '').trim()).find(v => /^[0-5](?:\\.\\d)?$/.test(v));
                      const sourceCategories = [...document.querySelectorAll('a[href^="/categories/"]')]
                        .map(el => (el.textContent || '').trim()).filter(Boolean);
                      const meta = name => document.querySelector(`meta[name="${name}"], meta[property="${name}"]`)?.content;
                      const pageTitle = meta('title') || document.title || '';
                      return {
                        product,
                        productId: meta('product:retailer_item_id'),
                        displayName: pageTitle.replace(/\s*\|\s*[^|]+\s*\|\s*KREAM\s*$/, '').trim(),
                        originalPrice: originals[0] || null,
                        rating: ratingNode ? Number(ratingNode) : null,
                        reviewCount: review ? review.replace(/[^0-9]/g, '') : 0,
                        availability: meta('product:availability'),
                        sourceCategories: [...new Set(sourceCategories)]
                      };
                    }
                    """
                )
                return product_from_payload(payload, ref.seed, datetime.now(timezone.utc).isoformat())
            except Exception as exc:  # Playwright exceptions vary by browser version.
                last_error = exc
                if attempt < self.retries:
                    await asyncio.sleep((2**attempt) + random.random())
        raise RuntimeError(f"상품 수집 실패: {ref.url}: {last_error}") from last_error

    async def crawl(
        self,
        categories: Iterable[Category],
        max_per_category: int,
        max_products: int,
        checkpoint_every: int = 25,
        checkpoint: Callable[[list[KreamProduct]], None] | None = None,
    ) -> tuple[list[KreamProduct], list[str]]:
        try:
            from playwright.async_api import async_playwright
        except ImportError as exc:
            raise RuntimeError("playwright가 없습니다. requirements.txt를 설치하세요.") from exc

        results: list[KreamProduct] = []
        errors: list[str] = []
        seen: set[str] = set()
        async with async_playwright() as playwright:
            try:
                browser = await playwright.chromium.launch(headless=self.headless)
            except Exception as exc:
                if "Executable doesn't exist" in str(exc):
                    raise RuntimeError(
                        "Playwright Chromium이 설치되지 않았습니다. "
                        "'python -m playwright install chromium'을 한 번 실행하세요."
                    ) from exc
                raise
            context = await browser.new_context(
                locale="ko-KR",
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126 Safari/537.36",
            )
            discovery_page = await context.new_page()
            detail_page = await context.new_page()
            try:
                for category in categories:
                    if max_products > 0 and len(results) >= max_products:
                        break
                    print(f"검색 중: {category.path}", flush=True)
                    try:
                        category_limit = max_per_category
                        if max_products > 0:
                            remaining = max_products - len(results)
                            category_limit = min(category_limit, remaining) if category_limit > 0 else remaining
                        category_found = 0
                        async for refs in self.discover_batches(discovery_page, category, category_limit):
                            category_found += len(refs)
                            print(f"  검색 발견: {category_found}개", flush=True)
                            for ref in refs:
                                if ref.product_id in seen:
                                    continue
                                if max_products > 0 and len(results) >= max_products:
                                    break
                                seen.add(ref.product_id)
                                try:
                                    product = await self.fetch_one(detail_page, ref)
                                    results.append(product)
                                    print(f"수집 {len(results)}개: {product.name}", flush=True)
                                    if checkpoint and len(results) % max(checkpoint_every, 1) == 0:
                                        checkpoint(results)
                                except Exception as exc:
                                    errors.append(str(exc))
                                await asyncio.sleep(self.delay + random.random() * 0.3)
                            if max_products > 0 and len(results) >= max_products:
                                break
                        print(f"카테고리 검색 완료: {category_found}개 발견", flush=True)
                    except Exception as exc:
                        errors.append(f"검색 실패 [{category.path}]: {exc}")
                        continue
            finally:
                await context.close()
                await browser.close()
        if checkpoint:
            checkpoint(results)
        return results, errors


def write_json(products: Iterable[KreamProduct], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".tmp")
    temporary.write_text(
        json.dumps([product.to_dict() for product in products], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    temporary.replace(output)
