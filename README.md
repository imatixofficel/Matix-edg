```markdown
# 🕊️ MatiX Worker

> **In the name of God**

A lightweight, fast, and extensible project for Cloudflare Workers.

🇬🇧 **English** · 🇮🇷 [فارسی](README_FA.md)

---

## 📖 About

MatiX Worker is an open-source, lightweight project for running serverless services on Cloudflare Workers.

It can be used to build APIs, web services, bots, online tools, and web or mobile projects.

The goal is to make setting up and managing a Worker as **simple, understandable, and customizable** as possible.

---

## ✨ Features

- ⚡ **Fast and lightweight** — no heavy dependencies
- ☁️ **Runs on Cloudflare Workers** — serverless, no setup
- 🔐 **Supports Variables and Secrets** — for sensitive data
- 🗄️ **Supports Cloudflare KV** — for data storage
- 🔄 **Ready for GitHub integration** — version control
- 📱 **Suitable for web and mobile** — cross-platform
- 🧩 **Extensible and customizable** — full freedom
- 🌐 **Serverless architecture** — scalable and economical
- 🧙 **Automated setup via MatiX Wizard** — just one token

---

## 🧙 Automated Setup with MatiX Wizard

> The easiest way to create and set up the panel is using **MatiX Wizard**. All you need is a Cloudflare API token — the rest is automated.

🔗 **Wizard URL:** [https://matix-wizard.imatixofficel.workers.dev/](https://matix-wizard.imatixofficel.workers.dev/)

### ✅ Prerequisites

- **Cloudflare account** (free)
- **Cloudflare API Token** with these permissions:
  - `Workers Scripts: Edit`
  - `Workers KV Storage: Edit`
  - `Account Settings: Read`

### 🔑 Creating a Cloudflare Token

1. Open [https://dash.cloudflare.com/profile/api-tokens](https://dash.cloudflare.com/profile/api-tokens)
2. Click **Create Token**.
3. Choose the **Edit Cloudflare Workers** template.
4. Ensure these permissions are enabled:
   - Account → Workers Scripts → **Edit**
   - Account → Workers KV Storage → **Edit**
   - Account → Account Settings → **Read**
5. Under **Account Resources**, select your account.
6. Click **Continue to summary** → **Create Token**.
7. **Copy** the token and store it safely.

> ⚠️ This token is shown only once.

### 🚀 Setup Steps

1. Open the Wizard URL.
2. Paste your Cloudflare token into the input field.
3. Click **Start**.
4. The Wizard will automatically:
   - ✅ Create the required **KV Namespace**
   - ✅ Create the **Worker** for the panel
   - ✅ Set up **Variables** and **Secrets** (including `ADMIN` and `UUID`)
   - ✅ Bind KV to the Worker
   - ✅ Perform the final **Deploy**
5. The Wizard shows your panel URL. Example:
```

https://matix-worker.YOURNAME.workers.dev

```
6. Log in to the panel:
```

https://matix-worker.YOURNAME.workers.dev/login

```
   The password is whatever you set as `ADMIN`.

### 📊 Setup Report

The Wizard reports each step as it runs, showing what has been done and what is currently in progress. If an error occurs, the reason is displayed.

### ⚠️ Security Notes

- The Cloudflare token is used **only during setup** and **is not stored**.
- After setup is complete, you can **Revoke** the token from the Cloudflare dashboard.
- Never commit the token to a public place (such as GitHub).

---

## 📁 Project Structure

```

MatiX-Worker/
├── worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── wrangler.toml

```

### 📄 Main File

The `worker.js` file is the core of the Worker, containing the project's execution logic.

---

## 🛠️ Manual Setup (Without Wizard)

### 1. Create the Project Folder

```bash
mkdir MatiX-Worker
cd MatiX-Worker
```

Create the main file:

```
worker.js
```

Then paste the Worker code into it.

2. Create the Worker in Cloudflare

Go to the Cloudflare dashboard and create a Worker from:

```
Workers & Pages → Create → Workers → Create Worker
```

Pick a name for the project; for example:

```
matix-worker
```

Then create and Deploy the Worker.

3. Add the Code

Open the Worker, paste the contents of worker.js into the code editor, then click:

```
Save and Deploy
```

After a successful Deploy, Cloudflare gives you a URL for the Worker.

---

🔐 Variables and Secrets

For non-sensitive settings, use Variables.

Typical path:

```
Workers & Pages → Worker → Settings → Variables and Secrets
```

For sensitive information such as the following, use Secrets:

· API_KEY
· BOT_TOKEN
· SECRET_KEY
· PASSWORD
· PRIVATE_TOKEN
· ADMIN
· UUID

⚠️ Security Note

❌ Wrong: Never put secrets inside worker.js or a public repository.

```javascript
const TOKEN = "YOUR_SECRET_TOKEN";   // ❌
```

✅ Correct: Store sensitive values as Secrets in Cloudflare and read them from the Worker environment.

---

🗄️ Using Cloudflare KV

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

---

🔄 GitHub Integration

Create a repository on GitHub and place the project files in it.

Once the repository is connected to Cloudflare, you can use GitHub to manage versions and publish changes.

---

🧭 Quick Start

If you just want to explore the project:

1. Open worker.js.
2. Set the required Variables and Secrets in Cloudflare.
3. Bind KV if needed.
4. Deploy the Worker.
5. Test the URL generated by Cloudflare.

---

🆘 Wizard Troubleshooting

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

---

📜 License

This project is released under the MIT License. See LICENSE for details.

---

📱 Connect with MatiX

· ✈️ Telegram: @Imatix7
· 📸 Instagram: @imatix_
· 🐙 GitHub: imatixofficel

---

