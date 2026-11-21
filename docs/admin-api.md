# Valam Admin Panel — Backend API Guide

Everything the admin panel frontend needs: how to run the backend, every endpoint, request/response
shapes, validation rules, errors, and TypeScript types to copy into the frontend.

> **No login yet.** All admin endpoints are currently open. Admin authentication will be added later; plan
> for an `Authorization` header on every request so it is a small change when it lands.

---

## 1. Run the backend locally

Requirements: Java 21, Docker.

```bash
cd backend/ecommerce
docker compose up -d          # PostgreSQL 17 on localhost:5433
./mvnw spring-boot:run        # API on http://localhost:8080 (Windows: mvnw.cmd spring-boot:run)
```

Database migrations run automatically on start and seed the six categories.

| Setting | Value |
|---|---|
| API base URL | `http://localhost:8080` |
| Allowed browser origins (CORS) | `http://localhost:5173` (Vite), `http://localhost:3000` |
| Uploaded images served from | `http://localhost:8080/uploads/...` |

If the frontend dev server runs on another port, add it to `app.cors.allowed-origins` in
`src/main/resources/application.properties` (comma-separated) and restart.

---

## 2. Endpoints at a glance

| Method | Path | Purpose | Success |
|---|---|---|---|
| GET | `/api/v1/categories` | Active categories for dropdowns | 200 `Category[]` |
| GET | `/api/v1/admin/categories` | All categories incl. inactive | 200 `Category[]` |
| GET | `/api/v1/admin/categories/{id}` | One category | 200 `Category` |
| POST | `/api/v1/admin/categories` | Add a category | 201 `Category` |
| PUT | `/api/v1/admin/categories/{id}` | Edit a category (full replace) | 200 `Category` |
| DELETE | `/api/v1/admin/categories/{id}` | Delete an empty category | 204 |
| GET | `/api/v1/admin/products` | Paged product list with filters | 200 `Page<Product>` |
| GET | `/api/v1/admin/products/{id}` | One product | 200 `Product` |
| POST | `/api/v1/admin/products` | Create a product | 201 `Product` |
| PUT | `/api/v1/admin/products/{id}` | Edit a product (full replace) | 200 `Product` |
| PATCH | `/api/v1/admin/products/{id}/status` | Show / hide in store | 200 `Product` |
| DELETE | `/api/v1/admin/products/{id}` | Delete a product | 204 |
| POST | `/api/v1/admin/uploads/images` | Upload a product image | 201 `{ url }` |

All bodies are JSON (`Content-Type: application/json`) except the image upload (`multipart/form-data`).
`POST` responses also carry a `Location` header with the new resource's URL (exposed to the browser via CORS).

---

## 3. Categories

### Category object

```json
{
  "id": 2,
  "name": "Snacks",
  "measureType": "WEIGHT",
  "allowedUnits": ["G", "KG"],
  "active": true,
  "sortOrder": 20
}
```

- `measureType` decides which pack-size units products in this category use:
  `WEIGHT` → `G`, `KG`; `VOLUME` → `ML`, `L`. Use `allowedUnits` to fill the unit dropdown in the product form.
- Lists are sorted by `sortOrder`, then `name`.

Seeded categories (all `WEIGHT`):

| sortOrder | name |
|---|---|
| 10 | Food Products |
| 20 | Snacks |
| 30 | Powder Products |
| 40 | Cow Dung Products |
| 50 | Traditional Seeds |
| 60 | Everyday Essential Products |

Don't hard-code ids — always load them from the API.

### `GET /api/v1/categories`
Active categories only. **Use this for the category dropdown in the product form.**

### `GET /api/v1/admin/categories`
All categories, including inactive ones. Use this on the category management screen.

### `POST /api/v1/admin/categories` / `PUT /api/v1/admin/categories/{id}`

```json
{ "name": "Oils", "measureType": "VOLUME", "sortOrder": 70, "active": true }
```

