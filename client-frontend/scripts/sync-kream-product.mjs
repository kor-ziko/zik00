import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sourcePath = resolve(projectRoot, '..', 'item_data', 'kream_output.json');
const outputPath = resolve(projectRoot, 'src', 'generated', 'kreamFeaturedProduct.json');
const selectedProductId = 'KREAM-489756';

const items = JSON.parse(await readFile(sourcePath, 'utf8'));
if (!Array.isArray(items)) {
  throw new Error(`상품 데이터가 배열이 아닙니다: ${sourcePath}`);
}

const selected = items.find((item) => item.productId === selectedProductId);
if (!selected) {
  throw new Error(`${selectedProductId} 상품을 찾을 수 없습니다: ${sourcePath}`);
}

const slug = createHash('sha256').update(selected.productId).digest('hex').slice(0, 8);
const localImages = selected.images.map(
  (_image, index) => `/assets/kream-${slug}/image-${index + 1}.png`,
);

const featuredProduct = {
  id: selected.productId,
  slug,
  name: selected.name,
  category: selected.category,
  price: selected.discountedPrice ?? selected.price,
  image: localImages[0] ?? selected.thumbnailUrl,
  images: localImages.length > 0 ? localImages : [selected.thumbnailUrl],
  brand: selected.brand,
  description: selected.description,
  currency: selected.currency,
  rating: selected.rating,
  reviewCount: selected.reviewCount,
  tags: selected.tags,
  badge: 'KREAM',
};

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${JSON.stringify(featuredProduct, null, 2)}\n`, 'utf8');
console.log(`Synced ${selected.productId} from item_data/kream_output.json`);
