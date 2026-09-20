```markdown
<h1 align="center">In the name of God</h1>

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=2,12,20&height=200&section=header&text=MatiX%20Worker&fontSize=70&fontColor=ffffff&animation=fadeIn" width="100%"/>

# 🕊️ MatiX Worker

### A lightweight, fast, and extensible project for Cloudflare Workers

[![Telegram](https://img.shields.io/badge/Telegram-Imatix7-blue?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/Imatix7)
[![Instagram](https://img.shields.io/badge/Instagram-imatix_-E4405F?style=for-the-badge&logo=instagram&logoColor=white)](https://instagram.com/imatix_)
[![GitHub](https://img.shields.io/badge/GitHub-imatixofficel-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/imatixofficel)
[![Wizard](https://img.shields.io/badge/🧙_MatiX_Wizard-Quick_Setup-28a745?style=for-the-badge)](https://matix-wizard.imatixofficel.workers.dev/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

🇬🇧 **English** · 🇮🇷 [فارسی](README_FA.md)

</div>

---

## 📖 About

<div align="center">

<table>
<tr>
<td align="center" style="background-color: #d4edda; border: 2px solid #28a745; border-radius: 10px; padding: 20px; color: #155724;">

**MatiX Worker** is an open-source, lightweight project for running serverless services on Cloudflare Workers.

It can be used to build APIs, web services, bots, online tools, and web or mobile projects.

Goal: make setting up and managing a Worker as **simple, understandable, and customizable** as possible.

</td>
</tr>
</table>

</div>

---

## ✨ Features

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

> ⚡ **Fast and lightweight** — no heavy dependencies
>
> ☁️ **Runs on Cloudflare Workers** — serverless, no setup
>
> 🔐 **Supports Variables and Secrets** — for sensitive data
>
> 🗄️ **Supports Cloudflare KV** — for data storage
>
> 🔄 **Ready for GitHub integration** — version control
>
> 📱 **Suitable for web and mobile** — cross-platform
>
> 🧩 **Extensible and customizable** — full freedom
>
> 🌐 **Serverless architecture** — scalable and economical
>
> 🧙 **Automated setup via MatiX Wizard** — just one token

</td>
</tr>
</table>

</div>

---

## 🧙 Automated Setup with MatiX Wizard

<div align="center">

<table>
<tr>
<td align="center" style="background-color: #d4edda; border: 2px solid #28a745; border-radius: 10px; padding: 20px; color: #155724;">

The easiest way to create and set up the panel is using **MatiX Wizard**.

All you need is a Cloudflare API token — the rest is automated.

<br/>

<a href="https://matix-wizard.imatixofficel.workers.dev/">
<img src="https://img.shields.io/badge/🚀_Start_Setup-28a745?style=for-the-badge&logoColor=white" alt="Start Wizard"/>
</a>

</td>
</tr>
</table>

</div>

### ✅ Prerequisites

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

> 1. **Cloudflare account** (free)
>
> 2. **Cloudflare API Token** with these permissions:
>    - `Workers Scripts: Edit`
>    - `Workers KV Storage: Edit`
>    - `Account Settings: Read`

</td>
</tr>
</table>

</div>

### 🔑 Creating a Cloudflare Token

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

> 1. Open: https://dash.cloudflare.com/profile/api-tokens
>
> 2. Click **Create Token**.
>
> 3. Choose **Edit Cloudflare Workers** template.
>
> 4. Ensure these permissions:
>    - Account → Workers Scripts → **Edit**
>    - Account → Workers KV Storage → **Edit**
>    - Account → Account Settings → **Read**
>
> 5. Select your account in **Account Resources**.
>
> 6. Click **Continue to summary** → **Create Token**.
>
> 7. **Copy** the token and store it safely.
>
> ⚠️ This token is shown only once.

</td>
</tr>
</table>

</div>

### 🚀 Setup Steps

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

> **Step 1:** Open the Wizard URL.
>
> **Step 2:** Paste your Cloudflare token.
>
> **Step 3:** Click **Start**.
>
> **Step 4:** The Wizard will automatically:
>
> - ✅ Create the required **KV Namespace**
> - ✅ Create the **Worker** for the panel
> - ✅ Set up **Variables** and **Secrets** (including `ADMIN` and `UUID`)
> - ✅ Bind KV to the Worker
> - ✅ Perform the final **Deploy**
>
> **Step 5:** The Wizard shows your panel URL:
>
```

https://matix-worker.YOURNAME.workers.dev

```

**Step 6:** Log in:

```

https://matix-worker.YOURNAME.workers.dev/login

