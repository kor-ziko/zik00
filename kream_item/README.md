# KREAM 상품 수집기

KREAM 검색 결과에서 상품 URL을 찾고, 상세·리뷰 페이지의 공개 HTML과 JSON-LD를
합쳐 요청한 JSON 형식으로 저장합니다. 상세·리뷰 수집은 브라우저 자동화 대신 일반
HTML 요청을 사용합니다. `categories.py`에는 전달받은 전체 분류 체계가 표준화되어 있습니다.

## 설치

Python 3.11 이상을 권장합니다.

```powershell
cd C:\Users\user\Desktop\zik\shop\kream_item
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m playwright install chromium
```

## 실행

작게 시험한 뒤 범위를 늘리는 것을 권장합니다.

```powershell
python main.py --category "여성 셔츠" --max-per-category 5 --max-products 5
python main.py --category "여성 셔츠" --max-per-category 5 --max-reviews-per-product 5
python main.py --category "여성 셔츠" --category "남성 셔츠" --max-products 20
python main.py --resume --start-category "남성 점프슈트" --max-per-category 5
python main.py --list-categories
```

기본 출력은 `shop/item_data/kream_output.json`입니다. 실행 위치와 관계없이
`C:\Users\user\Desktop\zik\shop\item_data\kream_output.json`에 저장됩니다.
옵션 없이 `python main.py`를 실행하면 전체 분류에서 카테고리마다 최대 5개씩
수집합니다. `--max-per-category 0`을 지정하면 검색 결과를 제한 없이 탐색합니다.
중복 상품은 한 번만 저장합니다. 상품 1개마다
결과를 중간 저장하므로 실행 도중에도 `kream_output.json`에서 진행 결과를 볼 수 있습니다.
전체 수집은 상품 수에 따라 수 시간 이상 걸릴 수 있으며 KREAM의 요청 제한에 영향을
받을 수 있습니다. 일부만 시험하려면 `--max-per-category`와 `--max-products`를 지정하세요.
중단된 작업은 `--resume --start-category "중단된 카테고리"`로 기존 JSON에 이어서
저장할 수 있습니다. 기존 상품 ID는 자동으로 중복 제거됩니다.
장시간 실행 중 상세 페이지가 연속 실패하면 기본 300초 휴식한 뒤 계속합니다.
또한 새 상품 50개마다 180초 예방 휴식을 적용합니다. `--failure-cooldown`,
`--pause-every`, `--pause-seconds`로 값을 조정할 수 있습니다.
KREAM이 자동 수집 요청을 HTTP 500으로 제한하는 상태가 서로 다른 상품 3개에서
연속 확인되면 요청을 더 보내지 않고 현재 JSON을 저장한 뒤 종료합니다. 제한이 풀린
뒤 화면에 출력된 `--resume --start-category` 명령으로 이어서 실행하세요.

## 필드 원칙

- `price`: 할인 전 가격이 화면에 있으면 할인 전 가격, 없으면 현재 가격
- `discountedPrice`: KREAM이 공개한 현재 판매가
- `stockCount`: KREAM 공개 페이지가 실제 수량을 제공하지 않으므로 `null`
- `options`, `variants`: 로그인/구매 동작 없이 검증 가능한 공개 옵션이 없으면 빈 배열
- `createdAt`: 공개되지 않으므로 `null`
- `updatedAt`: 해당 상품을 수집한 UTC 시각
- `productId`: 원본 ID와 충돌하지 않도록 `KREAM-{원본상품번호}`
- `sourceUrl`: KREAM 원본 상품 상세 페이지 주소
- `rating`, `reviewCount`: KREAM 일반 리뷰의 공개 별점과 개수
- `reviews`: 작성자·본문·작성 표시 시각·이미지·좋아요·원문 링크가 있는 공개 스타일 리뷰

기본적으로 상품마다 스타일 리뷰를 최대 5개 저장합니다.
`--max-reviews-per-product 10`처럼 수를 바꿀 수 있고, 리뷰 페이지 요청을 생략하려면
`--max-reviews-per-product 0`을 사용하세요.

사이트 이용약관과 `robots.txt`를 확인하고, 개인적·허용된 범위에서 사용하세요. 기본 요청 간격은 1초이며 코드가 강제하는 최솟값은 0.5초입니다. 페이지 구조가 바뀌면 `crawler.py`의 공개 JSON-LD/DOM 추출부를 조정해야 합니다.

## 테스트

```powershell
python -m unittest discover -s tests -v
```
