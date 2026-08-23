# Matrix Edge Worker

A Cloudflare Workers project built around the supplied `_worker.js`.

## Project layout

- `_worker.js` — main Worker implementation.
- `wrangler.toml` — Wrangler deployment configuration and KV binding template.
- `.gitignore` — prevents local secrets and Wrangler state from being committed.

## Cloudflare setup

1. Create a Cloudflare Worker project.
2. Create a KV namespace.
3. Put the KV namespace ID into `wrangler.toml`.
4. Configure sensitive values as Worker variables/secrets in the Cloudflare dashboard or Wrangler. Do not commit real passwords, UUIDs, API keys, bot tokens, or account credentials.
5. Deploy with Wrangler.

## Environment values used by the supplied worker

The worker reads values including `ADMIN`/`PASSWORD`/`TOKEN`, `KEY`, `UUID`, `HOST`, `URL`, `DEBUG`, `PRELOAD_RACE_DIAL`, `PROXYIP`, `GO2SOCKS5`, `TCP_CONCURRENT_DIAL`, `PROXY_CONCURRENT_DIAL`, and `BEST_SUB`. The source also uses the `KV` binding for persistent configuration and logs.

The admin interface stores configuration in KV keys such as `config.json`, `cf.json`, `tg.json`, and `ADD.txt`.

## GitHub

This folder is intended to be committed as a normal Git repository. Before publishing, review the source for any third-party code/license requirements and remove all private credentials or deployment-specific values.

## Security

Use only infrastructure and endpoints you own or are authorized to administer. Keep administrative credentials and API tokens out of Git history.