| Field | Required | Rules | If omitted |
|---|---|---|---|
| `name` | yes | 1–100 chars, unique (case-insensitive) | — |
| `measureType` | yes | `WEIGHT` or `VOLUME`; can't change once the category has products | — |
| `sortOrder` | no | integer ≥ 0 | `0` |
| `active` | no | inactive = hidden from the dropdown | `true` |

`PUT` is a full replace: send all fields, or omitted ones fall back to the defaults above.

### `DELETE /api/v1/admin/categories/{id}`
Only works when the category has no products; otherwise **400** with a message telling the admin to move/delete
the products or set the category inactive. Suggest "Set inactive" in the UI in that case.

---

## 4. Products

### Product object (response)

```json
{
  "id": 14,
  "productCode": "PRD-000014",
  "name": "Rice Murukku",
  "slug": "rice-murukku",
  "shortDescription": "Crunchy rice murukku",
  "description": "Made the traditional way.",
  "categoryId": 2,
  "categoryName": "Snacks",
  "measureType": "WEIGHT",
  "images": [
    { "url": "http://localhost:8080/uploads/images/3f1c….jpg", "altText": "Murukku pack" }
  ],
  "thumbnailUrl": "http://localhost:8080/uploads/images/3f1c….jpg",
  "highlights": ["No palm oil", "Hand made"],
  "ingredients": "Rice flour, urad dal, salt",
  "usageInstructions": null,
  "storageInstructions": "Store in an airtight container",
  "shelfLife": "45 days",
  "countryOfOrigin": "India",
  "hsnCode": "1905",
  "gstRate": 5.00,
  "metaTitle": "Rice Murukku",
  "metaDescription": "Traditional rice murukku",
  "active": true,
  "featured": true,
  "minPrice": 85.00,
  "totalStock": 32,
  "inStock": true,
  "variants": [
    {
      "id": 31,
      "isbn": "2000000000312",
      "quantity": 250,
      "unit": "G",
      "label": "250 g",
      "price": 85.00,
      "mrp": 100.00,
      "discountPercent": 15,
      "stockQuantity": 12,
      "inStock": true
    },
    {
      "id": 32,
      "isbn": "2000000000329",
      "quantity": 1000,
      "unit": "G",
      "label": "1 kg",
      "price": 320.00,
      "mrp": null,
      "discountPercent": null,
      "stockQuantity": 20,
      "inStock": true
    }
  ],
  "version": 3,
  "createdAt": "2026-11-18T10:15:30.123456",
  "updatedAt": "2026-11-18T11:02:04.987654"
}
```

Notes for display:
- `productCode` (e.g. `PRD-000014`) is the human-friendly product ID and each variant's `isbn` is its 13-digit
  barcode number (EAN-13 format, prefix `200`, valid check digit). **Both are generated by the server on
  create, unique, and never change** — show them read-only; there is no field to type them in.
- `thumbnailUrl` = first image, or `null`. Show a placeholder when null.
- `minPrice`, `totalStock`, `inStock` are computed — handy for the list table ("from ₹85", stock badge).
- Variants are sorted by size. **In responses `quantity` is always in the base unit** (`G` or `ML`); use `label`
  for display ("250 g", "1.5 kg").
- `discountPercent` is set only when `mrp` > `price` (rounded down). Show MRP struck through when present.
- Money (`price`, `mrp`, `minPrice`) and `gstRate` are JSON numbers with 2 decimals. Prices include GST.
- `createdAt` / `updatedAt` are ISO-8601 local date-times **without a time zone**; they are India time (IST).

### `GET /api/v1/admin/products` — list

Query parameters (all optional):

| Param | Example | Meaning |
|---|---|---|
| `categoryId` | `2` | Only this category |
| `active` | `true` / `false` | Visible / hidden in store |
| `featured` | `true` / `false` | Featured flag |
| `inStock` | `true` / `false` | At least one pack size in stock / everything out of stock |
| `q` | `murukku`, `PRD-000014` or `2000000000312` | Case-insensitive search in product name, product code **or** any variant ISBN (partial matches work) |
| `page` | `0` | Page number, **0-based** |
| `size` | `20` | Page size, default 20, max 100 |
| `sort` | `name,asc` | `name`, `category.name`, `createdAt`, `updatedAt` with `asc`/`desc`. Default `createdAt,desc` |

