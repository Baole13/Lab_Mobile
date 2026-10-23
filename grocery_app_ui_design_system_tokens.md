# Grocery App UI Design System & Code Tokens

> Context document for AI coding assistants (Codex, Cursor, Copilot) and frontend developers.
> Baseline viewport: Mobile 375pt x 812pt (iOS/Android).

---

## 1. Design Tokens (JSON Schema)

```json
{
  "theme": {
    "colors": {
      "primary": {
        "50": "#ECFDF5",
        "100": "#D1FAE5",
        "500": "#10B981",
        "600": "#059669",
        "700": "#047857"
      },
      "secondary": {
        "400": "#FBBF24",
        "500": "#F59E0B",
        "600": "#D97706"
      },
      "surface": {
        "light": {
          "background": "#F8FAFC",
          "card": "#FFFFFF",
          "cardMuted": "#F1F5F9",
          "border": "#E2E8F0"
        },
        "dark": {
          "background": "#0F172A",
          "card": "#1E293B",
          "cardMuted": "#334155",
          "border": "#334155"
        }
      },
      "text": {
        "light": {
          "primary": "#0F172A",
          "secondary": "#64748B",
          "muted": "#94A3B8",
          "inverse": "#FFFFFF"
        },
        "dark": {
          "primary": "#F8FAFC",
          "secondary": "#94A3B8",
          "muted": "#64748B",
          "inverse": "#0F172A"
        }
      },
      "status": {
        "discount": "#EF4444",
        "success": "#10B981",
        "rating": "#F59E0B"
      }
    },
    "typography": {
      "fontFamily": "Poppins, Plus Jakarta Sans, system-ui, sans-serif",
      "scale": {
        "hero": { "size": "26px", "lineHeight": "32px", "weight": "700" },
        "h1": { "size": "20px", "lineHeight": "28px", "weight": "600" },
        "h2": { "size": "16px", "lineHeight": "24px", "weight": "600" },
        "body": { "size": "14px", "lineHeight": "20px", "weight": "400" },
        "bodyMedium": { "size": "14px", "lineHeight": "20px", "weight": "500" },
        "caption": { "size": "12px", "lineHeight": "16px", "weight": "400" },
        "badge": { "size": "11px", "lineHeight": "14px", "weight": "600" }
      }
    },
    "spacing": {
      "xs": "4px",
      "sm": "8px",
      "md": "12px",
      "lg": "16px",
      "xl": "20px",
      "2xl": "24px",
      "3xl": "32px"
    },
    "borderRadius": {
      "sm": "8px",
      "md": "12px",
      "lg": "16px",
      "xl": "20px",
      "full": "9999px"
    },
    "elevation": {
      "soft": "0 4px 16px -2px rgba(0, 0, 0, 0.04)",
      "card": "0 8px 24px -4px rgba(15, 23, 42, 0.06)",
      "floating": "0 12px 32px -4px rgba(16, 185, 129, 0.25)"
    }
  }
}
```

---

## 2. Layout & Grid Rules

1. **Global Margins & Padding:**
   * Container horizontal padding: `px-4` (16px) or `px-5` (20px).
   * Safe area bottom handling: Always apply `pb-safe` or minimum `pb-6` for floating bottom bars.
2. **Product Grid:**
   * Display: 2-column flex or CSS Grid (`grid grid-cols-2 gap-3.5`).
   * Aspect ratio for product thumbnails: 1:1 square or 4:3 with transparent background PNGs.
3. **Horizontal Category Bar:**
   * Single-line horizontal scroll (`overflow-x-auto no-scrollbar`).
   * Space between category chips: `gap-2.5` (10px).

---

## 3. Core Component Guidelines

### A. Product Card (`ProductCard`)
* **Container:** 
  * Background: `#FFFFFF` (Light) / `#1E293B` (Dark).
  * Border radius: `rounded-2xl` (16px - 20px).
  * Padding: `p-3.5`.
  * Shadow: Soft elevation or subtle border `border border-slate-100`.
