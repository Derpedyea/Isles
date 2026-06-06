# Server Wiki

```sh
bun create astro@latest wiki --template minimal --install --no-git --yes
```

Basic Astro wiki scaffolded with Bun and configured for Cloudflare Workers Static Assets.

## Project Structure

Inside of your Astro project, you'll see the following folders and files:

```text
/
|-- public/
|-- src/
|   `-- pages/
|       `-- index.astro
`-- package.json
```

Astro looks for `.astro` or `.md` files in the `src/pages/` directory. Each page is exposed as a route based on its file name.

There's nothing special about `src/components/`, but that's where we like to put any Astro/React/Vue/Svelte/Preact components.

Any static assets, like images, can be placed in the `public/` directory.

## Commands

All commands are run from the root of the project, from a terminal:

| Command                   | Action                                           |
| :------------------------ | :----------------------------------------------- |
| `bun install`             | Installs dependencies                            |
| `bun run dev`             | Starts local dev server at `localhost:4321`      |
| `bun run build`           | Builds the production site to `./dist/`          |
| `bun run preview`         | Previews the built static site locally           |
| `bun run cf-preview`      | Builds and previews the Worker asset deployment locally |
| `bun run cf-check`        | Builds and validates the Worker asset deployment |
| `bun run cf-deploy`       | Deploys the already-built site in Cloudflare Workers Builds |
| `bun run cf-preview-deploy` | Uploads a preview version for non-production branch builds |
| `bun run deploy`          | Builds and deploys to Cloudflare Workers         |
| `bun astro ...`           | Runs Astro CLI commands                          |

## Cloudflare Workers

The wiki is fully prerendered and deployed with Workers Static Assets. `wrangler.jsonc`
points Cloudflare at the generated `./dist/` directory, with no Worker script or SSR
adapter needed.

## GitHub Auto Deploys

In Cloudflare Workers Builds, connect the existing `isles` Worker project to this repository and use:

| Setting | Value |
| :------ | :---- |
| Root directory | `wiki` |
| Build command | `bun run build` |
| Deploy command | `bun run cf-deploy` |
| Non-production branch deploy command | `bun run cf-preview-deploy` |

The Worker name in Cloudflare must match `isles`, the `name` in `wrangler.jsonc`.