Any other `sort` field returns 400.

Response:

```json
{
  "content": [ /* Product objects */ ],
  "page": 0,
  "size": 20,
  "totalElements": 57,
  "totalPages": 3,
  "last": false
}
```

### `GET /api/v1/admin/products/{id}`
One product, or **404**. Use it to load the edit form.

### `POST /api/v1/admin/products` — create

```json
{
  "name": "Rice Murukku",
  "categoryId": 2,
  "shortDescription": "Crunchy rice murukku",
  "description": "Made the traditional way.",
  "images": [
    { "url": "http://localhost:8080/uploads/images/3f1c….jpg", "altText": "Murukku pack" }
  ],
  "highlights": ["No palm oil", "Hand made"],
  "ingredients": "Rice flour, urad dal, salt",
  "storageInstructions": "Store in an airtight container",
  "shelfLife": "45 days",
  "hsnCode": "1905",
  "gstRate": 5,
  "metaTitle": "Rice Murukku",
  "metaDescription": "Traditional rice murukku",
  "active": true,
  "featured": true,
  "variants": [
    { "quantity": 250, "unit": "G", "price": 85, "mrp": 100, "stockQuantity": 12 },
    { "quantity": 1, "unit": "KG", "price": 320, "stockQuantity": 20 }
  ]
}
```

#### Product fields

| Field | Required | Rules | If omitted |
|---|---|---|---|
| `name` | yes | 1–255 chars | — |
| `categoryId` | yes | existing, **active** category | — |
| `slug` | no | ≤ 200, lowercase `a-z`, `0-9`, single hyphens (`rice-murukku`), unique | create: generated from name (`rice-murukku`, then `rice-murukku-2`…); update: unchanged |
| `shortDescription` | no | ≤ 500 | empty |
| `description` | no | ≤ 10 000 | empty |
| `images` | no | ≤ 10 items, display order, first = main | none |
| `images[].url` | yes | absolute `http(s)://` URL, ≤ 1000 | — |
| `images[].altText` | no | ≤ 255 | empty |
| `highlights` | no | ≤ 10 items, each 1–255 chars | none |
| `ingredients` | no | ≤ 5000 | empty |
| `usageInstructions` | no | ≤ 5000 | empty |
| `storageInstructions` | no | ≤ 2000 | empty |
| `shelfLife` | no | free text ≤ 100, e.g. "6 months" | empty |
| `countryOfOrigin` | no | ≤ 100 | `"India"` |
| `hsnCode` | no | 4, 6 or 8 digits | empty |
| `gstRate` | no | 0–100, max 2 decimals | empty |
| `metaTitle` | no | ≤ 100 (aim for ~60 for Google) | empty |
| `metaDescription` | no | ≤ 300 (aim for ~155) | empty |
| `active` | no | visible in store | `true` |
| `featured` | no | | `false` |
| `version` | update only | the `version` you loaded | no conflict check |
| `variants` | yes | 1–20 pack sizes | — |

Slugs drop non-Latin characters, so a Tamil-only name gets the slug `product`, `product-2`, …
For such products, ask the admin for an English slug.

#### Variant (pack size) fields

| Field | Required | Rules |
|---|---|---|
| `id` | update only | Set to update an existing pack size; **omit** to add a new one. Must not be sent on create. |
| `quantity` | yes | > 0; may be decimal in `KG`/`L` (e.g. `1.5`), but must convert to whole g/ml |
| `unit` | yes | one of the category's `allowedUnits` |
| `price` | yes | ≥ 0, max 2 decimals, GST-inclusive selling price |
| `mrp` | no | > 0, max 2 decimals, **≥ price** |
| `stockQuantity` | yes | integer ≥ 0 |

Other rules: no two pack sizes of a product can be the same size (`1 kg` and `1000 g` count as the same).
`productCode` and `isbn` are not request fields — if sent they are ignored.

