from __future__ import annotations

import asyncio
import html as html_module
import json
import random
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, AsyncIterator, Callable, Iterable
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urljoin, urlparse
from urllib.request import Request, urlopen

from bs4 import BeautifulSoup
from categories import Category, classify
from models import KreamProduct, ProductReview


BASE_URL = "https://kream.co.kr"
PRODUCT_ID_RE = re.compile(r"/products/(\d+)")
MONEY_RE = re.compile(r"^([\d,]+)원$")
REVIEW_RE = re.compile(r"^리뷰\s*([\d,]+)$")
HTTP_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 Chrome/126 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "ko-KR,ko;q=0.9",
    "Connection": "close",
}


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


def _download_html(url: str, timeout: float = 30.0) -> str:
    request = Request(url, headers=HTTP_HEADERS)
    try:
        with urlopen(request, timeout=timeout) as response:
            return response.read().decode("utf-8", errors="replace")
    except HTTPError as exc:
        raise RuntimeError(f"KREAM HTTP {exc.code} 응답") from exc
    except URLError as exc:
        raise RuntimeError(f"KREAM 연결 실패: {exc.reason}") from exc


def _meta(soup: BeautifulSoup, name: str) -> str | None:
    node = soup.select_one(f'meta[name="{name}"], meta[property="{name}"]')
    return str(node.get("content")).strip() if node and node.get("content") else None