```

Password = whatever you set as `ADMIN`.

</td>
</tr>
</table>

</div>

📊 Setup Report

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

The Wizard reports each step as it runs, showing what has been done and what is currently in progress. If an error occurs, the reason is displayed.

</td>
</tr>
</table>

</div>

⚠️ Security Notes

<div align="center">

<table>
<tr>
<td style="background-color: #fff3cd; border-left: 5px solid #ffc107; padding: 12px 18px; color: #856404; text-align: left;">

· The Cloudflare token is used only during setup and is not stored.
· After setup, you can Revoke the token from the Cloudflare dashboard.
· Never commit the token to a public place (such as GitHub).

</td>
</tr>
</table>

</div>

---

📁 Project Structure

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

```
MatiX-Worker/
├── worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── wrangler.toml
```

</td>
</tr>
</table>

</div>

📄 Main File

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

The worker.js file is the core of the Worker, containing the project's execution logic.

</td>
</tr>
</table>

</div>

---

🛠️ Manual Setup (Without Wizard)

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

If you prefer to set up manually, follow the steps below.

</td>
</tr>
</table>

</div>

1. Create the Project Folder

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

```bash
mkdir MatiX-Worker
cd MatiX-Worker
```

Create the main file:

```
worker.js
```

Then paste the Worker code into it.

</td>
</tr>
</table>

</div>

2. Create the Worker in Cloudflare

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

Go to the Cloudflare dashboard and create a Worker from:

```
Workers & Pages
        ↓
Create
        ↓
Workers
        ↓
Create Worker
```

Pick a name for the project; for example:

```
matix-worker
```

Then create and Deploy the Worker.

</td>
</tr>
</table>

</div>

3. Add the Code

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

Open the Worker and paste the contents of worker.js into the code editor.

Then choose:

```
Save and Deploy
```

After a successful Deploy, Cloudflare gives you a URL for the Worker.

</td>
</tr>
</table>

</div>

---

🔐 Variables and Secrets

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

For non-sensitive settings, use Variables.

Typical path:

```
Workers & Pages
        ↓
Worker
        ↓
Settings
        ↓
Variables and Secrets
```

For sensitive information such as the following, use Secrets:

· API_KEY
· BOT_TOKEN
· SECRET_KEY
· PASSWORD
· PRIVATE_TOKEN
· ADMIN
· UUID

</td>
</tr>
</table>

</div>

⚠️ Security Note

<div align="center">

<table>
<tr>
<td style="background-color: #f8d7da; border-left: 5px solid #dc3545; padding: 12px 18px; color: #721c24; text-align: left;">

❌ Wrong: Never put secrets inside worker.js or a public repository.

```javascript
const TOKEN = "YOUR_SECRET_TOKEN";   // ❌
```

✅ Correct: Store sensitive values as Secrets in Cloudflare and read them from the Worker environment.

</td>
</tr>
</table>

</div>

---

🗄️ Using Cloudflare KV

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

To store data needed by the Worker, you can use Cloudflare KV.

Create a KV Namespace in Cloudflare and bind it to the Worker under Bindings.

For example, name the Binding:

```
KV
```

In code:

```javascript
await env.KV.put("test", "Hello MatiX");
const value = await env.KV.get("test");
```

Note: The Binding name in Cloudflare must exactly match the name used in the code.

</td>
</tr>
</table>

</div>

---

🔄 GitHub Integration

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

Create a repository on GitHub and place the project files in it.

Once the repository is connected to Cloudflare, you can use GitHub to manage versions and publish changes.

</td>
</tr>
</table>

</div>

---

🧭 Quick Start

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

If you just want to explore the project:

1. Open worker.js.
2. Set the required Variables and Secrets in Cloudflare.
3. Bind KV if needed.
4. Deploy the Worker.
5. Test the URL generated by Cloudflare.

</td>
</tr>
</table>

</div>

---

🆘 Wizard Troubleshooting

<div align="center">

<table>
<tr>
<td style="background-color: #d4edda; border-left: 5px solid #28a745; padding: 12px 18px; color: #155724; text-align: left;">

"Invalid API Token" error

· Make sure you pasted the token completely, without spaces.
· Verify that the required permissions are enabled on the token.
· If the token has expired, create a new one.

"KV Namespace already exists" error

· A KV with the same name already exists. You can delete it from the Cloudflare dashboard or choose a different name.

"Worker name already taken" error

· The Worker name is already in use. Enter a different name in the Wizard.

Panel doesn't come up after Deploy

· Wait a few minutes; Worker propagation can take a moment.
· Clear your browser cache (Ctrl + F5).
· Make sure the KV binding is attached with the exact name KV.

</td>
</tr>
</table>

</div>

---

📜 License

<div align="center">

<table>
<tr>
<td align="center" style="background-color: #d4edda; border: 2px solid #28a745; border-radius: 10px; padding: 15px; color: #155724;">

This project is released under the MIT License. See LICENSE for details.

</td>
</tr>
</table>

</div>

---

📱 Connect with MatiX

<div align="center">

<a href="https://t.me/Imatix7">
<img src="https://img.shields.io/badge/Telegram-@Imatix7-26A5E4?style=for-the-badge&logo=telegram&logoColor=white"/>
</a>
<a href="https://instagram.com/imatix_">
<img src="https://img.shields.io/badge/Instagram-@imatix_-E4405F?style=for-the-badge&logo=instagram&logoColor=white"/>
</a>
<a href="https://github.com/imatixofficel">
<img src="https://img.shields.io/badge/GitHub-imatixofficel-181717?style=for-the-badge&logo=github&logoColor=white"/>
</a>

</div>

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=2,12,20&height=120&section=footer" width="100%"/>
