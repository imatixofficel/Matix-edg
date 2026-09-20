markdown
# Matix Scanner — Cloudflare IP Scanner

**In the name of God**

Find clean, fast Cloudflare IPs — automatically, every 10 minutes.

🌐 **Live Demo:** [https://imatixofficel.github.io/Scanner-matix/](https://imatixofficel.github.io/Scanner-matix/)

---

## What is this?

Matix Scanner is a simple, free tool that finds working Cloudflare IPs for your VLESS, Trojan, or Shadowsocks configs.

It runs quietly in the background every 10 minutes, tests thousands of IPs with real TCP+TLS handshakes and HTTP requests, checks their real download speed, and keeps only the ones that are actually alive and fast.

No fake data. No duplicates. No API keys. Just clean, tested IPs.

---

## Why use it?

- Real testing — not a random list copied from somewhere else
- Always fresh — updated every 10 minutes, 24/7
- Speed-tested — the top 100 IPs are benchmarked for download speed
- Smart tracking — IPs that stay alive over time get marked as persistent
- Zero duplicates — every IP appears only once
- Free forever — no backend, no server, no cost

---

## How to use it

1. Go to 👉 https://imatixofficel.github.io/Scanner-matix/
2. Click Start Scan
3. Pick how many IPs you want
4. Copy the best ones into your config

That's it.

---

## What you get

Every scan gives you a structured list like this:


json
{
  "ip": "104.25.195.64",
  "ms": 82,
  "status": "online",
  "colo": "SEA",
  "speed_mbps": 17.55,
  "persistent": false,
  "online_count": 1
}


· ms — how fast the IP responds
· colo — which Cloudflare edge answered (SEA, FRA, AMS, …)
· speed_mbps — actual download speed
· persistent — whether this IP has been stable over time

---

Persistent IPs

Some IPs keep working for days or weeks. Matix tracks these automatically.

If an IP is seen online 3 or more times in the last 7 days, it gets marked as persistent and highlighted with a diamond in the UI.

Persistent IPs tend to be more stable and more reliable than fresh ones.

---

How it works behind the scenes

Every 10 minutes, Matix:

1. Samples 7,500 IPs from Cloudflare ranges
2. Tests each one with real TCP + TLS
3. Verifies with real HTTP requests
4. Benchmarks speed on the best 100
5. Removes duplicates
6. Tracks persistent IPs
7. Saves the results
8. Publishes them on GitHub Pages

Everything runs on GitHub Actions — completely free.

---

Good to know

· Latency is measured from GitHub servers, not from your own network. Your real ping might be different — always test on your own device if you can.
· If no commit is made for 60 days, GitHub may pause the automatic scans. Just make a small change occasionally to keep it running.
· The repository must stay Public to keep GitHub Actions free.
· No data is collected, no keys are stored, nothing is tracked.

---

Built with

· Python 3.11 — the scanner
· Vanilla JavaScript — the website
· GitHub Actions — the automation
· GitHub Pages — the hosting
· Vazirmatn + Manrope — the fonts

---

Live Demo

👉 https://imatixofficel.github.io/Scanner-matix/

---

Connect with me

· 📺 YouTube: https://youtube.com/@i.matix7
· ✈️ Telegram: https://t.me/Imatix7

---