### `PUT /api/v1/admin/products/{id}` — edit

Same body as create, plus `version`. **It is a full replace**:

- Send **every field** the form has. Omitted optional fields are cleared; omitted `active` becomes `true`
  and omitted `featured` becomes `false` — so always send their current values.
- Variants: items **with `id`** update that pack size, items **without `id`** are added, existing pack sizes
  **missing from the list are deleted**. An edited pack size keeps its `isbn`; one that is removed and added again
  gets a new `isbn`, so always send the `id` of pack sizes you are editing.
- `slug` omitted = keep the current slug. Changing a slug breaks old storefront links; warn the admin.
- Send the `version` from the loaded product. If someone else saved in between, you get **409** —
  show "This product was changed by someone else. Reload to see the latest version."
- A product can keep an inactive category it already has, but can't be moved into an inactive one.
- Changing the category to one with a different measure type requires units that match the new category.

**Pre-filling the edit form from a response:** response variants are in `G`/`ML`. Convert for the form, e.g.
`quantity >= 1000 && quantity % 1000 === 0` → `{ quantity: quantity / 1000, unit: "KG" }` (or `"L"`),
otherwise keep `G`/`ML`. Sending `{ quantity: 1000, unit: "G" }` back is equally valid.

### `PATCH /api/v1/admin/products/{id}/status`

```json
{ "active": false }
```

Quick show/hide toggle for the list table, without loading the whole form.

### `DELETE /api/v1/admin/products/{id}`
Deletes the product and its pack sizes (204). Ask for confirmation in the UI — it can't be undone.
Uploaded image files are not removed from disk.

---

## 5. Image upload

### `POST /api/v1/admin/uploads/images`

`multipart/form-data` with one part named **`file`**.

- JPEG, PNG or WebP only (checked from the file's content, not its name), **max 5 MB**.
- Response **201**: `{ "url": "http://localhost:8080/uploads/images/3f1c2b7e-….jpg" }`

Flow in the product form:
1. Admin picks an image → upload it immediately → show the preview from the returned `url`.
2. Keep the URLs in the form's `images` list (drag to reorder; first = main image).
3. Send them in `images` on save.

```ts
const form = new FormData();
form.append("file", file);
const res = await fetch(`${API}/api/v1/admin/uploads/images`, { method: "POST", body: form });
// Do NOT set Content-Type yourself — the browser adds the multipart boundary.
const { url } = await res.json();
```

Errors: 400 wrong type or missing file, 413 over 5 MB.

---

## 6. Errors

Every error is `application/problem+json` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/admin/products",
  "errors": {
    "name": "must not be blank",
    "variants[0].price": "must not be null",
    "images[1].url": "must be an http(s) URL"
  }
}
```

| Status | When | What to show |
|---|---|---|
| 400 + `errors` | Field validation failed | Put each message under its field. Keys are field paths like `variants[0].price`. |
| 400 (no `errors`) | Business rule broken: duplicate slug/category name, MRP < price, wrong unit, category has products, … | Show `detail` as a form-level message — it's written for humans. |
| 404 | Product/category not found | "Not found", go back to the list |
| 409 | Stale edit (version mismatch) or rare DB conflict | Show `detail`, offer reload |
| 413 | Image over 5 MB | "Image must be 5 MB or smaller" |
| 500 | Server bug | Generic error |

Malformed JSON, unknown enum values (e.g. `"unit": "LB"`) and non-numeric ids also return 400.

---

## 7. TypeScript types

```ts
export type MeasureType = "WEIGHT" | "VOLUME";
export type QuantityUnit = "G" | "KG" | "ML" | "L";

export interface Category {
  id: number;
  name: string;
  measureType: MeasureType;
  allowedUnits: QuantityUnit[];
  active: boolean;
  sortOrder: number;
}

export interface CategoryRequest {
  name: string;
  measureType: MeasureType;
  sortOrder?: number;
  active?: boolean;
}

export interface ProductImage {
  url: string;
  altText?: string | null;
}

