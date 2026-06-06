# Isles Core

Isles Core is the Paper plugin behind the Isles island survival experience. It creates protected void islands, shared team islands, custom center and dimension progression zones, generator upgrades, timed center events, and managed world reset tooling.

The repository also includes an Astro-based player wiki in `wiki/`.

## Features

- Automatic player island creation in a void island overworld.
- Team islands with invites, leadership transfer, rename, kick, leave, and disband flows.
- Protected island cores and custom spawnpoint handling.
- Center asteroid field with PvP, resource nodes, crates, regenerating content, and timed events.
- Generator upgrades purchased with Center Shards and materials.
- Managed Nether archipelago with protected terrain, resource nodes, mobs, and PvP hotspots.
- Managed End unlock with shared center portal and dragon arena setup.
- Runtime HUD/sidebar updates for island, shard, PvP, event, and progression state.
- Admin tools for island maintenance, center/nether rebuilds, dimension unlocks, biome fixes, reloads, and full world resets.

## Requirements

- A Paper-compatible Minecraft server matching the API version in `src/main/resources/plugin.yml`.
- A JDK with `javac` and `jar` available, or a `JAVA_EXE` entry in the parent repo `.env`.
- Paper server libraries under `<network-root>/skyblock/libraries`.

The build script expects this repository to live at:

```text
<network-root>/custom-plugins/skyblock-core
```

By default, `build.ps1` treats the grandparent directory as `<network-root>`.

## Build And Deploy

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

If the Paper libraries are missing, start your server once or run the parent repo jar download script before building.

## Plugin Configuration

Default configuration lives in `src/main/resources/config.yml` and is copied by Paper to:

```text
plugins/isles/config.yml
```

Important settings include:

- `world-name`, `nether-world-name`, `end-world-name`: managed world names.
- `auto-register-world-generators`: updates `bukkit.yml` with the plugin's world generators.
- `grid-spacing`, `island-y`, `center-y`, `center-radius`: island and center layout.
- `event-duration-minutes`, `node-respawn-seconds`: center event and resource timing.
- `pvp-only-in-center`, `physical-travel-only`: travel and PvP rules.
- `passive-mobs` and `wandering-traders`: island spawn assist behavior.
- `center-generator`: asteroid archetypes, palettes, structures, crates, event nodes, and loot.
- `nether-*`: Nether seed, biome radius, node respawns, ancient debris caps, and PvP hotspot radius.

Runtime state is stored in:

```text
plugins/isles/data.yml
```

That file contains island assignments, unlock state, mined node cooldowns, temporary blocks, teams, and active event state.

## Commands

Player commands:

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
| `/team disband` | Leader only. Remove or transfer leadership out of the team. |
| `/team info` | Show team leader and members. |

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

## World Reset Behavior

`/isles worldreset confirm` writes `plugins/isles/full-world-reset.flag`. On the next server startup, the plugin backs up configured Isles worlds and `plugins/isles/data.yml` to a sibling `backups` directory, removes the flag, and lets fresh worlds/data generate.

Use this for intentional season resets. For smaller maintenance, prefer the targeted reset commands:

- `/isles center reset`
- `/isles nether reset`
- `/isles island wipe`

## Player Wiki

The wiki is an Astro site configured for Cloudflare Workers.

```powershell
cd wiki
bun install
bun run dev
bun run build
bun run deploy
```

Wiki content and reference data live in `wiki/src/data/wiki.ts`.

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

## License

MIT. See `LICENSE`.
