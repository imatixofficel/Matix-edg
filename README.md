# Matix Edge — Cloudflare Workers Panel

**In the name of Allah**

A lightweight, fast, and easy-to-deploy panel for Cloudflare Workers.

🌐 **Wizard:** [https://matix-wizard.imatixofficel.workers.dev/](https://matix-wizard.imatixofficel.workers.dev/)

🌐 **BOT TELEGRAM** [[https://t.me/IMatixedgebot](https://t.me/IMatixedgebot)]

---

## What is this?

Matix Edge is a simple, free panel that runs entirely on Cloudflare Workers.

It gives you a clean dashboard to manage your own VLESS / Trojan / Shadowsocks subscription, generate config links, check usage, and control everything from one place.

No server, no VPS, no cost. Just deploy once and use it.

---

## Why use it?

- Fast — runs on Cloudflare's global edge network
- Free — no server, no cost, no maintenance
- Easy setup — one-click deploy with the Matix Wizard
- Clean dashboard — bilingual (Persian + English), dark & light theme
- Smart subscription — automatically pulls clean IPs from the Matix Scanner
- Built-in Telegram bot — get your config, check status, reset settings
- Self-update — update the panel from GitHub with one click
- Usage tracking — see your Worker requests and quota

---

## How to set it up

The easiest way is using the Matix Wizard:

1. Go to 👉 https://matix-wizard.imatixofficel.workers.dev/
2. Paste your Cloudflare API Token
3. Click Start

The Wizard will automatically:

1. Create a KV Namespace
2. Create the Worker
3. Set up Variables and Secrets (ADMIN, UUID)
4. Bind KV to the Worker
5. Deploy everything

Once it's done, the Wizard shows you your panel URL:



https://matix-worker.YOURNAME.workers.dev



Log in at:



https://matix-worker.YOURNAME.workers.dev/login



The password is whatever you set as `ADMIN`.

---

## What you get

A full management dashboard with:

- 🔗 Subscription link
- 🔗 Single node link
- ⚙️ Protocol & transport settings
- 📊 Usage chart
- ⏳ Subscription limits (days / GB)
- 🌐 Preferred IP source
- 🛡️ Proxy settings (SOCKS5 / HTTP / HTTPS)
- 📋 Custom IP list
- 🤖 Telegram bot activation
- 🚀 Self-update from GitHub
- 🧾 Recent logs

---

## How it works behind the scenes

Matix Edge runs entirely on Cloudflare Workers:

1. The Worker handles all incoming requests
2. Settings are stored in Cloudflare KV
3. Clean IPs are pulled from the Matix Scanner
4. Config links are generated on the fly
5. Telegram bot runs on a webhook
6. Updates pull directly from GitHub Releases

Everything is serverless and free.

---

## Good to know

- The panel runs on Cloudflare's free tier for normal usage.
- The Cloudflare API token is used only during setup and is not stored.
- After setup, you can revoke the token from the Cloudflare dashboard.
- No data is collected, no keys are stored, nothing is tracked.

---

## Built with

- JavaScript — the Worker
- Cloudflare Workers — the runtime
- Cloudflare KV — the storage
- GitHub Pages / Releases — distribution
- Vazirmatn + Manrope — the fonts

---

## Wizard

👉 https://matix-wizard.imatixofficel.workers.dev/

---

## Connect with me

- 📺 YouTube: https://youtube.com/@i.matix7
- ✈️ Telegram: https://t.me/Imatix7

---