export interface Variant {
  id: number;
  isbn: string;          // generated 13-digit barcode number, read-only
  quantity: number;      // base unit (G or ML)
  unit: "G" | "ML";
  label: string;         // "250 g", "1.5 kg"
  price: number;
  mrp: number | null;
  discountPercent: number | null;
  stockQuantity: number;
  inStock: boolean;
}

export interface Product {
  id: number;
  productCode: string;   // generated, e.g. "PRD-000014", read-only
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  categoryId: number;
  categoryName: string;
  measureType: MeasureType;
  images: ProductImage[];
  thumbnailUrl: string | null;
  highlights: string[];
  ingredients: string | null;
  usageInstructions: string | null;
  storageInstructions: string | null;
  shelfLife: string | null;
  countryOfOrigin: string;
  hsnCode: string | null;
  gstRate: number | null;
  metaTitle: string | null;
  metaDescription: string | null;
  active: boolean;
  featured: boolean;
  minPrice: number | null;
  totalStock: number;
  inStock: boolean;
  variants: Variant[];
  version: number;
  createdAt: string;     // ISO local date-time, IST
  updatedAt: string;
}

export interface VariantRequest {
  id?: number;           // omit for new pack sizes
  quantity: number;
  unit: QuantityUnit;
  price: number;
  mrp?: number;
  stockQuantity: number;
}

export interface ProductRequest {
  name: string;
  categoryId: number;
  slug?: string;
  shortDescription?: string;
  description?: string;
  images?: ProductImage[];
  highlights?: string[];
  ingredients?: string;
  usageInstructions?: string;
  storageInstructions?: string;
  shelfLife?: string;
  countryOfOrigin?: string;
  hsnCode?: string;
  gstRate?: number;
  metaTitle?: string;
  metaDescription?: string;
  active?: boolean;
  featured?: boolean;
  version?: number;      // send on update
  variants: VariantRequest[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ProductListParams {
  categoryId?: number;
  active?: boolean;
  featured?: boolean;
  inStock?: boolean;
  q?: string;
  page?: number;
  size?: number;
  sort?: `${"name" | "category.name" | "createdAt" | "updatedAt"},${"asc" | "desc"}`;
}

export interface ApiProblem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  errors?: Record<string, string>;
}
```

---

## 8. Suggested admin panel screens

1. **Products list** — table: thumbnail, name, category, from-price (`minPrice`), stock (`totalStock`,
   out-of-stock badge), active toggle (PATCH status), featured, updated date, edit/delete actions.
   Toolbar: search box (`q`, debounced), category filter, active / featured / stock filters, sort, pagination.
2. **Create / edit product** — sections:
   - Basic info: name, category (dropdown from `/api/v1/categories`), short description, description
   - Images: upload, preview, reorder, alt text, remove
   - Pack sizes: repeatable rows — ISBN (read-only, shown after save), quantity, unit (from the category's `allowedUnits`), price, MRP,
     stock; show the live discount %
   - Details: highlights (add/remove list), ingredients, usage, storage, shelf life, country of origin
   - Tax: HSN code, GST %
   - SEO: slug (auto from name on create), meta title, meta description, with character counters
   - Visibility: active, featured
   When the category changes, reset each pack size's unit if it isn't in the new `allowedUnits`.
3. **Delete confirmation** dialog.
4. **Categories** — list from `/api/v1/admin/categories`, add/edit dialog (name, measure type, sort order,
   active), delete (offer "set inactive" when it has products).

Validate on the client with the same rules as the tables above for instant feedback, but always handle the
server's 400 responses too — the server is the source of truth (e.g. slug uniqueness).

---

## 9. Quick test with curl

```bash
curl http://localhost:8080/api/v1/categories

curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Rice Murukku","categoryId":2,
       "variants":[{"quantity":250,"unit":"G","price":85,"mrp":100,"stockQuantity":12}]}'

curl "http://localhost:8080/api/v1/admin/products?q=murukku&sort=name,asc"

curl -X POST http://localhost:8080/api/v1/admin/uploads/images -F "file=@photo.jpg"
```
