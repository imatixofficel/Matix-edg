# به نام خداوند جان و خرد

<div dir="rtl" align="right">

# 🕊️ MatiX Worker

**یک پروژه ایرانی‌پسند، سبک، سریع و قابل توسعه برای Cloudflare Workers**

</div>

<div align="center">

[🇮🇷 فارسی](README_FA.md) · [🇬🇧 English](README.md)

</div>

---

## 📖 درباره پروژه

**MatiX Worker** پروژه‌ای سبک و قابل توسعه برای اجرای سرویس‌های بدون‌سرور روی **Cloudflare Workers** است.

این پروژه می‌تواند برای ساخت API، سرویس‌های وب، ربات‌ها، ابزارهای آنلاین و پروژه‌های وب و موبایل مورد استفاده قرار گیرد.

هدف MatiX این است که راه‌اندازی، تنظیم و توسعه Worker برای کاربران تا جای ممکن ساده و قابل فهم باشد.

---

## ✨ امکانات

- ⚡ اجرای سریع و سبک
- ☁️ اجرا روی Cloudflare Workers
- 🔐 پشتیبانی از Variables و Secrets
- 🗄️ پشتیبانی از Cloudflare KV
- 🔄 امکان اتصال به GitHub
- 📱 مناسب پروژه‌های وب و موبایل
- 🧩 قابلیت توسعه و شخصی‌سازی
- 🌐 معماری بدون‌سرور
- 🇮🇷 مستندات و آموزش فارسی

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

فایل `worker.js` هسته اصلی پروژه است و کد Worker در آن قرار دارد.

---

## 🛠️ آموزش نصب و راه‌اندازی

### ۱. ساخت پوشه پروژه

در ترمینال یک پوشه برای پروژه بسازید:

```bash
mkdir MatiX-Worker
cd MatiX-Worker
```

سپس فایل اصلی را ایجاد کنید:

```text
worker.js
```

کد Worker را داخل این فایل قرار دهید.

### ۲. ساخت Worker در Cloudflare

وارد داشبورد Cloudflare شوید و از مسیر زیر یک Worker جدید بسازید:

```text
Workers & Pages
        ↓
Create
        ↓
Workers
        ↓
Create Worker
```

یک نام برای Worker انتخاب کنید؛ برای نمونه:

```text
matix-worker
```

سپس Worker را ایجاد و Deploy کنید.

### ۳. قرار دادن کد Worker

پس از ساخت Worker، وارد صفحه آن شوید و در بخش ویرایش کد، محتوای `worker.js` را قرار دهید.

سپس گزینه:

```text
Save and Deploy
```

را انتخاب کنید.

پس از Deploy موفق، Cloudflare آدرس Worker را در اختیار شما قرار می‌دهد.

---

## 🔐 تنظیم Variables

برای مقادیر معمولی و غیرمحرمانه می‌توانید از Variables استفاده کنید.

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

برای نمونه:

```text
Variable Name:
API_URL
```

مقدار:

```text
https://example.com/api
```

---

## 🔒 تنظیم Secrets

اطلاعات حساس را هرگز داخل `worker.js` یا Repository عمومی GitHub قرار ندهید.

نمونه اطلاعات حساس:

```text
API_KEY
BOT_TOKEN
SECRET_KEY
PASSWORD
PRIVATE_TOKEN
```

این موارد را از مسیر Variables and Secrets به‌عنوان **Secret** اضافه کنید.

### ⚠️ نکته بسیار مهم

❌ این کار را انجام ندهید:

```js
const TOKEN = "YOUR_SECRET_TOKEN";
```

✅ روش صحیح:

مقدار را در Cloudflare به‌صورت Secret ذخیره کنید و در Worker از محیط `env` دریافت کنید.

---

## 🗄️ ساخت و اتصال Cloudflare KV

اگر پروژه به ذخیره‌سازی داده نیاز دارد، می‌توانید از Cloudflare KV استفاده کنید.

در Cloudflare وارد بخش KV شوید و یک Namespace ایجاد کنید.

برای مثال:

```text
MATIX_KV
```

سپس از قسمت Bindings آن را به Worker متصل کنید.

برای نمونه نام Binding:

```text
KV
```

در کد می‌توانید بنویسید:

```js
await env.KV.put("test", "Hello MatiX");

const value = await env.KV.get("test");
```

**توجه:** نام Binding در Cloudflare باید دقیقاً با نامی که در کد استفاده می‌کنید یکسان باشد.

---

## 🔄 اتصال پروژه به GitHub

برای مدیریت نسخه‌های پروژه می‌توانید یک Repository در GitHub ایجاد کنید.

ساختار پیشنهادی:

```text
MatiX-Worker/
├── worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── wrangler.toml
```

پس از آن می‌توانید Repository را به Cloudflare متصل کنید تا انتشار و مدیریت نسخه‌ها ساده‌تر شود.

---

## 🧭 راه‌اندازی سریع

اگر می‌خواهید سریع پروژه را اجرا کنید:

**۱.** فایل `worker.js` را باز کنید.  
**۲.** Variables و Secrets موردنیاز را در Cloudflare تنظیم کنید.  
**۳.** اگر پروژه به KV نیاز دارد، KV را بسازید و Binding آن را تنظیم کنید.  
**۴.** Worker را Deploy کنید.  
**۵.** آدرس Worker ساخته‌شده را آزمایش کنید.

برای جزئیات بیشتر، مراحل این راهنما را به ترتیب انجام دهید.

---

## 📜 مجوز پروژه

مجوز استفاده از پروژه در فایل [`LICENSE`](LICENSE) قرار دارد.

---

## 📱 ارتباط با MatiX

- ✈️ تلگرام: **@Imatix7**
- 📸 اینستاگرام: **@imatix_**

---

<div align="center">

