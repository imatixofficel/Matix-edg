# به نام خداوند جان و خرد

<div align="center">

# 🕊️ MatiX Worker

**یک پروژه سبک، سریع و قابل توسعه برای Cloudflare Workers**

[🇮🇷 فارسی](README_FA.md) · [🇬🇧 English](README.md)

</div>

---

## 📖 معرفی پروژه

**MatiX Worker** یک پروژه متن‌باز و سبک برای اجرای سرویس‌های بدون‌سرور (Serverless) روی **Cloudflare Workers** است.

این پروژه می‌تواند برای ساخت API، سرویس‌های وب، ربات‌ها، ابزارهای آنلاین و پروژه‌های وب و موبایل استفاده شود.

هدف پروژه این است که راه‌اندازی و مدیریت یک Worker تا حد ممکن ساده، قابل فهم و قابل شخصی‌سازی باشد.

---

## ✨ امکانات

- ⚡ سریع و سبک
- ☁️ اجرا روی Cloudflare Workers
- 🔐 پشتیبانی از Variables و Secrets
- 🗄️ پشتیبانی از Cloudflare KV
- 🔄 مناسب برای اتصال به GitHub
- 📱 مناسب پروژه‌های وب و موبایل
- 🧩 قابل توسعه و شخصی‌سازی
- 🌐 معماری Serverless
- 🇮🇷 مستندات فارسی

---

## 📁 ساختار پروژه

```text
MatiX-Worker/
├── worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── wrangler.toml
```

### 📄 فایل اصلی

فایل `worker.js` هسته اصلی Worker است و منطق اجرای پروژه در آن قرار می‌گیرد.

---

## 🛠️ آموزش راه‌اندازی

### 1. ساخت پوشه پروژه

```bash
mkdir MatiX-Worker
cd MatiX-Worker
```

فایل اصلی را ایجاد کنید:

```text
worker.js
```

سپس کد Worker را داخل آن قرار دهید.

### 2. ساخت Worker در Cloudflare

وارد داشبورد Cloudflare شوید و از مسیر زیر Worker خود را بسازید:

```text
Workers & Pages
        ↓
Create
        ↓
Workers
        ↓
Create Worker
```

یک نام برای پروژه انتخاب کنید؛ برای نمونه:

```text
matix-worker
```

سپس Worker را ایجاد و Deploy کنید.

### 3. قرار دادن کد

وارد Worker شوید و در بخش ویرایش کد، محتوای `worker.js` را قرار دهید.

پس از آن:

```text
Save and Deploy
```

را انتخاب کنید.

بعد از Deploy موفق، Cloudflare یک آدرس برای Worker در اختیار شما قرار می‌دهد.

---

## 🔐 Variables و Secrets

برای تنظیمات غیرحساس می‌توانید از **Variables** استفاده کنید.

مسیر معمول:

```text
Workers & Pages
        ↓
Worker
        ↓
Settings
        ↓
Variables and Secrets
```

برای اطلاعات حساس مانند موارد زیر از **Secrets** استفاده کنید:

```text
API_KEY
BOT_TOKEN
SECRET_KEY
PASSWORD
PRIVATE_TOKEN
```

### ⚠️ نکته امنیتی

اطلاعات محرمانه را مستقیماً داخل `worker.js` یا Repository عمومی GitHub قرار ندهید.

❌ نادرست:

```js
const TOKEN = "YOUR_SECRET_TOKEN";
```

✅ درست:

مقدار حساس را به‌عنوان Secret در Cloudflare ذخیره کنید و از محیط Worker دریافت کنید.

---

## 🗄️ استفاده از Cloudflare KV

برای ذخیره داده‌های موردنیاز Worker می‌توانید از Cloudflare KV استفاده کنید.

از Cloudflare یک KV Namespace بسازید و سپس آن را از بخش Bindings به Worker متصل کنید.

برای نمونه نام Binding را قرار دهید:

```text
KV
```

در کد:

```js
await env.KV.put("test", "Hello MatiX");

const value = await env.KV.get("test");
```

**توجه:** نام Binding در Cloudflare باید دقیقاً با نام استفاده‌شده در کد یکسان باشد.

---

## 🔄 اتصال به GitHub

Repository پروژه را در GitHub ایجاد کنید و فایل‌های پروژه را در آن قرار دهید.

پس از اتصال Repository به Cloudflare می‌توانید از GitHub برای مدیریت نسخه‌ها و انتشار تغییرات استفاده کنید.

---

## 🧭 شروع سریع

اگر فقط می‌خواهید پروژه را بررسی کنید:

1. فایل `worker.js` را باز کنید.
2. متغیرها و Secrets موردنیاز را در Cloudflare تنظیم کنید.
3. در صورت نیاز KV را متصل کنید.
4. Worker را Deploy کنید.
5. آدرس ایجادشده توسط Cloudflare را آزمایش کنید.

برای آموزش کامل‌تر، بخش‌های همین README را مرحله‌به‌مرحله دنبال کنید.

---

## 📜 مجوز

این پروژه تحت مجوز موجود در فایل [`LICENSE`](LICENSE) منتشر شده است.

---

## 📱 ارتباط با MatiX

- ✈️ Telegram: **@Imatix7**
- 📸 Instagram: **@imatix_**

---

<div align="center">

### 🕋 به نام خداوند جان و خرد

**MatiX**

ساخته‌شده با ❤️

</div>
