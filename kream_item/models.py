from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass(slots=True)
class ProductOption:
    optionType: str
    values: list[str] = field(default_factory=list)


@dataclass(slots=True)
class ProductVariant:
    variantId: str
    sku: str | None = None
    attributes: dict[str, str] = field(default_factory=dict)
    additionalPrice: int = 0
    stockCount: int | None = None


@dataclass(slots=True)
class KreamProduct:
    productId: str
    name: str
    category: str
    brand: str | None
    description: str | None
    price: int
    currency: str = "KRW"
    discountRate: int = 0
    discountedPrice: int = 0
    isAvailable: bool = False
    stockCount: int | None = None
    thumbnailUrl: str | None = None
    images: list[str] = field(default_factory=list)
    options: list[ProductOption] = field(default_factory=list)
    variants: list[ProductVariant] = field(default_factory=list)
    rating: float | None = None
    reviewCount: int = 0
    tags: list[str] = field(default_factory=list)
    createdAt: str | None = None
    updatedAt: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

