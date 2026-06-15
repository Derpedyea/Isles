# Isles Core

Isles Core is a Paper server plugin that turns a Minecraft server into a protected skyblock island survival mode with teams, generator upgrades, custom void worlds, shared progression zones, and a deployable player wiki.

![Isles island icon](wiki/public/assets/isles/skyblock-island-icon-full.png)

**Download:** [Modrinth](https://modrinth.com/mod/isles) or [GitHub Releases](https://github.com/Derpedyea/Isles/releases)

## What It Does

- Creates protected void islands for new players, with custom spawn handling and safe void-death drop recovery.
- Supports shared team islands with invites, accepting/declining, leadership, kicking, leaving, renaming, and disbanding.
- Adds Center Shards and generator upgrades so cobblestone generators can progress into iron, gold, and diamond tiers.
- Generates a shared center asteroid field with PvP, resource nodes, crates, ruins, hazards, structures, and timed events.
- Unlocks managed Nether and End progression with custom terrain, resource respawns, PvP hotspots, and a shared End arena.
- Protects island cores, center nodes, Nether resource nodes, and generated terrain so players cannot accidentally break the game loop.
- Provides staff commands for resets, migrations, center/nether rebuilds, dimension unlocks, config reloads, diagnostics, and full season resets.
- Includes an Astro player wiki that can be deployed to Cloudflare Workers Static Assets.

## Server Requirements

- Paper server compatible with the API version in `src/main/resources/plugin.yml`.
- Java version required by that Paper server.
- Server file access so you can copy the jar into `plugins/` and edit `plugins/isles/config.yml`.

## Installation

Isles is published as a server-side Paper plugin jar.

1. Download the jar from [Modrinth](https://modrinth.com/mod/isles) or [GitHub Releases](https://github.com/Derpedyea/Isles/releases).
2. Stop your Paper server.
3. Put the jar in the server's `plugins/` folder.
4. Start the server.
5. Edit `plugins/isles/config.yml` if you want custom world names, island spacing, center timing, or generation settings.
6. Restart once more if the server says generator settings were written to `bukkit.yml`.

On first startup, Isles creates its config and runtime data files:

```text
plugins/isles/
|-- config.yml
`-- data.yml
```

## AI Disclosure

AI tools were used heavily during development, including for Java/Paper plugin code, debugging, refactoring, and documentation. I reviewed the generated code, tested the plugin myself, and playtested it with a group of friends on our personal SMP before shipping.

All public project media, including screenshots and videos, is real gameplay capture from the plugin running in Minecraft. No AI-generated media is used for the shipped project.

## Testing And Playtesting

This plugin has been run on a personal SMP by the developer and a small group of friends. Playtesting covered the main survival loop: joining the server, island creation, team island flows, generator progression, Center Shard collection, center resource nodes, PvP-zone messaging, and staff maintenance commands.

Because this is a server plugin, the most important validation is real in-game behavior on a Paper server with multiple players, worlds, teams, and progression state.

## Build Locally

The included PowerShell build script is meant for the local development server layout. It expects the plugin folder at:

```text
<network-root>/custom-plugins/skyblock-core
```

By default, `build.ps1` treats the grandparent directory as `<network-root>`.

Build requirements:

- JDK with `javac` and `jar` available, or a `JAVA_EXE` entry in `<network-root>/.env`.
- Paper server libraries under `<network-root>/skyblock/libraries`.
- PowerShell.

From this directory:

```powershell
.\build.ps1
```

If the network root is somewhere else:

```powershell
.\build.ps1 -Root E:\Code\Repos\mc-network
```

The script:

1. Finds Paper API and server library jars under `<network-root>\skyblock\libraries`.
2. Compiles `src/main/java`.
3. Copies `src/main/resources` into the class output.
4. Builds `build\isles-1.0.0.jar`.
5. Deploys it to `<network-root>\skyblock\plugins\isles-1.0.0.jar`.
6. Moves older legacy Skyblock plugin jars into `plugins\disabled-legacy`.

If the Paper libraries are missing, start the server once or add the Paper libraries under `<network-root>/skyblock/libraries` before building.

## Configuration

Default configuration lives in `src/main/resources/config.yml` and is copied by Paper to:

```text
plugins/isles/config.yml
```

Important settings include:

- `world-name`, `nether-world-name`, `end-world-name`: managed world names.
- `auto-register-world-generators`: writes Isles world generators into `bukkit.yml`.
- `grid-spacing`, `island-y`, `center-y`, `center-radius`: island and center layout.
- `event-duration-minutes`, `node-respawn-seconds`: center event and resource-node timing.
- `pvp-only-in-center`, `physical-travel-only`: travel and PvP rules.
- `passive-mobs` and `wandering-traders`: island spawn-assist behavior.
- `center-generator`: asteroid archetypes, palettes, structures, crates, event nodes, and loot.
- `nether-*`: Nether seed, biome radius, node respawns, ancient debris caps, and PvP hotspot radius.

Runtime state is stored in:

```text
plugins/isles/data.yml
```

That file contains island assignments, unlock state, mined node cooldowns, temporary blocks, teams, and active event state.

## Player Commands

| Command | Description |
| --- | --- |
| `/isles` | Show Isles help. |
| `/isles island` | Show island coordinates and team island info. |
| `/isles upgrades` | Open the generator upgrade menu. |
| `/isles upgrades buy <name>` | Buy an upgrade by friendly name, such as `iron`, `gold`, or `diamond`. |
| `/setspawn` | Set the island spawnpoint while standing on your island. |
| `/isles setspawn` | Alias for `/setspawn`. |
| `/team` | Show team command help. |
| `/team create <name>` | Create a team around your island. |
| `/team invite <player>` | Invite an online player to share your island. |
| `/team accept` | Accept a pending team invite. Your old solo island is wiped. |
| `/team decline` | Decline a pending team invite. |
| `/team leave` | Leave your team and receive a fresh island. |
| `/team kick <player>` | Leader only. Remove a member and give them a fresh island. |
| `/team rename <name>` | Leader only. Rename the team. |
| `/team disband` | Leader only. Transfer leadership out of the team. |
| `/team info` | Show team leader and members. |

## Staff Commands

Admin commands require:

```text
isles.admin
```

The legacy `mineperial.skyblock.admin` permission is still accepted for compatibility.

| Command | Description |
| --- | --- |
| `/isles island create <player>` | Create an island for a player who has joined before. |
| `/isles island list` | List island owners, assignment slots, and coordinates. |
| `/isles island wipe` | Clear all island blocks and reset island assignment. |
| `/isles event start` | Start a center event with bonus nodes and crates. |
| `/isles event stop` | Stop the active center event. |
| `/isles center reset` | Rebuild the center asteroid field. |
| `/isles center status` | Show center seed, asteroid count, archetypes, event settings, structures, and config warnings. |
| `/isles nether reset` | Rebuild the managed Nether archipelago. |
| `/isles biome fix` | Refresh managed world biome data. |
| `/isles worldreset confirm` | Queue a full Isles world and data reset for the next server startup. |
| `/isles unlock nether` | Unlock Nether portal travel and ensure the managed Nether exists. |
| `/isles unlock end` | Unlock the End, build the arena, and create the center End portal. |
| `/isles migrate [all\|config\|data\|items]` | Migrate legacy MSB config, data, and item tags after the Isles rename. |
| `/isles reload` | Reload config/data, refresh generators and managed worlds, and update player tab names. |

Legacy `/msb` command forms still work as compatibility aliases.

## Gameplay Systems

### Islands And Teams

Each player gets a protected void island. The island core cannot be broken, `/setspawn` lets players choose a safer respawn point, and team commands let multiple players share one island without duplicating progression.

### Generator Upgrades

Players earn Center Shards by mining center resources. They can spend those shards on:

| Upgrade | Cost | Effect |
| --- | --- | --- |
| Iron Generator | 8 Center Shards + 64 Cobblestone | Cobblestone generators can sometimes form iron ore. |
| Gold Generator | 18 Center Shards + 16 Iron Ingots | Cobblestone generators can sometimes form gold ore. |
| Diamond Generator | 40 Center Shards + 4 Diamonds | Cobblestone generators rarely form diamond ore. |

### Center

The center is a generated asteroid field around spawn. It includes resource nodes, gravel, crates, structures, hazards, PvP, shard rewards, and timed events. Normal players can only mine allowed nodes or temporary placed blocks there, which keeps the arena reusable.

### Nether And End

Staff can unlock the managed Nether and End when the season is ready. The Nether is generated as an archipelago with protected terrain, resource nodes, mobs, and PvP hotspots. The End has a shared portal and dragon arena.

## World Reset Behavior

`/isles worldreset confirm` writes:

```text
plugins/isles/full-world-reset.flag
```

On the next server startup, the plugin backs up configured Isles worlds and `plugins/isles/data.yml` to a sibling `backups` directory, removes the flag, and lets fresh worlds/data generate.

Use this for intentional season resets. For smaller maintenance, prefer targeted reset commands:

- `/isles center reset`
- `/isles nether reset`
- `/isles island wipe`

## Player Wiki

The wiki is an Astro site configured for Cloudflare Workers Static Assets.

```powershell
cd wiki
bun install
bun run dev
bun run build
bun run deploy
```

Wiki content and reference data live in `wiki/src/data/wiki.ts`.

See `wiki/README.md` for the wiki-specific development and deployment notes.

## Repository Layout

```text
.
|-- build.ps1
|-- src/
|   `-- main/
|       |-- java/net/mineperial/skyblockcore/MineperialSkyblockCorePlugin.java
|       `-- resources/
|           |-- config.yml
|           `-- plugin.yml
`-- wiki/
    |-- src/
    |-- public/
    |-- package.json
    `-- README.md
```

## Maintainer Release Workflow

GitHub release jars are built by `.github/workflows/release.yml`.

To publish from a tag:

```powershell
git tag v1.0.0
git push origin v1.0.0
```

The workflow reads the plugin version from `src/main/resources/plugin.yml`, compiles the plugin against Paper API `26.1.2.build.69-stable` by default, uploads the jar as an Actions artifact, and attaches `isles-<version>.jar` to the GitHub Release.

Maintainers can also run **Build GitHub Release** manually from the GitHub Actions tab. Leave the release tag blank to use `v<plugin.yml version>`, or provide a custom tag. If the release already exists, the workflow replaces the jar asset.

## Credits

- Built for the Mineperial Isles skyblock experience.
- Uses the Paper Minecraft server API.
- Player documentation is built with Astro, Bun, Wrangler, and Cloudflare Workers Static Assets.

## License

MIT. See `LICENSE`.
