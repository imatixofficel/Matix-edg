🕋 In the Name of Allah

🚀 MatiX Worker

MatiX Worker is a lightweight and customizable project built for Cloudflare Workers.

It can be used for APIs, web services, bots, online tools, and other serverless projects.

---

✨ Features

- ⚡ Fast and lightweight
- ☁️ Runs on Cloudflare Workers
- 🔐 Variables & Secrets support
- 🗄️ Cloudflare KV support
- 🔄 GitHub integration
- 📱 Suitable for web and mobile projects
- 🧩 Easy to customize
- 🌐 Serverless architecture

---

📁 Project Structure

MatiX-Worker/
│
├── _worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── .gitignore

📄 Main File

"_worker.js" is the main Cloudflare Worker file.

---

🛠️ Creating the Worker File

First, create a project folder:

mkdir MatiX-Worker
cd MatiX-Worker

Then create the main Worker file:

_worker.js

Put your Worker code inside "_worker.js".

---

☁️ Creating a Cloudflare Worker

Go to the Cloudflare Dashboard.

Navigate to:

Workers & Pages
        ↓
Create
        ↓
Workers
        ↓
Create Worker

Choose a name for your Worker.

Example:

matix-worker

Then click Create and Deploy.

---

📤 Uploading "_worker.js"

Open your Worker and select:

Edit Code

Then add your:

_worker.js

Worker code.

After adding the code, click:

Save and Deploy

After a successful deployment, Cloudflare will provide a Worker URL.

Example:

https://matix-worker.example.workers.dev

---

🔐 Variables

Variables are useful for configuration values that are not sensitive.

Go to:

Workers & Pages
        ↓
Your Worker
        ↓
Settings
        ↓
Variables and Secrets

Create a new Variable.

Example:

Variable Name:
API_URL

Value:
https://example.com/api

You can then access the value from your Worker environment.

---

🔒 Secrets

Never put sensitive information directly inside "_worker.js" or a public GitHub repository.

Examples of sensitive information:

- API Keys
- Bot Tokens
- Secret Keys
- Passwords
- Private Tokens

Go to:

Settings
        ↓
Variables and Secrets
        ↓
Add
        ↓
Secret

Add the secret there.

⚠️ Important

Do NOT write secrets directly inside your source code.

❌ Bad:

const TOKEN = "YOUR_SECRET_TOKEN";

✅ Better:

Cloudflare Secrets

---

🗄️ Cloudflare KV

Cloudflare KV can be used to store data that your Worker needs to access.

To create a KV Namespace:

Workers & Pages
        ↓
KV
        ↓
Create Namespace

Example Namespace:

MATIX_KV

---

🔗 Connecting KV to the Worker

After creating your KV Namespace:

Your Worker
        ↓
Settings
        ↓
Bindings

Add:

KV Namespace Binding

For example:

Binding Name:
KV

Then select your KV Namespace.

---

💻 Using KV in "_worker.js"

Example:

await env.KV.put("test", "Hello MatiX");

const value = await env.KV.get("test");

console.log(value);

«Important: The Binding name in Cloudflare must match the name used in your code.»

For example:

Cloudflare Binding:
KV

must match:

env.KV

---

🔄 GitHub Integration

Create a new repository on GitHub.

Recommended structure:

MatiX-Worker/
│
├── _worker.js
├── README.md
├── README_FA.md
├── LICENSE
└── .gitignore

You can then connect the repository to Cloudflare Workers for deployment and version control.

---

🔐 Security

Never upload private credentials to a public GitHub repository.

Keep these values inside Cloudflare Secrets:

API_KEY
BOT_TOKEN
SECRET_KEY
PASSWORD
PRIVATE_TOKEN

Your source code should not contain real secret values.

---

📱 MatiX

✈️ Telegram

"@Imatix7" (https://t.me/Imatix7)

📸 Instagram

@imatix_

---

⭐ Support

If you like this project:

⭐ Star the repository

🍴 Fork the repository

📢 Share it with others

---

<div align="center">🕋 In the Name of Allah

MatiX

Made with ❤️

</div>
