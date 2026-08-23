🕋 به نام خدا

MatiX Worker

پروژه MatiX Worker برای اجرای سرویس‌ها و APIها روی Cloudflare Workers ساخته شده است.

---

📌 امکانات

- ⚡ اجرای سریع روی Cloudflare Workers
- ☁️ بدون نیاز به سرور اختصاصی
- 🔐 پشتیبانی از Variables و Secrets
- 🗄️ پشتیبانی از Cloudflare KV
- 🔄 قابلیت اتصال به GitHub
- 📱 مناسب برای پروژه‌های وب و موبایل
- 🧩 قابل توسعه و شخصی‌سازی

---

📁 ساختار پروژه

ساختار پیشنهادی:

MatiX-Worker/
│
├── _worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── .gitignore

فایل اصلی Worker:

_worker.js

---

🛠️ ساخت فایل Worker

اگر می‌خواهید پروژه را از ابتدا بسازید، یک پوشه با نام دلخواه ایجاد کنید.

مثلاً:

mkdir MatiX-Worker
cd MatiX-Worker

سپس فایل زیر را ایجاد کنید:

_worker.js

کد Worker خود را داخل این فایل قرار دهید.

---

☁️ ساخت Worker در Cloudflare

وارد داشبورد Cloudflare شوید.

سپس:

Workers & Pages
        ↓
Create
        ↓
Workers
        ↓
Create Worker

یک نام برای Worker انتخاب کنید.

مثلاً:

matix-worker

بعد Worker را ایجاد و Deploy کنید.

---

📤 آپلود فایل Worker

بعد از ساخت Worker، وارد صفحه Worker شوید.

از قسمت:

Edit Code

فایل:

_worker.js

را قرار دهید.

سپس:

Save and Deploy

را انتخاب کنید.

Cloudflare بعد از Deploy یک آدرس برای Worker ایجاد می‌کند.

مثال:

https://matix-worker.example.workers.dev

---

🔐 تنظیم Variables

برای متغیرهای معمولی می‌توانید از Variables استفاده کنید.

مسیر:

Workers & Pages
        ↓
Worker
        ↓
Settings
        ↓
Variables and Secrets

یک Variable جدید ایجاد کنید.

مثلاً:

API_URL

مقدار:

https://example.com/api

---

🔒 تنظیم Secrets

اطلاعات حساس را داخل "_worker.js" یا GitHub قرار ندهید.

مواردی مانند:

API_KEY
BOT_TOKEN
SECRET_KEY
PASSWORD
PRIVATE_TOKEN

باید به‌عنوان Secret ذخیره شوند.

از مسیر:

Settings
        ↓
Variables and Secrets
        ↓
Add
        ↓
Secret

Secret موردنظر را اضافه کنید.

---

🗄️ ساخت Cloudflare KV

اگر پروژه به KV نیاز دارد:

وارد Cloudflare شوید و به بخش:

Workers & Pages
        ↓
KV

بروید.

سپس:

Create Namespace

را انتخاب کنید.

برای مثال:

MATIX_KV

---

🔗 اتصال KV به Worker

بعد از ساخت Namespace، وارد Worker شوید:

Worker
        ↓
Settings
        ↓
Bindings

یک KV Namespace Binding اضافه کنید.

مثلاً نام Binding را قرار دهید:

KV

و Namespace ساخته‌شده را انتخاب کنید.

در کد Worker می‌توان از آن استفاده کرد:

await env.KV.put("test", "Hello MatiX");

const value = await env.KV.get("test");

«نام "KV" در کد باید با نام Binding در Cloudflare یکسان باشد.»

---

🔄 اتصال GitHub به Cloudflare

برای مدیریت نسخه‌های پروژه، Repository را در GitHub ایجاد کنید.

ساختار Repository:

MatiX-Worker/
├── _worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── .gitignore

سپس Repository را به Cloudflare متصل کنید تا بتوانید پروژه را از GitHub مدیریت و Deploy کنید.

---

⚠️ نکات امنیتی

هرگز اطلاعات حساس را داخل GitHub عمومی قرار ندهید.

❌ اشتباه:

const TOKEN = "YOUR_SECRET_TOKEN";

✅ روش بهتر:

Cloudflare Secrets

و مقدار Secret را از محیط Worker دریافت کنید.

---

📱 ارتباط با MatiX

📲 Telegram

"✈️ @Imatix7" (https://t.me/Imatix7)

📸 Instagram

@imatix_

---

⭐ حمایت

اگر پروژه برای شما مفید بود، Repository را ⭐ Star کنید.

---

<div align="center">🕋 به نام خدا

MatiX

Made with ❤️

</div>
