# Isles Player Wiki

This folder contains the Astro-based player wiki for Isles Core. It documents player commands, staff tools, upgrades, teams, the center asteroid field, Nether progression, End progression, and safety rules.

The wiki is fully prerendered and deployed with Cloudflare Workers Static Assets.

## Requirements

- Bun `1.3.14` or compatible.
- Node `>=22.12.0`.
- A Cloudflare account with a Workers project named `isles` for deployment.

## Development

Install dependencies:

```sh
bun install
```

Start the local dev server:

```sh
bun run dev
```

Build the static site:

```sh
bun run build
```

Preview the built site locally:

```sh
bun run preview
```

## Cloudflare Deployment

`wrangler.jsonc` points Cloudflare at the generated `./dist/` directory. There is no Worker script or server-side rendering adapter.

Useful commands:

| Command | Action |
| --- | --- |
| `bun run cf-preview` | Build and preview the Worker asset deployment locally. |
| `bun run cf-check` | Build and validate the Worker deployment with `wrangler deploy --dry-run`. |
| `bun run cf-deploy` | Deploy the already-built site. |
| `bun run cf-preview-deploy` | Upload a preview version for non-production branch builds. |
| `bun run deploy` | Build and deploy to Cloudflare Workers. |

For Cloudflare Workers Builds, connect the existing `isles` Worker project to this repository and use:

| Setting | Value |
| --- | --- |
| Root directory | `wiki` |
| Build command | `bun run build` |
| Deploy command | `bun run cf-deploy` |
| Non-production branch deploy command | `bun run cf-preview-deploy` |

## Content

Most wiki content and reference data lives in:

```text
src/data/wiki.ts
```

Static images and icons live under:

```text
public/assets/
```

## Project Structure

```text
.
|-- public/
|   `-- assets/
|-- src/
|   |-- components/
|   |-- data/
|   |   `-- wiki.ts
|   |-- layouts/
|   `-- pages/
|-- astro.config.mjs
|-- package.json
|-- tsconfig.json
`-- wrangler.jsonc
```
