from __future__ import annotations

import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from categories import classify, find_category  # noqa: E402
from crawler import product_from_payload  # noqa: E402


class CategoryTest(unittest.TestCase):
    def test_typo_alias_is_normalized(self) -> None:
        category = find_category("남성 후드티")
        self.assertEqual(classify("암성 후드 티셔츠 블랙", category).leaf, "남성 후드티")

    def test_full_path_lookup(self) -> None:
        path = "뷰티·미용 > 스킨케어 > 선케어"
        self.assertEqual(find_category(path).path, path)


class TransformTest(unittest.TestCase):
    def test_product_json_contract(self) -> None:
        seed = find_category("여성 숏팬츠")
        payload = {
            "productId": "889991",
            "displayName": "아모우 켈리 숏츠 크림",
            "product": {
                "name": "AMOU Cali Shorts Cream",
                "description": "여성 숏 팬츠",
                "image": ["https://example.com/1.png", "https://example.com/2.png"],
                "productID": "889991",
                "brand": {"name": "AMOU"},
                "offers": {"price": 49000, "priceCurrency": "KRW", "availability": "https://schema.org/InStock"},
            },
            "originalPrice": "98,000원",
            "rating": 4.9,
            "reviewCount": "24",
            "sourceCategories": ["숏 팬츠", "하의"],
        }
        item = product_from_payload(payload, seed, datetime.now(timezone.utc).isoformat())
        self.assertEqual(item.productId, "KREAM-889991")
        self.assertEqual(item.name, "아모우 켈리 숏츠 크림")
        self.assertEqual(item.price, 98000)
        self.assertEqual(item.discountedPrice, 49000)
        self.assertEqual(item.discountRate, 50)
        self.assertTrue(item.isAvailable)
        self.assertIsNone(item.stockCount)
        self.assertEqual(item.category, seed.path)


if __name__ == "__main__":
    unittest.main()