* **Elements:**
  1. **Badge (Optional):** Absolute top-left `Discount` tag (e.g., `-20%`), red bg (`#FEE2E2`), text red (`#DC2626`), `rounded-lg px-2 py-0.5 text-xs font-semibold`.
  2. **Image Container:** Centered, height `h-28` to `h-32`, clean transparent background.
  3. **Product Name:** `text-sm font-semibold text-slate-800 line-clamp-1`.
  4. **Unit/Weight:** `text-xs text-slate-400 mt-0.5` (e.g., `1 kg`, `500g`, `1 piece`).
  5. **Price Row:**
     * Left side: `text-base font-bold text-emerald-600` alongside optional strikethrough original price.
     * Right side: **Quick Add Button (+)**: `w-8 h-8 rounded-xl bg-emerald-500 text-white flex items-center justify-center active:scale-95 transition-transform`.

### B. Category Chip (`CategoryItem`)
* **Inactive state:** Light grey / muted card surface, muted text.
* **Active state:** Subtle emerald tint background (`bg-emerald-50 text-emerald-700 border border-emerald-200`) or primary solid.
* **Icon:** 24x24px colored SVG or vector illustration.

### C. Search & Filter Bar (`SearchBar`)
* **Height:** `h-12` (48px).
* **Border Radius:** `rounded-2xl`.
* **Style:** Neutral surface (`bg-slate-100` in light mode), prefix magnifying glass icon (`text-slate-400`), suffix filter icon button with primary or neutral background.
* **Touch Target:** Minimum `48px` height for effortless single-hand tap.

### D. Quantity Stepper (`QuantityStepper`)
* Used in Cart item and Product detail:
  * Layout: `flex items-center space-x-3 bg-slate-100 rounded-xl p-1`.
  * Buttons: Minus `–` and Plus `+`, minimum touch area `32x32px`, rounded surface.
  * Count Display: `min-w-[24px] text-center font-semibold text-sm`.
  * Behavior: If quantity = 1, minus icon changes to a Trash can icon (`text-red-500`).

### E. Sticky Bottom Bar (`CheckoutBar`)
* **Position:** `fixed bottom-0 left-0 right-0 z-50`.
* **Background:** Card surface with blurred backdrop (`backdrop-blur-md bg-white/90 border-t border-slate-100`).
* **Content:**
  * Left: Total label + large total amount (`text-lg font-bold text-slate-900`).
  * Right: CTA Button ("Checkout" / "Add to Cart"), `bg-emerald-500 text-white font-semibold py-3.5 px-6 rounded-2xl shadow-lg shadow-emerald-500/20 active:bg-emerald-600`.

---

## 4. Tailwind CSS Utility Reference

When generating HTML/React/Vue components, map tokens as follows:

| Design Element | Recommended Tailwind Classes |
| :--- | :--- |
| **Primary Action Button** | `bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-semibold rounded-2xl py-3.5 px-5 shadow-sm transition` |
| **Product Card** | `bg-white dark:bg-slate-800 rounded-2xl p-3.5 shadow-sm border border-slate-100 dark:border-slate-700/50 flex flex-col justify-between` |
| **Add Button (+)** | `w-8 h-8 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white flex items-center justify-center transition-all shadow-sm` |
| **Input / Search** | `w-full h-12 bg-slate-100 dark:bg-slate-800 rounded-2xl px-4 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500/20` |
| **Price Tag** | `text-base font-bold text-emerald-600 dark:text-emerald-400` |
| **Unit Text** | `text-xs font-normal text-slate-400 dark:text-slate-500` |

---

## 5. Rules for Codex When Generating Code

1. **Accessibility & Tap Targets:** All touchable icons/buttons (`+`, `-`, back button, bookmark) MUST have a hit-box area of at least `36x36px` (preferably `44x44px` with padding).
2. **Text Truncation:** Product titles inside lists/cards must specify `line-clamp-1` or `line-clamp-2` to prevent layout breaking.
3. **Number Formatting:** Always format currency and prices with thousand separators and unit suffix (e.g., `$4.99 / kg` or `45.000đ / kg`).
4. **Imagery:** Always supply a fallback solid color placeholder with an icon when product image fails to load.