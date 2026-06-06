# Server Wiki

```sh
bun create astro@latest wiki --template minimal --install --no-git --yes
bun astro add cloudflare --yes
```

Basic Astro wiki scaffolded with Bun and configured for Cloudflare Workers.

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
| `bun run preview`         | Previews the built Worker locally                |
| `bun run generate-types`  | Generates Cloudflare Worker binding types        |
| `bun run deploy`          | Builds and deploys to Cloudflare Workers         |
| `bun astro ...`           | Runs Astro CLI commands                          |

## Cloudflare Workers

The app uses `@astrojs/cloudflare`, `wrangler.jsonc`, and the current date as the Workers compatibility date.
