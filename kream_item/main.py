from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path

from categories import CATEGORIES, find_category
from crawler import KreamCrawler, write_json


DEFAULT_OUTPUT = Path(__file__).resolve().parent / "kream_output.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="KREAM 상품을 표준 JSON으로 수집합니다.")
    parser.add_argument("--category", action="append", help="수집할 leaf 또는 전체 경로. 여러 번 지정 가능")
    parser.add_argument("--max-per-category", type=int, default=0, help="카테고리당 제한. 0은 무제한")
    parser.add_argument("--max-products", type=int, default=0, help="전체 상품 제한. 0은 무제한")
    parser.add_argument("--checkpoint-every", type=int, default=25, help="중간 저장 주기")
    parser.add_argument("--delay", type=float, default=1.0, help="상품 요청 간 최소 대기(초, 최솟값 0.5)")
    parser.add_argument("--retries", type=int, default=2)
    parser.add_argument("--headed", action="store_true", help="브라우저 창을 표시")
    parser.add_argument("--list-categories", action="store_true")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    return parser.parse_args()


async def run(args: argparse.Namespace) -> int:
    if args.list_categories:
        print("\n".join(category.path for category in CATEGORIES))
        return 0
    try:
        categories = [find_category(value) for value in args.category] if args.category else CATEGORIES
    except ValueError as exc:
        print(f"오류: {exc}", file=sys.stderr)
        return 2
    crawler = KreamCrawler(headless=not args.headed, delay=args.delay, retries=args.retries)
    write_json([], args.output)
    try:
        products, errors = await crawler.crawl(
            categories,
            args.max_per_category,
            args.max_products,
            checkpoint_every=args.checkpoint_every,
            checkpoint=lambda items: write_json(items, args.output),
        )
    except RuntimeError as exc:
        print(f"실행 오류: {exc}", file=sys.stderr)
        return 1
    write_json(products, args.output)
    print(f"완료: {len(products)}개 -> {args.output.resolve()}")
    if errors:
        error_file = args.output.with_suffix(".errors.log")
        error_file.write_text("\n".join(errors) + "\n", encoding="utf-8")
        print(f"실패 {len(errors)}건 -> {error_file.resolve()}", file=sys.stderr)
    return 0 if products else 1


if __name__ == "__main__":
    raise SystemExit(asyncio.run(run(parse_args())))
