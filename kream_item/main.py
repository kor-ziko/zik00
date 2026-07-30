from __future__ import annotations

import argparse
import asyncio
import json
import sys
from pathlib import Path

from categories import CATEGORIES, find_category
from crawler import KreamCrawler, write_json
from models import KreamProduct


DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "item_data" / "kream_output.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="KREAM 상품을 표준 JSON으로 수집합니다.")
    parser.add_argument("--category", action="append", help="수집할 leaf 또는 전체 경로. 여러 번 지정 가능")
    parser.add_argument("--start-category", help="전체 분류 중 이 카테고리부터 마지막까지 수집")
    parser.add_argument("--resume", action="store_true", help="기존 출력 JSON을 유지하며 이어서 수집")
    parser.add_argument("--max-per-category", type=int, default=0, help="카테고리당 제한. 0은 무제한")
    parser.add_argument("--max-products", type=int, default=0, help="전체 상품 제한. 0은 무제한")
    parser.add_argument("--checkpoint-every", type=int, default=25, help="중간 저장 주기")
    parser.add_argument("--delay", type=float, default=1.0, help="상품 요청 간 최소 대기(초, 최솟값 0.5)")
    parser.add_argument("--retries", type=int, default=2)
    parser.add_argument("--failure-cooldown", type=float, default=300.0, help="상세 수집 3회 연속 실패 시 대기 시간")
    parser.add_argument("--pause-every", type=int, default=100, help="이 수만큼 새 상품 수집 후 예방 휴식")
    parser.add_argument("--pause-seconds", type=float, default=60.0, help="예방 휴식 시간")
    parser.add_argument("--headed", action="store_true", help="브라우저 창을 표시")
    parser.add_argument("--list-categories", action="store_true")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    return parser.parse_args()


async def run(args: argparse.Namespace) -> int:
    if args.list_categories:
        print("\n".join(category.path for category in CATEGORIES))
        return 0
    try:
        if args.category and args.start_category:
            raise ValueError("--category와 --start-category는 함께 사용할 수 없습니다.")
        if args.category:
            categories = [find_category(value) for value in args.category]
        elif args.start_category:
            start = find_category(args.start_category)
            categories = CATEGORIES[CATEGORIES.index(start) :]
        else:
            categories = CATEGORIES
    except ValueError as exc:
        print(f"오류: {exc}", file=sys.stderr)
        return 2

    initial_products: list[KreamProduct] = []
    if args.resume and args.output.exists():
        try:
            raw_items = json.loads(args.output.read_text(encoding="utf-8"))
            if not isinstance(raw_items, list):
                raise ValueError("JSON 최상위 값이 배열이 아닙니다.")
            initial_products = [KreamProduct(**item) for item in raw_items]
            print(f"기존 상품 {len(initial_products)}개를 불러왔습니다.", flush=True)
        except (OSError, ValueError, TypeError, json.JSONDecodeError) as exc:
            print(f"재개 오류: 기존 출력 파일을 읽을 수 없습니다: {exc}", file=sys.stderr)
            return 2

    crawler = KreamCrawler(
        headless=not args.headed,
        delay=args.delay,
        retries=args.retries,
        failure_cooldown=args.failure_cooldown,
        pause_every=args.pause_every,
        pause_seconds=args.pause_seconds,
    )
    if not args.resume:
        write_json([], args.output)
    try:
        products, errors = await crawler.crawl(
            categories,
            args.max_per_category,
            args.max_products,
            checkpoint_every=args.checkpoint_every,
            checkpoint=lambda items: write_json(items, args.output),
            initial_products=initial_products,
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