def payload_from_html(html: str, url: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "html.parser")
    product: dict[str, Any] | None = None
    for script in soup.select('script[type="application/ld+json"]'):
        try:
            parsed = json.loads(script.get_text() or "null")
        except (TypeError, json.JSONDecodeError):
            continue
        candidates = parsed if isinstance(parsed, list) else [parsed]
        product = next(
            (
                item
                for item in candidates
                if isinstance(item, dict) and item.get("@type") == "Product"
            ),
            product,
        )

    path_match = PRODUCT_ID_RE.search(url)
    product_id = _meta(soup, "product:retailer_item_id") or (
        path_match.group(1) if path_match else None
    )
    if not product:
        image = _meta(soup, "og:image")
        product = {
            "@type": "Product",
            "name": _meta(soup, "og:title") or (soup.title.get_text(strip=True) if soup.title else ""),
            "description": _meta(soup, "og:description") or _meta(soup, "description"),
            "image": [image] if image else [],
            "productID": product_id,
            "brand": (
                {"name": _meta(soup, "product:brand")}
                if _meta(soup, "product:brand")
                else None
            ),
            "offers": {
                "price": _integer(_meta(soup, "product:price:amount")),
                "priceCurrency": _meta(soup, "product:price:currency") or "KRW",
                "availability": _meta(soup, "product:availability") or "",
            },
        }

    texts = [
        node.get_text(" ", strip=True)
        for node in soup.select("p, span")
        if node.get_text(" ", strip=True)
    ]
    originals = [
        node.get_text(" ", strip=True)
        for node in soup.select("p.strikethrough")
        if MONEY_RE.fullmatch(node.get_text(" ", strip=True))
    ]
    review = next((value for value in texts if REVIEW_RE.fullmatch(value)), None)
    rating = next(
        (
            value
            for node in soup.select(".rating-number span, p.semibold")
            if re.fullmatch(r"[0-5](?:\.\d)?", value := node.get_text(" ", strip=True))
        ),
        None,
    )
    source_categories = list(
        dict.fromkeys(
            node.get_text(" ", strip=True)
            for node in soup.select('a[href^="/categories/"]')
            if node.get_text(" ", strip=True)
        )
    )
    page_title = _meta(soup, "title") or (soup.title.get_text(strip=True) if soup.title else "")
    rendered_style_url = next(
        (
            urljoin(BASE_URL, str(node.get("href")))
            for node in soup.select('a[href*="/social/products/"][href*="/details"]')
            if f"/social/products/{product_id}/details" in str(node.get("href"))
        ),
        None,
    )
    style_match = re.search(
        rf"(?:https://kream\.co\.kr)?/social/products/{re.escape(str(product_id))}"
        r'/details[^"\\\s<]*',
        html,
    )
    style_review_url = rendered_style_url or (
        urljoin(BASE_URL, html_module.unescape(style_match.group(0)))
        if style_match
        else None
    )
    return {
        "product": product,
        "productId": product_id,
        "displayName": re.sub(
            r"\s*\|\s*[^|]+\s*\|\s*KREAM\s*$",
            "",
            page_title,
        ).strip(),
        "originalPrice": originals[0] if originals else None,
        "rating": float(rating) if rating is not None else None,
        "reviewCount": re.sub(r"[^0-9]", "", review) if review else 0,
        "availability": _meta(soup, "product:availability"),
        "sourceCategories": source_categories,
        "styleReviewUrl": style_review_url,
    }


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
        sourceUrl=f"{BASE_URL}/products/{product_id}",
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
    def __init__(
        self,
        *,
        headless: bool = True,
        delay: float = 3.0,
        retries: int = 2,
        failure_cooldown: float = 300.0,
        pause_every: int = 50,
        pause_seconds: float = 180.0,
        max_reviews_per_product: int = 5,
    ) -> None:
        self.headless = headless
        self.delay = max(delay, 0.5)
        self.retries = max(retries, 0)
        self.failure_cooldown = max(failure_cooldown, 0.0)
        self.pause_every = max(pause_every, 0)
        self.pause_seconds = max(pause_seconds, 0.0)
        self.max_reviews_per_product = max(max_reviews_per_product, 0)

    async def new_context(self, browser: Any) -> Any:
        return await browser.new_context(
            locale="ko-KR",
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 Chrome/126 Safari/537.36"
            ),
        )

    async def fetch_reviews(
        self,
        product_id: str,
        style_review_url: str | None,
    ) -> list[ProductReview]:
        if self.max_reviews_per_product == 0 or not style_review_url:
            return []
        html = await asyncio.to_thread(_download_html, style_review_url)
        soup = BeautifulSoup(html, "html.parser")
        reviews: list[ProductReview] = []
        product_path = f"/products/{product_id}"
        for card in soup.select(".social_post_detail"):
            tagged_paths = {
                urlparse(str(link.get("href"))).path
                for link in card.select('a.product_link[href^="/products/"]')
            }
            if product_path not in tagged_paths:
                continue
            post_id = (
                card.get("data-post-id")
                or card.get("data-feed-card")
                or str(card.get("id") or "").removeprefix("p")
            )
            if not post_id:
                continue
            profile = card.select_one('a.profile_info[href^="/social/users/"]')
            author_node = profile.select_one("span") if profile else None
            content_node = card.select_one(".text, h2")
            images = list(
                dict.fromkeys(
                    str(image.get("src"))
                    for image in card.select("img.full_width.base-image__image[src]")
                    if not image.find_parent("a", class_="product_link")
                )
            )
            like_node = card.select_one("a.btn.like button, a.btn.like span")
            created_node = card.select_one(".upload_time")
            reviews.append(
                ProductReview(
                    reviewId=f"KREAM-STYLE-{post_id}",
                    reviewType="style",
                    author=(
                        author_node.get_text(" ", strip=True)
                        if author_node
                        else profile.get_text(" ", strip=True)
                        if profile
                        else None
                    ),
                    content=content_node.get_text(" ", strip=True) if content_node else None,
                    createdAt=created_node.get_text(" ", strip=True) if created_node else None,
                    rating=None,
                    likeCount=_integer(like_node.get_text(" ", strip=True) if like_node else None),
                    images=images,
                    reviewUrl=f"{BASE_URL}/social/products/{product_id}/details?p={post_id}",
                )
            )
            if len(reviews) >= self.max_reviews_per_product:
                break
        return reviews

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

    async def fetch_one(self, ref: ProductRef) -> KreamProduct:
        last_error: Exception | None = None
        for attempt in range(self.retries + 1):
            try:
                html = await asyncio.to_thread(_download_html, ref.url)
                payload = payload_from_html(html, ref.url)
                product = product_from_payload(
                    payload,
                    ref.seed,
                    datetime.now(timezone.utc).isoformat(),
                )
                if self.max_reviews_per_product > 0:
                    try:
                        product.reviews = await self.fetch_reviews(
                            ref.product_id,
                            payload.get("styleReviewUrl"),
                        )
                        if payload.get("styleReviewUrl"):
                            print(
                                f"  리뷰 수집: KREAM-{ref.product_id} {len(product.reviews)}개",
                                flush=True,
                            )
                    except Exception as review_exc:
                        detail = str(review_exc).splitlines()[0][:120]
                        print(
                            f"  리뷰 수집 실패, 상품은 저장: "
                            f"KREAM-{ref.product_id} ({type(review_exc).__name__}: {detail})",
                            flush=True,
                        )
                return product
            except Exception as exc:  # Playwright exceptions vary by browser version.
                last_error = exc
                detail = str(exc).splitlines()[0][:120]
                # A repeated 500 is commonly KREAM's temporary automation limit.
                # Retrying the same URL three times only extends that limit.
                if "HTTP 500" in detail and attempt >= 1:
                    break
                if attempt < self.retries:
                    is_throttled = (
                        "Timeout" in type(exc).__name__
                        or "HTTP 403" in detail
                        or "HTTP 429" in detail
                    )
                    wait_seconds = (
                        60 * (attempt + 1)
                        if is_throttled
                        else (2**attempt) + random.random()
                    )
                    print(
                        f"  상세 재시도 {attempt + 1}/{self.retries}: "
                        f"KREAM-{ref.product_id} ({type(exc).__name__}: {detail}) "
                        f"- {wait_seconds:.0f}초 대기",
                        flush=True,
                    )
                    await asyncio.sleep(wait_seconds)
        raise RuntimeError(f"상품 수집 실패: {ref.url}: {last_error}") from last_error

    async def crawl(
        self,
        categories: Iterable[Category],
        max_per_category: int,
        max_products: int,
        checkpoint_every: int = 25,
        checkpoint: Callable[[list[KreamProduct]], None] | None = None,
        initial_products: Iterable[KreamProduct] = (),
    ) -> tuple[list[KreamProduct], list[str]]:
        try:
            from playwright.async_api import async_playwright
        except ImportError as exc:
            raise RuntimeError("playwright가 없습니다. requirements.txt를 설치하세요.") from exc

        results: list[KreamProduct] = list(initial_products)
        initial_count = len(results)
        errors: list[str] = []
        seen: set[str] = {product.productId.removeprefix("KREAM-") for product in results}
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
            discovery_context = await self.new_context(browser)
            discovery_page = await discovery_context.new_page()
            consecutive_failures = 0
            consecutive_http_500 = 0
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
                            duplicate_count = 0
                            for ref in refs:
                                if ref.product_id in seen:
                                    duplicate_count += 1
                                    continue
                                if max_products > 0 and len(results) >= max_products:
                                    break
                                try:
                                    print(f"  상세 확인: KREAM-{ref.product_id}", flush=True)
                                    product = await self.fetch_one(ref)
                                    results.append(product)
                                    seen.add(ref.product_id)
                                    consecutive_failures = 0
                                    consecutive_http_500 = 0
                                    print(f"수집 {len(results)}개: {product.name}", flush=True)
                                    new_count = len(results) - initial_count
                                    if checkpoint and new_count % max(checkpoint_every, 1) == 0:
                                        checkpoint(results)
                                    if (
                                        self.pause_every > 0
                                        and new_count > 0
                                        and new_count % self.pause_every == 0
                                        and self.pause_seconds > 0
                                    ):
                                        print(
                                            f"  {new_count}개 추가 수집 완료: "
                                            f"서버 보호를 위해 {self.pause_seconds:.0f}초 휴식합니다.",
                                            flush=True,
                                        )
                                        await asyncio.sleep(self.pause_seconds)
                                except Exception as exc:
                                    errors.append(str(exc))
                                    consecutive_failures += 1
                                    if "KREAM HTTP 500" in str(exc):
                                        consecutive_http_500 += 1
                                    else:
                                        consecutive_http_500 = 0
                                    print(f"  상품 실패, 다음으로 이동: KREAM-{ref.product_id}", flush=True)
                                    if consecutive_http_500 >= 3:
                                        message = (
                                            "KREAM의 임시 자동 수집 제한(HTTP 500)이 "
                                            "서로 다른 상품 3개에서 연속 감지됐습니다. "
                                            "현재 결과를 저장하고 종료합니다."
                                        )
                                        errors.append(message)
                                        print(f"  {message}", flush=True)
                                        print(
                                            f'  나중에 이어서 실행: python main.py --resume '
                                            f'--start-category "{category.path}"',
                                            flush=True,
                                        )
                                        if checkpoint:
                                            checkpoint(results)
                                        return results, errors
                                    if consecutive_failures >= 3 and self.failure_cooldown > 0:
                                        print(
                                            f"  연속 실패 {consecutive_failures}회: "
                                            f"{self.failure_cooldown:.0f}초 휴식 후 계속합니다.",
                                            flush=True,
                                        )
                                        await asyncio.sleep(self.failure_cooldown)
                                        consecutive_failures = 0
                                await asyncio.sleep(self.delay + random.random() * 0.3)
                            if duplicate_count:
                                print(f"  기존 상품 중복: {duplicate_count}개 건너뜀", flush=True)
                            if max_products > 0 and len(results) >= max_products:
                                break
                        print(f"카테고리 검색 완료: {category_found}개 발견", flush=True)
                    except Exception as exc:
                        errors.append(f"검색 실패 [{category.path}]: {exc}")
                        continue
            finally:
                await discovery_context.close()
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
