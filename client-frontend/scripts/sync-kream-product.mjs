import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sourcePath = resolve(projectRoot, '..', 'item_data', 'kream_output.json');
const outputPath = resolve(projectRoot, 'src', 'generated', 'kreamFeaturedProduct.json');
const selectedProductId = 'KREAM-489756';
const toImageProxyUrl = (imageUrl) =>
  `/api/product-images/proxy?url=${encodeURIComponent(imageUrl)}`;

const items = JSON.parse(await readFile(sourcePath, 'utf8'));
if (!Array.isArray(items)) {
  throw new Error(`상품 데이터가 배열이 아닙니다: ${sourcePath}`);
}

const selected = items.find((item) => item.productId === selectedProductId);
if (!selected) {
  await readFile(outputPath, 'utf8');
  console.warn(`${selectedProductId} 상품이 없어 기존 생성 데이터를 유지합니다.`);
} else {
  const slug = Buffer.from(selected.productId, 'utf8').toString('base64url');
  const productImages = (selected.images ?? []).map(toImageProxyUrl);
  const thumbnail = selected.thumbnailUrl
    ? toImageProxyUrl(selected.thumbnailUrl)
    : undefined;
  const reviews = (selected.reviews ?? []).map((review) => ({
    ...review,
    images: (review.images ?? []).map(toImageProxyUrl),
  }));

  const featuredProduct = {
  id: selected.productId,
  slug,
  sourceUrl: selected.sourceUrl,
  name: selected.name,
    category: selected.category,
    price: selected.discountedPrice ?? selected.price,
    image: productImages[0] ?? thumbnail,
    images: productImages.length > 0 ? productImages : [thumbnail].filter(Boolean),
    brand: selected.brand,
    description: selected.description,
    currency: selected.currency,
    rating: selected.rating,
    reviewCount: reviews.length || selected.reviewCount || 0,
    reviews,
    tags: selected.tags,
    badge: 'KREAM',
  };

  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, `${JSON.stringify(featuredProduct, null, 2)}\n`, 'utf8');
  console.log(`Synced ${selected.productId} from item_data/kream_output.json`);
}
