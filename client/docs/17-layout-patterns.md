# Layout Patterns: Headers, Footers & Navigation

This document covers fundamental CSS layout patterns for app shells, with a focus on flexbox-based layouts commonly used in modern web applications.

---

## Table of Contents

1. [The Sticky Footer Problem](#the-sticky-footer-problem)
2. [Flexbox App Shell](#flexbox-app-shell)
3. [Understanding flex: 1](#understanding-flex-1)
4. [Header Best Practices](#header-best-practices)
5. [Footer Best Practices](#footer-best-practices)
6. [Navigation Buttons](#navigation-buttons)
7. [Alternative Approaches](#alternative-approaches)
8. [Implementation Reference](#implementation-reference)
9. [Centering](#centering)
10. [Containers](#containers)

---

## The Sticky Footer Problem

The classic web layout challenge:

> How do you keep a footer at the bottom of the viewport when content is short, but let it push down naturally when content is long?

### The Problem Visualized

```
SHORT CONTENT (Bad)          SHORT CONTENT (Good)
┌──────────────┐             ┌──────────────┐
│    Header    │             │    Header    │
├──────────────┤             ├──────────────┤
│   Content    │             │              │
├──────────────┤             │   Content    │
│    Footer    │             │              │
│              │             ├──────────────┤
│  (empty)     │             │    Footer    │
└──────────────┘             └──────────────┘
Footer floats mid-page       Footer stays at bottom
```

---

## Flexbox App Shell

The modern solution uses CSS Flexbox with a column direction.

### The Pattern

```css
/* Container: full viewport height, vertical flex */
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Header: fixed/content height */
header {
  /* height determined by content or explicit value */
}

/* Main: grows to fill remaining space */
main {
  flex: 1;
}

/* Footer: fixed/content height */
footer {
  /* height determined by content or explicit value */
}
```

### HTML Structure

```html
<div class="app-shell">
  <header>...</header>
  <main>...</main>
  <footer>...</footer>
</div>
```

### Tailwind CSS Version

```html
<div class="min-h-screen flex flex-col">
  <header>...</header>
  <main class="flex-1">...</main>
  <footer>...</footer>
</div>
```

### Why This Works

1. **`min-h-screen`** (or `min-height: 100vh`): Container is at least viewport height
2. **`flex flex-col`**: Children stack vertically
3. **`flex-1`** on main: Main content absorbs all leftover space
4. Header and Footer keep their natural/fixed heights
5. If content exceeds viewport, page scrolls normally

---

## Understanding flex: 1

The `flex` property is shorthand for three values:

```css
flex: <flex-grow> <flex-shrink> <flex-basis>;
```

### Common Values

| Tailwind | CSS | Meaning |
|----------|-----|---------|
| `flex-1` | `flex: 1 1 0%` | Grow and shrink equally, start from 0 |
| `flex-auto` | `flex: 1 1 auto` | Grow and shrink, start from content size |
| `flex-initial` | `flex: 0 1 auto` | Don't grow, can shrink |
| `flex-none` | `flex: none` | Don't grow or shrink |

### flex-grow Explained

```
Before flex-grow:
┌──────────────────────────────────────┐
│ Header (60px) │ Main │ Footer (48px) │
└──────────────────────────────────────┘
                 ↑
           What about this space?

After flex-grow: 1 on Main:
┌──────────────────────────────────────┐
│ Header │        Main        │ Footer │
│  60px  │   (all the rest)   │  48px  │
└──────────────────────────────────────┘
```

When only one element has `flex-grow: 1`, it takes ALL remaining space.

### flex-basis: 0% vs auto

- **`flex-basis: 0%`** (flex-1): Size calculated from available space only
- **`flex-basis: auto`** (flex-auto): Size starts from content, then grows

For app shells, `flex-1` is preferred because you want main to fill space regardless of content size.

---

## Header Best Practices

### 1. Sticky Positioning

Headers typically stay visible while scrolling:

```css
header {
  position: sticky;
  top: 0;
  z-index: 10; /* Stay above page content */
}
```

```html
<!-- Tailwind -->
<header class="sticky top-0 z-10">
```

### 2. Fixed Height

Consistent header height improves UX:

```css
header {
  height: 64px; /* or 4rem */
}
```

```html
<!-- Tailwind: h-16 = 64px -->
<header class="h-16">
```

### 3. Max-Width Container

Content shouldn't stretch infinitely on wide screens:

```html
<header class="...">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <!-- Header content -->
  </div>
</header>
```

This pattern:
- **`max-w-7xl`**: Limits width to 1280px
- **`mx-auto`**: Centers the container
- **`px-4 sm:px-6 lg:px-8`**: Responsive horizontal padding

### 4. Flex Layout for Header Content

```html
<header>
  <div class="flex justify-between items-center h-16">
    <div>Logo</div>
    <nav>Navigation</nav>
  </div>
</header>
```

- **`flex`**: Horizontal layout
- **`justify-between`**: Logo left, nav right
- **`items-center`**: Vertically centered
- **`h-16`**: Full header height for clickable area

---

## Footer Best Practices

### 1. Mirror Header Styling

Consistency between header and footer creates visual harmony:

```html
<!-- Header -->
<header class="bg-amber-500 border-b-4 border-black">

<!-- Footer (mirrors header) -->
<footer class="bg-amber-500 border-t-4 border-black">
```

### 2. Simple vs Complex Footers

**Simple Footer** (small apps, internal tools):
```html
<footer class="h-12 flex items-center justify-center">
  <span>© 2025 Company Name</span>
</footer>
```

**Complex Footer** (marketing sites, large apps):
```html
<footer class="py-12">
  <div class="max-w-7xl mx-auto grid grid-cols-4 gap-8">
    <div><!-- Column 1: About --></div>
    <div><!-- Column 2: Products --></div>
    <div><!-- Column 3: Resources --></div>
    <div><!-- Column 4: Social --></div>
  </div>
  <div class="border-t mt-8 pt-8 text-center">
    © 2025 Company Name
  </div>
</footer>
```

### 3. Footer Height Considerations

- **Minimal footer**: 48px (`h-12`) - just copyright
- **Standard footer**: 64-80px - copyright + a few links
- **Full footer**: Variable - multiple columns, responsive

### 4. Don't Make Footer Sticky

Unlike headers, footers should NOT be sticky (position: fixed at bottom). This:
- Takes up valuable screen space
- Obscures content
- Is generally poor UX

Exception: Cookie consent banners, which are temporary.

---

## Navigation Buttons

### 1. Clear Active State

Users must know where they are:

```css
/* Inactive */
.nav-link {
  background: white;
  color: black;
}

/* Active */
.nav-link.active {
  background: black;
  color: white;
}
```

With TanStack Router:
```tsx
<Link
  to="/tasks"
  activeProps={{ className: 'bg-black text-white' }}
  inactiveProps={{ className: 'bg-white text-black' }}
>
  Tasks
</Link>
```

### 2. Hover and Focus States

Interactive elements need feedback:

```css
.nav-link {
  transition: transform 0.1s, box-shadow 0.1s;
}

.nav-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.nav-link:active {
  transform: translateY(0);
}
```

### 3. Consistent Sizing

All nav buttons should have uniform dimensions:

```html
<nav class="flex gap-2">
  <a class="px-4 py-2">Home</a>
  <a class="px-4 py-2">Tasks</a>
  <a class="px-4 py-2">Projects</a>
</nav>
```

### 4. Accessibility Considerations

```html
<!-- Use semantic nav element -->
<nav aria-label="Main navigation">
  <!-- Ensure sufficient color contrast (4.5:1 for text) -->
  <!-- Keyboard focus must be visible -->
  <a class="focus:outline-2 focus:outline-offset-2 focus:outline-blue-500">
    Link
  </a>
</nav>
```

---

## Alternative Approaches

### CSS Grid

Grid can also solve the sticky footer:

```css
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
}
```

- **`auto`**: Header/footer take content height
- **`1fr`**: Main takes remaining space

### Older Approaches (Avoid)

**Negative margins** (fragile):
```css
/* DON'T DO THIS */
footer {
  margin-top: -100px;
}
main {
  padding-bottom: 100px;
}
```

**calc() with fixed heights** (inflexible):
```css
/* DON'T DO THIS */
main {
  min-height: calc(100vh - 64px - 48px);
}
```

---

## Implementation Reference

### Files in This Project

| File | Purpose |
|------|---------|
| `src/routes/__root.tsx` | App shell with flex layout |
| `src/components/Header.tsx` | Sticky header with navigation |
| `src/components/Footer.tsx` | Simple copyright footer |
| `src/styles.css` | Theme colors and custom utilities |

### Layout Structure

```tsx
// __root.tsx
<div className="min-h-screen flex flex-col bg-halftone">
  <Header />           {/* sticky top-0, h-16, bg-amber-vivid */}
  <main className="flex-1">
    <Outlet />         {/* Route content renders here */}
  </main>
  <Footer />           {/* h-12, bg-amber-vivid, border-t-4 */}
</div>
```

### Header Implementation

```tsx
// Header.tsx
<header className="bg-amber-vivid border-b-4 border-ink sticky top-0 z-10">
  <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div className="flex justify-between items-center h-16">
      <Link to="/">Logo</Link>
      <nav className="flex gap-2">
        <Link activeProps={{...}} inactiveProps={{...}}>
          Tasks
        </Link>
      </nav>
    </div>
  </div>
</header>
```

### Footer Implementation

```tsx
// Footer.tsx
<footer className="bg-amber-vivid border-t-4 border-ink">
  <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div className="flex justify-center items-center h-12">
      <span className="text-display text-sm text-ink">
        © 2025 Task Manager. All rights reserved.
      </span>
    </div>
  </div>
</footer>
```

---

## Centering

Centering is one of the most common CSS tasks. Here are the standard approaches.

### Horizontal Centering

**1. Auto Margins (block elements)**

The classic approach for centering block elements with a fixed/max width:

```css
.centered {
  max-width: 768px;
  margin-left: auto;
  margin-right: auto;
}
```

```html
<!-- Tailwind -->
<div class="max-w-3xl mx-auto">Centered content</div>
```

Best for: Containers, cards, content sections.

**2. Flexbox**

```css
.parent {
  display: flex;
  justify-content: center;
}
```

```html
<!-- Tailwind -->
<div class="flex justify-center">
  <div>Centered child</div>
</div>
```

Best for: Centering within a flex container, single items.

**3. Text Align (inline/text content)**

```css
.parent {
  text-align: center;
}
```

```html
<!-- Tailwind -->
<div class="text-center">Centered text</div>
```

Best for: Text, inline elements, inline-block elements.

### Vertical Centering

**1. Flexbox (recommended)**

```css
.parent {
  display: flex;
  align-items: center;
  min-height: 100vh; /* or fixed height */
}
```

```html
<!-- Tailwind -->
<div class="flex items-center min-h-screen">
  <div>Vertically centered</div>
</div>
```

**2. Grid**

```css
.parent {
  display: grid;
  align-items: center;
  min-height: 100vh;
}
```

```html
<!-- Tailwind -->
<div class="grid items-center min-h-screen">
  <div>Vertically centered</div>
</div>
```

### Both Horizontal & Vertical (Dead Center)

**1. Flexbox (most common)**

```css
.parent {
  display: flex;
  justify-content: center;  /* horizontal */
  align-items: center;      /* vertical */
  min-height: 100vh;
}
```

```html
<!-- Tailwind -->
<div class="flex justify-center items-center min-h-screen">
  <div>Dead center</div>
</div>
```

**2. Grid (cleanest)**

```css
.parent {
  display: grid;
  place-items: center;  /* shorthand for both */
  min-height: 100vh;
}
```

```html
<!-- Tailwind -->
<div class="grid place-items-center min-h-screen">
  <div>Dead center</div>
</div>
```

**3. Absolute Positioning + Transform (legacy)**

```css
.parent {
  position: relative;
}
.child {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
```

```html
<!-- Tailwind -->
<div class="relative h-screen">
  <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
    Dead center
  </div>
</div>
```

Best for: Overlays, modals, legacy browser support.

### Quick Reference

| Goal | Tailwind Classes |
|------|-----------------|
| Center block horizontally | `mx-auto` (needs max-width) |
| Center text | `text-center` |
| Center flex children horizontally | `flex justify-center` |
| Center flex children vertically | `flex items-center` |
| Dead center (flex) | `flex justify-center items-center` |
| Dead center (grid) | `grid place-items-center` |
| Center single grid child | `place-self-center` |

### Common Patterns

**Centered container with max-width:**
```html
<div class="max-w-3xl mx-auto">
  <!-- Content stays at 768px and centers -->
</div>
```

**Centered content within full-width section:**
```html
<section class="bg-blue-500 py-12">
  <div class="max-w-7xl mx-auto px-4">
    <!-- Content is constrained and centered, background is full-width -->
  </div>
</section>
```

**Vertically centered page content:**
```html
<main class="min-h-screen flex items-center">
  <div class="max-w-md mx-auto">
    <!-- Login form, error page, etc. -->
  </div>
</main>
```

**Centered button group:**
```html
<div class="flex justify-center gap-4">
  <button>Cancel</button>
  <button>Save</button>
</div>
```

### Avoid These Anti-Patterns

```html
<!-- ❌ Don't use fixed widths with margin auto -->
<div class="w-[500px] mx-auto">...</div>

<!-- ✅ Use max-width instead (responsive) -->
<div class="max-w-lg mx-auto">...</div>
```

```html
<!-- ❌ Don't use absolute positioning for simple centering -->
<div class="relative">
  <div class="absolute left-1/2 -translate-x-1/2">...</div>
</div>

<!-- ✅ Use flexbox (simpler, more robust) -->
<div class="flex justify-center">
  <div>...</div>
</div>
```

```html
<!-- ❌ Don't nest unnecessary flex containers -->
<div class="flex justify-center">
  <div class="flex justify-center">
    <div>...</div>
  </div>
</div>

<!-- ✅ One flex parent is enough -->
<div class="flex justify-center">
  <div>...</div>
</div>
```

---

## Containers

Containers limit content width for readability and visual alignment.

### The Container Pattern

```html
<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
  <!-- Content -->
</div>
```

| Class | CSS | Purpose |
|-------|-----|---------|
| `max-w-7xl` | `max-width: 1280px` | Limits width for readability |
| `mx-auto` | `margin: 0 auto` | Centers horizontally |
| `px-4` | `padding: 0 1rem` | Mobile padding (16px) |
| `sm:px-6` | `padding: 0 1.5rem` | Tablet padding (24px) |
| `lg:px-8` | `padding: 0 2rem` | Desktop padding (32px) |

### Why Containers Matter

**Without container** (ultrawide monitor):
```
┌──────────────────────────────────────────────────────────────────────────┐
│ Text stretches across 2560px making it extremely difficult to read       │
│ because eyes must travel too far from line end to next line start...     │
└──────────────────────────────────────────────────────────────────────────┘
```

**With container**:
```
┌──────────────────────────────────────────────────────────────────────────┐
│              ┌────────────────────────────────────────┐                  │
│              │ Text stays within comfortable width    │                  │
│              │ of ~65-80 characters per line.         │                  │
│              └────────────────────────────────────────┘                  │
└──────────────────────────────────────────────────────────────────────────┘
```

### Common Max-Width Values

| Tailwind | Pixels | Use Case |
|----------|--------|----------|
| `max-w-sm` | 384px | Narrow cards, login forms |
| `max-w-md` | 448px | Modals, small forms |
| `max-w-lg` | 512px | Content cards |
| `max-w-xl` | 576px | Wide cards |
| `max-w-2xl` | 672px | Article content |
| `max-w-3xl` | 768px | Detail pages, forms |
| `max-w-4xl` | 896px | Landing sections |
| `max-w-5xl` | 1024px | Wide content |
| `max-w-6xl` | 1152px | Dashboard content |
| `max-w-7xl` | 1280px | App shell (header/footer) |

### Container Strategies

**Strategy 1: Full-Width Lists**
Grid content stretches to fill. Good for dashboards with many items.
```html
<div class="p-6">
  <div class="grid grid-cols-3 gap-6">...</div>
</div>
```

**Strategy 2: Constrained Content**
Reading content has max-width. Good for articles, forms, detail views.
```html
<div class="p-6 max-w-3xl mx-auto">
  <article>...</article>
</div>
```

**Strategy 3: Consistent App Container** (Recommended)
All pages match header/footer width for visual alignment.
```html
<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
  <!-- Page content aligns with header/footer -->
</div>
```

### Nested Containers

For content that needs narrower width than the app container:

```html
<!-- Outer: matches header/footer -->
<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">

  <!-- Inner: narrower for forms -->
  <div class="max-w-3xl">
    <form>...</form>
  </div>

</div>
```

---

## Summary

| Concept | Key Takeaway |
|---------|--------------|
| Sticky Footer | Use `min-h-screen flex flex-col` + `flex-1` on main |
| flex: 1 | Shorthand for "grow to fill available space" |
| Header | Sticky, fixed height, max-width container |
| Footer | Mirror header style, NOT sticky, simple is fine |
| Nav Buttons | Clear active state, hover feedback, consistent size |
| Containers | `max-w-7xl mx-auto px-4 sm:px-6 lg:px-8` for app shell |
| Responsive Padding | `px-4 sm:px-6 lg:px-8` scales with screen size |
| Horizontal Center | `mx-auto` (block) or `flex justify-center` |
| Vertical Center | `flex items-center` (needs height) |
| Dead Center | `grid place-items-center` or `flex justify-center items-center` |

The flexbox approach is the modern industry standard because it's:
- **Simple**: Just 3 CSS properties
- **Robust**: Works with any content length
- **Flexible**: Easy to add sidebars, nested layouts
- **Responsive**: No magic numbers or calculations
