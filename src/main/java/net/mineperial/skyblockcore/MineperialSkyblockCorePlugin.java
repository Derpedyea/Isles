package net.mineperial.skyblockcore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class MineperialSkyblockCorePlugin extends JavaPlugin
    implements Listener, CommandExecutor, TabCompleter {
  private static final String ADMIN_PERMISSION = "mineperial.skyblock.admin";
  private static final String GENERATOR_ID_CENTER = "center";
  private static final String GENERATOR_ID_VOID = "void";
  private static final Material SHARD_MATERIAL = Material.AMETHYST_SHARD;
  private static final String UI_PREFIX = "\u00a76\u00a7lMineperial \u00a78| \u00a7r";
  private static final int UPGRADE_MENU_SIZE = 54;
  private static final int[] UPGRADE_MENU_SLOTS = {19, 21, 23, 25, 29, 31, 33, 35};
  private static final int PACKED_X_SHIFT = 42;
  private static final int PACKED_Y_SHIFT = 22;
  private static final int PACKED_XZ_BITS = 22;
  private static final int PACKED_Y_BITS = 20;
  private static final long PACKED_XZ_MASK = (1L << PACKED_XZ_BITS) - 1L;
  private static final long PACKED_Y_MASK = (1L << PACKED_Y_BITS) - 1L;
  private static final int END_ARENA_Y = 64;
  private static final int END_ARENA_RADIUS = 76;
  private static final int END_ARENA_CHUNK_RADIUS = 5;
  private static final int END_DRAGON_AGGRO_RANGE = 160;
  private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("[A-Za-z0-9 -]{3,24}");
  private static final List<String> PUBLIC_MSB_COMMANDS = List.of("island", "upgrades", "setspawn");
  private static final List<String> ADMIN_MSB_COMMANDS =
      List.of("event", "center", "nether", "biome", "worldreset", "unlock", "reload");
  private static final List<String> TEAM_SUBCOMMANDS =
      List.of("create", "invite", "accept", "decline", "leave", "kick", "rename", "disband", "info");
  private static final List<String> ISLAND_ADMIN_SUBCOMMANDS = List.of("create", "list", "wipe");
  private static final List<String> EVENT_ADMIN_SUBCOMMANDS = List.of("start", "stop");
  private static final List<String> CENTER_ADMIN_SUBCOMMANDS = List.of("reset", "status");
  private static final List<String> UNLOCK_ADMIN_SUBCOMMANDS = List.of("nether", "end");
  private static final List<EntityType> DEFAULT_PASSIVE_MOB_TYPES =
      List.of(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT);
  private static final int[][] END_PILLARS = {
    {0, 56, 88}, {35, 45, 91}, {55, 18, 84}, {55, -18, 94}, {35, -45, 86},
    {0, -56, 92}, {-35, -45, 89}, {-55, -18, 96}, {-55, 18, 87}, {-35, 45, 93}
  };

  private final VoidChunkGenerator voidGenerator = new VoidChunkGenerator();
  private final CenterAsteroidChunkGenerator centerGenerator = new CenterAsteroidChunkGenerator();
  private final Map<UUID, Island> islands = new LinkedHashMap<>();
  private final Map<UUID, Location> lastSafeLocations = new HashMap<>();
  private final Map<String, Material> nodeMaterialByKey = new HashMap<>();
  private final Map<String, MinedNode> minedNodes = new HashMap<>();
  private final Map<String, Material> netherNodeMaterialByKey = new HashMap<>();
  private final Map<String, MinedNode> netherMinedNodes = new HashMap<>();
  private final Set<String> temporaryBlocks = new HashSet<>();
  private final Set<String> netherPlayerBlocks = new HashSet<>();
  private final Set<String> netherProtectedBlocks = new HashSet<>();
  private final Set<String> eventNodes = new HashSet<>();
  private final Set<String> eventChests = new HashSet<>();
  private final Set<String> centerStructureBlocks = new HashSet<>();
  private final List<NetherHotspot> netherHotspots = new ArrayList<>();
  private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
  private final Map<UUID, Boolean> pvpZoneState = new HashMap<>();
  private final Map<UUID, List<String>> lastScoreboardLines = new HashMap<>();
  private final Map<String, SkyblockTeam> teams = new LinkedHashMap<>();
  private final Map<UUID, SkyblockTeam> teamByMember = new HashMap<>();
  private final Map<UUID, TeamInvite> pendingTeamInvites = new HashMap<>();

  private File dataFile;
  private YamlConfiguration data;
  private NamespacedKey shardKey;
  private NamespacedKey upgradeKey;
  private int nextIslandSlot;
  private boolean netherUnlocked;
  private boolean endUnlocked;
  private boolean eventActive;
  private long eventEndsAtMillis;
  private long nextPassiveMobAssistAtMillis;
  private long nextWanderingTraderAssistAtMillis;
  private long centerSeed = 776431L;
  private long netherSeed = 993177L;
  private long cachedCenterAsteroidSeed = Long.MIN_VALUE;
  private boolean warnedDragonStrafeReflection;
  private final Set<String> warnedGeneratorMismatches = new HashSet<>();
  private int cachedCenterAsteroidY = Integer.MIN_VALUE;
  private int cachedCenterAsteroidRadius = Integer.MIN_VALUE;
  private int cachedCenterGeneratorSignature = Integer.MIN_VALUE;
  private List<Asteroid> cachedCenterAsteroids = Collections.emptyList();

  @Override
  public void onLoad() {
    saveDefaultConfig();
    configureManagedWorldGenerators();
    performPendingFullWorldReset();
  }

  @Override
  public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    String generatorId = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    boolean isSkyblockOverworld = worldName != null && worldName.equals(getWorldName());
    if (GENERATOR_ID_CENTER.equals(generatorId)
        || (isSkyblockOverworld
            && (generatorId.isEmpty() || GENERATOR_ID_VOID.equals(generatorId)))) {
      return centerGenerator;
    }
    if (generatorId.isEmpty() || GENERATOR_ID_VOID.equals(generatorId)) {
      return voidGenerator;
    }
    getLogger().warning("Unknown world generator id '" + id + "' for " + worldName + "; using void.");
    return voidGenerator;
  }

  @Override
  public void onEnable() {
    shardKey = new NamespacedKey(this, "center_shard");
    upgradeKey = new NamespacedKey(this, "upgrade_id");
    registerUpgrades();
    loadData();
    refreshConfigCache();

    Bukkit.getPluginManager().registerEvents(this, this);
    registerCommands();

    Bukkit.getScheduler().runTask(this, this::initializeWorlds);
    scheduleRepeatingTasks();
    updateAllPlayerTabNames();
    getLogger().info("Mineperial Skyblock Core enabled.");
  }

  private void registerCommands() {
    registerCommand("msb");
    registerCommand("setspawn");
    registerCommand("team");
  }

  private void registerCommand(String name) {
    PluginCommand command = getCommand(name);
    if (command == null) {
      getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
      return;
    }
    command.setExecutor(this);
    command.setTabCompleter(this);
  }

  private void scheduleRepeatingTasks() {
    Bukkit.getScheduler().runTaskTimer(this, this::updateOnlinePlayerHuds, 40L, 40L);
    Bukkit.getScheduler().runTaskTimer(this, this::refreshNetherMobsIfUnlocked, 200L, 600L);
    Bukkit.getScheduler().runTaskTimer(this, this::refreshEndDragonAggro, 80L, 100L);
    Bukkit.getScheduler().runTaskTimer(this, this::refreshIslandMobSpawnAssists, 100L, 200L);
  }

  private void updateOnlinePlayerHuds() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      updatePlayerHud(player);
    }
  }

  private void refreshNetherMobsIfUnlocked() {
    if (netherUnlocked) {
      refreshNetherMobs();
    }
  }

  @Override
  public void onDisable() {
    Bukkit.getWorlds().forEach(world -> world.removePluginChunkTickets(this));
    saveData();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    Island island = effectiveIsland(player.getUniqueId());
    if (island == null) {
      island = getOrCreateEffectiveIsland(player);
      if (team == null) {
        sendSuccess(player, "Your Skyblock island was created at " + islandCoordinateText(island) + ".");
      }
    }
    updatePlayerTabName(player);

    Island finalIsland = island;
    Bukkit.getScheduler()
        .runTaskLater(
            this,
            () -> {
              if (!player.isOnline()) {
                return;
              }
              if (!player.hasPlayedBefore()
                  || isSkyblockOverworld(player.getWorld())
                      && player.getLocation().distanceSquared(getOverworld().getSpawnLocation()) < 64) {
                player.teleport(islandSpawnLocation(finalIsland));
              }
              updatePlayerHud(player);
            },
            20L);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) {
      return;
    }

    Player player = event.getPlayer();
    Location location = player.getLocation();
    Block below = location.clone().subtract(0, 1, 0).getBlock();
    if (below.getType().isSolid()
        && location.getY() > location.getWorld().getMinHeight() + 2) {
      lastSafeLocations.put(player.getUniqueId(), location.clone());
    }
    updatePlayerHud(player);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    pvpZoneState.remove(playerId);
    lastScoreboardLines.remove(playerId);
    pendingTeamInvites.remove(playerId);
    pendingTeamInvites.entrySet().removeIf(entry -> entry.getValue().inviterId.equals(playerId));
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    Island island = effectiveIsland(event.getPlayer().getUniqueId());
    if (island != null && !event.isBedSpawn() && !event.isAnchorSpawn()) {
      event.setRespawnLocation(islandSpawnLocation(island));
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    Iterator<ItemStack> iterator = event.getDrops().iterator();
    while (iterator.hasNext()) {
      ItemStack item = iterator.next();
      if (shouldKeepOnDeath(item)) {
        event.getItemsToKeep().add(item.clone());
        iterator.remove();
      }
    }

    EntityDamageEvent lastDamage = player.getLastDamageCause();
    boolean voidDeath =
        player.getLocation().getY() <= player.getWorld().getMinHeight() + 4
            || (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.VOID);

    if (!voidDeath || event.getDrops().isEmpty()) {
      return;
    }

    List<ItemStack> relocatedDrops =
        event.getDrops().stream().map(ItemStack::clone).collect(Collectors.toList());
    event.getDrops().clear();

    Location dropLocation = getSafeDropLocation(player);
    Bukkit.getScheduler()
        .runTask(
            this,
            () -> {
              for (ItemStack drop : relocatedDrops) {
                dropLocation.getWorld().dropItemNaturally(dropLocation, drop);
              }
            });
  }

  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!getConfig().getBoolean("pvp-only-in-center", true)) {
      return;
    }

    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }

    Player attacker = attackingPlayer(event.getDamager());
    if (attacker == null) {
      return;
    }

    if (!isPvpAllowed(victim.getLocation()) || !isPvpAllowed(attacker.getLocation())) {
      event.setCancelled(true);
      sendError(attacker, "PvP is only enabled inside the center or Nether bastion hotspots.");
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
        && isInManagedNether(event.getLocation())) {
      event.setCancelled(true);
      return;
    }

    if (!(event.getEntity() instanceof Monster)
        || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
        || !isInCenter(event.getLocation())) {
      return;
    }

    double keepChance =
        Math.max(0.0, Math.min(1.0, getConfig().getDouble("center-natural-monster-spawn-chance", 0.65)));
    if (ThreadLocalRandom.current().nextDouble() >= keepChance) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    if (isInManagedNether(block.getLocation())) {
      handleNetherBlockBreak(event);
      return;
    }

    if (!isInCenter(block.getLocation())) {
      if (isProtectedIslandCore(block)) {
        event.setCancelled(true);
        sendError(event.getPlayer(), "Island cores cannot be broken.");
      }
      return;
    }

    String key = locationKey(block.getLocation());
    Material nodeMaterial = nodeMaterialByKey.get(key);
    if (nodeMaterial != null) {
      if (isDepletedNode(key)) {
        event.setCancelled(true);
        sendWarning(event.getPlayer(), "That resource vein is regenerating.");
        return;
      }
      if (block.getType() != nodeMaterial) {
        event.setCancelled(true);
        return;
      }

      mineCenterNode(event, key, nodeMaterial);
      return;
    }

    if (temporaryBlocks.remove(key)) {
      saveData();
      return;
    }

    if (event.getPlayer().getGameMode() == GameMode.CREATIVE
        && event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
      return;
    }

    event.setCancelled(true);
    sendError(event.getPlayer(), "Only resource veins and temporary blocks can be mined here.");
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    if (isInManagedNether(event.getBlockPlaced().getLocation())) {
      String key = locationKey(event.getBlockPlaced().getLocation());
      if (netherNodeMaterialByKey.containsKey(key)) {
        event.setCancelled(true);
        sendWarning(event.getPlayer(), "That Nether resource node is regenerating.");
        return;
      }
      netherPlayerBlocks.add(key);
      saveData();
      return;
    }

    if (!isInCenter(event.getBlockPlaced().getLocation())) {
      return;
    }

    String key = locationKey(event.getBlockPlaced().getLocation());
    if (nodeMaterialByKey.containsKey(key)) {
      event.setCancelled(true);
      sendWarning(event.getPlayer(), "That resource vein is regenerating.");
      return;
    }

    temporaryBlocks.add(key);
    saveData();
  }

  private void handleNetherBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    String key = locationKey(block.getLocation());
    Material nodeMaterial = netherNodeMaterialByKey.get(key);
    if (nodeMaterial != null) {
      if (isDepletedNetherNode(key)) {
        event.setCancelled(true);
        sendWarning(event.getPlayer(), "That Nether resource is regenerating.");
        return;
      }
      if (block.getType() != nodeMaterial) {
        event.setCancelled(true);
        return;
      }

      mineNetherNode(event, key, nodeMaterial);
      return;
    }

    if (netherPlayerBlocks.remove(key)) {
      saveData();
      return;
    }

    if (event.getPlayer().getGameMode() == GameMode.CREATIVE
        && event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
      netherProtectedBlocks.remove(key);
      return;
    }

    if (netherProtectedBlocks.contains(key)) {
      event.setCancelled(true);
      sendError(event.getPlayer(), "Only player bridges and Nether resource nodes can be mined here.");
      return;
    }

    event.setCancelled(true);
    sendError(event.getPlayer(), "Only player bridges and Nether resource nodes can be mined here.");
  }

  @EventHandler
  public void onBlockForm(BlockFormEvent event) {
    Material formed = event.getNewState().getType();
    if (formed != Material.COBBLESTONE && formed != Material.STONE) {
      return;
    }

    Island island = nearestIsland(event.getBlock().getLocation(), 96);
    if (island == null) {
      return;
    }

    Random random = ThreadLocalRandom.current();
    if (island.upgrades.contains("generator_diamond") && random.nextDouble() < 0.02) {
      event.getNewState().setType(Material.DIAMOND_ORE);
    } else if (island.upgrades.contains("generator_gold") && random.nextDouble() < 0.05) {
      event.getNewState().setType(Material.GOLD_ORE);
    } else if (island.upgrades.contains("generator_iron") && random.nextDouble() < 0.12) {
      event.getNewState().setType(Material.IRON_ORE);
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof UpgradeMenuHolder holder)) {
      return;
    }

    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (event.getClickedInventory() == null || event.getClickedInventory() != event.getInventory()) {
      return;
    }

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || !clicked.hasItemMeta()) {
      return;
    }

    String upgradeId =
        clicked
            .getItemMeta()
            .getPersistentDataContainer()
            .get(upgradeKey, PersistentDataType.STRING);
    if (upgradeId == null) {
      return;
    }

    Island island = islands.get(holder.ownerId);
    Island effectiveIsland = effectiveIsland(player.getUniqueId());
    if (island == null || effectiveIsland == null || !holder.ownerId.equals(effectiveIsland.ownerId)) {
      player.closeInventory();
      sendError(player, "That upgrade menu is no longer valid.");
      return;
    }

    buyUpgrade(player, island, upgradeId);
    openUpgradeMenu(player, island);
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof UpgradeMenuHolder) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerPortal(PlayerPortalEvent event) {
    Location to = event.getTo();
    if (to == null || to.getWorld() == null) {
      return;
    }

    World fromWorld = event.getFrom().getWorld();
    World.Environment targetEnvironment = to.getWorld().getEnvironment();
    if (targetEnvironment == World.Environment.NETHER && !netherUnlocked) {
      event.setCancelled(true);
      sendError(event.getPlayer(), "The Nether is still locked for this SMP.");
      return;
    }

    if (targetEnvironment == World.Environment.NETHER) {
      World nether = ensureNetherWorld();
      event.setTo(netherEntryLocation(nether, event.getPlayer()));
      return;
    }

    if (isManagedNetherWorld(fromWorld) && targetEnvironment == World.Environment.NORMAL) {
      event.setTo(netherReturnLocation(event.getPlayer()));
      return;
    }

    if (targetEnvironment == World.Environment.THE_END && !endUnlocked) {
      event.setCancelled(true);
      sendError(event.getPlayer(), "The End is still locked for this SMP.");
      return;
    }

    if (targetEnvironment == World.Environment.THE_END) {
      World end = ensureEndWorld();
      event.setTo(endEntryLocation(end, event.getPlayer()));
    }
  }

  @EventHandler
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
      return;
    }

    if (!endUnlocked) {
      event.setCancelled(true);
      sendError(event.getPlayer(), "The End is still locked for this SMP.");
      return;
    }

    Location to = event.getTo();
    World toWorld = to == null ? null : to.getWorld();
    World fromWorld = event.getFrom().getWorld();
    if (fromWorld.getEnvironment() == World.Environment.THE_END
        || (toWorld != null && toWorld.getEnvironment() != World.Environment.THE_END)) {
      return;
    }

    World end = ensureEndWorld();
    event.setTo(endEntryLocation(end, event.getPlayer()));
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    World world = player.getWorld();
    if (world.getEnvironment() == World.Environment.NETHER) {
      if (isManagedNetherWorld(world)) {
        ensureNetherArchipelago(world, false);
        updatePlayerHud(player);
        return;
      }
      Location platformLocation = player.getLocation().clone();
      Bukkit.getScheduler()
          .runTaskLater(
              this,
              () -> {
                if (player.isOnline() && player.getWorld().equals(platformLocation.getWorld())) {
                  buildSafePlatform(platformLocation, Material.OBSIDIAN);
                }
              },
              2L);
    } else if (world.getEnvironment() == World.Environment.THE_END) {
      ensureEndArena(world);
      Bukkit.getScheduler().runTaskLater(this, this::refreshEndDragonAggro, 20L);
    }
    updatePlayerHud(player);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("setspawn")) {
      handleSetSpawnCommand(sender, args);
      return true;
    }
    if (command.getName().equalsIgnoreCase("team")) {
      handleTeamCommand(sender, args);
      return true;
    }

    if (args.length == 0) {
      sendHelp(sender);
      return true;
    }

    String subcommand = args[0].toLowerCase(Locale.ROOT);
    switch (subcommand) {
      case "island":
        handleIslandCommand(sender, args);
        return true;
      case "upgrades":
        handleUpgradesCommand(sender, args);
        return true;
      case "setspawn":
        handleSetSpawnCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
      case "event":
        if (!requireAdmin(sender)) {
          return true;
        }
        handleEventCommand(sender, args);
        return true;
      case "center":
        if (!requireAdmin(sender)) {
          return true;
        }
        handleCenterCommand(sender, args);
        return true;
      case "nether":
        if (!requireAdmin(sender)) {
          return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
          resetNether();
          sendSuccess(sender, "Nether archipelago reset.");
        } else {
          sendWarning(sender, "Usage: /msb nether reset");
        }
        return true;
      case "biome":
        if (!requireAdmin(sender)) {
          return true;
        }
        handleBiomeCommand(sender, args);
        return true;
      case "worldreset":
        if (!requireAdmin(sender)) {
          return true;
        }
        handleWorldResetCommand(sender, args);
        return true;
      case "unlock":
        if (!requireAdmin(sender)) {
          return true;
        }
        handleUnlockCommand(sender, args);
        return true;
      case "reload":
        if (!requireAdmin(sender)) {
          return true;
        }
        reloadConfig();
        configureManagedWorldGenerators();
        refreshConfigCache();
        loadData();
        initializeWorlds();
        updateAllPlayerTabNames();
        sendSuccess(sender, "Mineperial Skyblock reloaded.");
        return true;
      default:
        sendHelp(sender);
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (command.getName().equalsIgnoreCase("setspawn")) {
      return Collections.emptyList();
    }
    if (command.getName().equalsIgnoreCase("team")) {
      return completeTeamCommand(sender, args);
    }

    if (args.length == 1) {
      List<String> root = new ArrayList<>(PUBLIC_MSB_COMMANDS);
      if (sender.hasPermission(ADMIN_PERMISSION)) {
        root.addAll(ADMIN_MSB_COMMANDS);
      }
      return filter(root, args[0]);
    }

    if (args.length == 2) {
      if (args[0].equalsIgnoreCase("island") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(ISLAND_ADMIN_SUBCOMMANDS, args[1]);
      }
      if (args[0].equalsIgnoreCase("upgrades")) {
        return filter(Collections.singletonList("buy"), args[1]);
      }
      if (args[0].equalsIgnoreCase("event") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(EVENT_ADMIN_SUBCOMMANDS, args[1]);
      }
      if (args[0].equalsIgnoreCase("center") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(CENTER_ADMIN_SUBCOMMANDS, args[1]);
      }
      if (args[0].equalsIgnoreCase("nether") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(Collections.singletonList("reset"), args[1]);
      }
      if (args[0].equalsIgnoreCase("biome") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(Collections.singletonList("fix"), args[1]);
      }
      if (args[0].equalsIgnoreCase("worldreset") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(Collections.singletonList("confirm"), args[1]);
      }
      if (args[0].equalsIgnoreCase("unlock") && sender.hasPermission(ADMIN_PERMISSION)) {
        return filter(UNLOCK_ADMIN_SUBCOMMANDS, args[1]);
      }
    }

    if (args.length == 3
        && args[0].equalsIgnoreCase("upgrades")
        && args[1].equalsIgnoreCase("buy")) {
      return filter(upgradeBuySuggestions(), args[2]);
    }

    return Collections.emptyList();
  }

  private void initializeWorlds() {
    refreshConfigCache();
    World overworld = getOverworld();
    setBooleanGameRule(overworld, "keepInventory", false);
    setBooleanGameRule(overworld, "doImmediateRespawn", false);
    applyOverworldMobSpawnSettings(overworld);
    overworld.setSpawnLocation(new Location(overworld, 0.5, getConfig().getInt("center-y", 92) + 24, 0.5));
    if (!eventActive && (!eventNodes.isEmpty() || !eventChests.isEmpty())) {
      clearEventContent();
      saveData();
    }
    ensureCenter(false);
    normalizeManagedWorldBiomes();
    applyMinedNodeState();

    if (netherUnlocked) {
      ensureNetherWorld();
      applyNetherMinedNodeState();
      refreshNetherMobs();
    }
    if (endUnlocked) {
      ensureEndWorld();
      createEndPortalAtCenter();
    }
    if (eventActive) {
      resumeEvent();
    }
  }

  private void applyOverworldMobSpawnSettings(World overworld) {
    overworld.setTicksPerSpawns(
        SpawnCategory.ANIMAL, configInt("passive-mobs.ticks-per-spawn", 80, 1, 1200));
    overworld.setSpawnLimit(
        SpawnCategory.ANIMAL, configInt("passive-mobs.spawn-limit", 32, 0, 128));
    setBooleanGameRule(
        overworld, "doTraderSpawning", getConfig().getBoolean("wandering-traders.vanilla-enabled", true));
  }

  private void refreshIslandMobSpawnAssists() {
    long now = System.currentTimeMillis();

    if (getConfig().getBoolean("passive-mobs.assist-enabled", true)
        && now >= nextPassiveMobAssistAtMillis) {
      nextPassiveMobAssistAtMillis =
          now + configSecondsMillis("passive-mobs.check-seconds", 45, 5, 3600);
      assistPassiveMobSpawns();
    }

    if (getConfig().getBoolean("wandering-traders.assist-enabled", true)
        && now >= nextWanderingTraderAssistAtMillis) {
      nextWanderingTraderAssistAtMillis =
          now + configSecondsMillis("wandering-traders.check-seconds", 600, 30, 7200);
      assistWanderingTraderSpawns();
    }
  }

  private void assistPassiveMobSpawns() {
    World world = Bukkit.getWorld(getWorldName());
    if (world == null) {
      return;
    }

    int activeRadius = configIslandAssistRadius("passive-mobs.active-radius", 96);
    for (Island island : activeOverworldIslands(world, activeRadius)) {
      spawnPassiveMobsForIsland(world, island);
    }
  }

  private void spawnPassiveMobsForIsland(World world, Island island) {
    int target = configInt("passive-mobs.island-target", 10, 0, 64);
    if (target <= 0) {
      return;
    }

    int radius = configIslandAssistRadius("passive-mobs.spawn-radius", 28);
    Location center = islandEntityCenter(world, island);
    long existing = countNearbyEntities(center, radius, 64, radius, entity -> entity instanceof Animals);
    int needed = target - (int) existing;
    if (needed <= 0) {
      return;
    }

    List<EntityType> types = passiveMobSpawnTypes();
    Random random = ThreadLocalRandom.current();
    int spawns = Math.min(needed, configInt("passive-mobs.spawns-per-check", 2, 1, 8));
    for (int i = 0; i < spawns; i++) {
      Location spawn = randomIslandSurfaceSpawnLocation(world, island, radius, true);
      if (spawn == null) {
        return;
      }
      EntityType type = types.get(random.nextInt(types.size()));
      world.spawnEntity(spawn, type, CreatureSpawnEvent.SpawnReason.NATURAL);
    }
  }

  private void assistWanderingTraderSpawns() {
    World world = Bukkit.getWorld(getWorldName());
    if (world == null) {
      return;
    }

    int activeRadius = configIslandAssistRadius("wandering-traders.active-radius", 96);
    for (Island island : activeOverworldIslands(world, activeRadius)) {
      maybeSpawnWanderingTraderForIsland(world, island);
    }
  }

  private void maybeSpawnWanderingTraderForIsland(World world, Island island) {
    int limit = configInt("wandering-traders.island-limit", 1, 0, 4);
    if (limit <= 0) {
      return;
    }

    int radius = configIslandAssistRadius("wandering-traders.spawn-radius", 32);
    Location center = islandEntityCenter(world, island);
    long existing = countNearbyEntities(center, radius, 64, radius, entity -> entity instanceof WanderingTrader);
    if (existing >= limit) {
      return;
    }

    double chance = configDouble("wandering-traders.spawn-chance", 0.35, 0.0, 1.0);
    if (ThreadLocalRandom.current().nextDouble() >= chance) {
      return;
    }

    Location spawn = randomIslandSurfaceSpawnLocation(world, island, radius, false);
    if (spawn == null) {
      return;
    }

    int despawnTicks = configInt("wandering-traders.despawn-seconds", 1800, 60, 7200) * 20;
    Location wanderingTarget =
        new Location(world, island.x + 0.5, getConfig().getInt("island-y", 88) + 1.0, island.z + 0.5);
    world.spawn(
        spawn,
        WanderingTrader.class,
        CreatureSpawnEvent.SpawnReason.NATURAL,
        true,
        trader -> {
          trader.setDespawnDelay(despawnTicks);
          trader.setWanderingTowards(wanderingTarget);
        });
  }

  private long countNearbyEntities(
      Location center, double radiusX, double radiusY, double radiusZ, Predicate<Entity> matcher) {
    World world = center.getWorld();
    if (world == null) {
      return 0L;
    }
    return world.getNearbyEntities(center, radiusX, radiusY, radiusZ).stream().filter(matcher).count();
  }

  private List<Island> activeOverworldIslands(World world, int activeRadius) {
    List<Island> active = new ArrayList<>();
    Set<UUID> seen = new HashSet<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.getWorld().equals(world)) {
        continue;
      }

      Island island = nearestIsland(player.getLocation(), activeRadius);
      if (island != null && seen.add(island.ownerId)) {
        active.add(island);
      }
    }
    return active;
  }

  private Location islandEntityCenter(World world, Island island) {
    return new Location(world, island.x + 0.5, getConfig().getInt("island-y", 88) + 8.0, island.z + 0.5);
  }

  private Location randomIslandSurfaceSpawnLocation(World world, Island island, int radius, boolean requireGrass) {
    Random random = ThreadLocalRandom.current();
    int islandY = getConfig().getInt("island-y", 88);
    for (int attempt = 0; attempt < 48; attempt++) {
      int x = island.x + random.nextInt(radius * 2 + 1) - radius;
      int z = island.z + random.nextInt(radius * 2 + 1) - radius;
      Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
      if (ground.getY() < islandY - 8 || ground.getY() > islandY + 48) {
        continue;
      }
      if (!ground.isSolid() || ground.isLiquid()) {
        continue;
      }
      if (requireGrass && ground.getType() != Material.GRASS_BLOCK) {
        continue;
      }

      Block feet = world.getBlockAt(x, ground.getY() + 1, z);
      Block head = world.getBlockAt(x, ground.getY() + 2, z);
      if (!feet.isPassable() || !head.isPassable() || feet.isLiquid() || head.isLiquid()) {
        continue;
      }
      if (requireGrass && feet.getLightLevel() < 9) {
        continue;
      }
      return new Location(world, x + 0.5, ground.getY() + 1.0, z + 0.5);
    }
    return null;
  }

  private List<EntityType> passiveMobSpawnTypes() {
    List<String> configuredTypes = getConfig().getStringList("passive-mobs.types");
    if (configuredTypes.isEmpty()) {
      return DEFAULT_PASSIVE_MOB_TYPES;
    }

    List<EntityType> types = new ArrayList<>();
    for (String configuredType : configuredTypes) {
      try {
        EntityType type = EntityType.valueOf(configuredType.trim().toUpperCase(Locale.ROOT));
        Class<? extends Entity> entityClass = type.getEntityClass();
        if (type.isSpawnable() && entityClass != null && Animals.class.isAssignableFrom(entityClass)) {
          types.add(type);
        }
      } catch (IllegalArgumentException ignored) {
        // Invalid config entries are ignored so one typo does not disable all passive assists.
      }
    }
    return types.isEmpty() ? DEFAULT_PASSIVE_MOB_TYPES : types;
  }

  private int configInt(String path, int fallback, int min, int max) {
    return clamp(getConfig().getInt(path, fallback), min, max);
  }

  private double configDouble(String path, double fallback, double min, double max) {
    return clamp(getConfig().getDouble(path, fallback), min, max);
  }

  private long configSecondsMillis(String path, int fallback, int min, int max) {
    return (long) configInt(path, fallback, min, max) * 1000L;
  }

  private int configIslandAssistRadius(String path, int fallback) {
    int spacingRadius = Math.max(8, getConfig().getInt("grid-spacing", 384) / 2 - 16);
    return configInt(path, fallback, 4, Math.min(128, spacingRadius));
  }

  private void refreshConfigCache() {
    centerSeed = getConfig().getLong("center-seed", 776431L);
    netherSeed = getConfig().getLong("nether-seed", 993177L);
  }

  private void registerUpgrades() {
    upgrades.clear();
    upgrades.put(
        "generator_iron",
        new Upgrade(
            "generator_iron",
            "Iron Generator",
            "Cobblestone generators can sometimes form iron ore.",
            Material.IRON_ORE,
            8,
            Map.of(Material.COBBLESTONE, 64)));
    upgrades.put(
        "generator_gold",
        new Upgrade(
            "generator_gold",
            "Gold Generator",
            "Cobblestone generators can sometimes form gold ore.",
            Material.GOLD_ORE,
            18,
            Map.of(Material.IRON_INGOT, 16)));
    upgrades.put(
        "generator_diamond",
        new Upgrade(
            "generator_diamond",
            "Diamond Generator",
            "Cobblestone generators rarely form diamond ore.",
            Material.DIAMOND_ORE,
            40,
            Map.of(Material.DIAMOND, 4)));
  }

  private Set<String> knownUpgrades(Iterable<String> upgradeIds) {
    Set<String> known = new HashSet<>();
    for (String upgradeId : upgradeIds) {
      if (upgrades.containsKey(upgradeId)) {
        known.add(upgradeId);
      }
    }
    return known;
  }

  private int ownedUpgradeCount(Island island) {
    int owned = 0;
    for (String upgradeId : island.upgrades) {
      if (upgrades.containsKey(upgradeId)) {
        owned++;
      }
    }
    return owned;
  }

  private void loadData() {
    dataFile = new File(getDataFolder(), "data.yml");
    if (!dataFile.getParentFile().exists()) {
      dataFile.getParentFile().mkdirs();
    }

    data = YamlConfiguration.loadConfiguration(dataFile);
    islands.clear();
    minedNodes.clear();
    netherMinedNodes.clear();
    teams.clear();
    teamByMember.clear();
    pendingTeamInvites.clear();
    temporaryBlocks.clear();
    netherPlayerBlocks.clear();
    netherProtectedBlocks.clear();
    netherHotspots.clear();
    eventNodes.clear();
    eventChests.clear();

    nextIslandSlot = data.getInt("next-island-slot", 0);
    netherUnlocked = data.getBoolean("unlocks.nether", false);
    endUnlocked = data.getBoolean("unlocks.end", false);
    eventActive = data.getBoolean("event.active", false);
    eventEndsAtMillis = data.getLong("event.ends-at", 0L);

    int highestIslandSlot = nextIslandSlot - 1;
    ConfigurationSection islandSection = data.getConfigurationSection("islands");
    if (islandSection != null) {
      for (String key : islandSection.getKeys(false)) {
        UUID uuid;
        try {
          uuid = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
          getLogger().warning("Ignoring island with invalid owner UUID in data.yml: " + key);
          continue;
        }

        ConfigurationSection section = islandSection.getConfigurationSection(key);
        if (section == null) {
          continue;
        }
        int slot = section.getInt("slot");
        Island island =
            new Island(
                uuid,
                section.getString("name", uuid.toString()),
                slot,
                section.getInt("x"),
                section.getInt("z"),
                knownUpgrades(section.getStringList("upgrades")),
                readSpawnPoint(section));
        islands.put(uuid, island);
        highestIslandSlot = Math.max(highestIslandSlot, slot);
      }
    }
    nextIslandSlot = Math.max(nextIslandSlot, highestIslandSlot + 1);
    loadTeams(data.getConfigurationSection("teams"));

    ConfigurationSection minedSection = data.getConfigurationSection("mined-nodes");
    if (minedSection != null) {
      for (String key : minedSection.getKeys(false)) {
        Material material =
            Material.matchMaterial(minedSection.getString(key + ".material", "DIAMOND_ORE"));
        if (material != null) {
          minedNodes.put(key, new MinedNode(material, minedSection.getLong(key + ".respawn-at")));
        }
      }
    }

    temporaryBlocks.addAll(data.getStringList("temporary-blocks"));
    netherPlayerBlocks.addAll(data.getStringList("nether.player-blocks"));
    eventNodes.addAll(data.getStringList("event.nodes"));
    eventChests.addAll(data.getStringList("event.chests"));

    ConfigurationSection netherMinedSection = data.getConfigurationSection("nether.mined-nodes");
    if (netherMinedSection != null) {
      for (String key : netherMinedSection.getKeys(false)) {
        Material material =
            Material.matchMaterial(netherMinedSection.getString(key + ".material", "NETHER_QUARTZ_ORE"));
        if (material != null) {
          netherMinedNodes.put(key, new MinedNode(material, netherMinedSection.getLong(key + ".respawn-at")));
        }
      }
    }
  }

  private void loadTeams(ConfigurationSection teamSection) {
    if (teamSection == null) {
      return;
    }

    Set<UUID> assignedMembers = new HashSet<>();
    for (String key : teamSection.getKeys(false)) {
      ConfigurationSection section = teamSection.getConfigurationSection(key);
      if (section == null) {
        continue;
      }

      String displayName = cleanTeamName(section.getString("name", key));
      if (!isValidTeamName(displayName)) {
        getLogger().warning("Ignoring team with invalid name in data.yml: " + displayName);
        continue;
      }
      String normalizedName = normalizeTeamName(displayName);
      if (teams.containsKey(normalizedName)) {
        getLogger().warning("Ignoring duplicate team in data.yml: " + displayName);
        continue;
      }

      UUID leaderId = uuidFromString(section.getString("leader", ""));
      if (leaderId == null) {
        getLogger().warning("Ignoring team with invalid leader UUID in data.yml: " + displayName);
        continue;
      }

      List<UUID> members = new ArrayList<>();
      members.add(leaderId);
      for (String rawMemberId : section.getStringList("members")) {
        UUID memberId = uuidFromString(rawMemberId);
        if (memberId != null && !members.contains(memberId)) {
          members.add(memberId);
        }
      }

      boolean duplicateMember = false;
      for (UUID memberId : members) {
        if (assignedMembers.contains(memberId)) {
          duplicateMember = true;
          break;
        }
      }
      if (duplicateMember) {
        getLogger().warning("Ignoring team with duplicate member assignment in data.yml: " + displayName);
        continue;
      }

      SkyblockTeam team = new SkyblockTeam(displayName, normalizedName, leaderId, members);
      teams.put(normalizedName, team);
      for (UUID memberId : members) {
        assignedMembers.add(memberId);
        teamByMember.put(memberId, team);
      }
    }
  }

  private UUID uuidFromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private SpawnPoint readSpawnPoint(ConfigurationSection section) {
    ConfigurationSection spawnSection = section.getConfigurationSection("spawn");
    if (spawnSection == null
        || !spawnSection.contains("x")
        || !spawnSection.contains("y")
        || !spawnSection.contains("z")) {
      return null;
    }

    return new SpawnPoint(
        spawnSection.getDouble("x"),
        spawnSection.getDouble("y"),
        spawnSection.getDouble("z"),
        (float) spawnSection.getDouble("yaw", 0.0),
        (float) spawnSection.getDouble("pitch", 0.0));
  }

  private void saveData() {
    if (data == null || dataFile == null) {
      return;
    }

    data.set("next-island-slot", nextIslandSlot);
    data.set("unlocks.nether", netherUnlocked);
    data.set("unlocks.end", endUnlocked);
    data.set("event.active", eventActive);
    data.set("event.ends-at", eventEndsAtMillis);
    data.set("event.nodes", new ArrayList<>(eventNodes));
    data.set("event.chests", new ArrayList<>(eventChests));
    data.set("temporary-blocks", new ArrayList<>(temporaryBlocks));
    data.set("nether.player-blocks", new ArrayList<>(netherPlayerBlocks));
    data.set("islands", null);
    for (Island island : islands.values()) {
      String path = "islands." + island.ownerId;
      data.set(path + ".name", island.ownerName);
      data.set(path + ".slot", island.slot);
      data.set(path + ".x", island.x);
      data.set(path + ".z", island.z);
      data.set(path + ".upgrades", new ArrayList<>(knownUpgrades(island.upgrades)));
      if (island.spawnPoint != null) {
        data.set(path + ".spawn.x", island.spawnPoint.x);
        data.set(path + ".spawn.y", island.spawnPoint.y);
        data.set(path + ".spawn.z", island.spawnPoint.z);
        data.set(path + ".spawn.yaw", island.spawnPoint.yaw);
        data.set(path + ".spawn.pitch", island.spawnPoint.pitch);
      }
    }

    data.set("teams", null);
    for (SkyblockTeam team : teams.values()) {
      String path = "teams." + team.normalizedName;
      data.set(path + ".name", team.name);
      data.set(path + ".leader", team.leaderId.toString());
      data.set(
          path + ".members",
          team.memberIds.stream().map(UUID::toString).collect(Collectors.toList()));
    }

    data.set("mined-nodes", null);
    for (Map.Entry<String, MinedNode> entry : minedNodes.entrySet()) {
      data.set("mined-nodes." + entry.getKey() + ".material", entry.getValue().material.name());
      data.set("mined-nodes." + entry.getKey() + ".respawn-at", entry.getValue().respawnAtMillis);
    }

    data.set("nether.mined-nodes", null);
    for (Map.Entry<String, MinedNode> entry : netherMinedNodes.entrySet()) {
      data.set("nether.mined-nodes." + entry.getKey() + ".material", entry.getValue().material.name());
      data.set("nether.mined-nodes." + entry.getKey() + ".respawn-at", entry.getValue().respawnAtMillis);
    }

    try {
      data.save(dataFile);
    } catch (IOException e) {
      getLogger().warning("Could not save Skyblock data.yml: " + e.getMessage());
    }
  }

  private Island createIsland(UUID ownerId, String ownerName) {
    Island existing = islands.get(ownerId);
    if (existing != null) {
      return existing;
    }

    int slot = nextIslandSlot++;
    int[] grid = slotToGrid(slot);
    int spacing = getConfig().getInt("grid-spacing", 384);
    Island island =
        new Island(ownerId, ownerName, slot, grid[0] * spacing, grid[1] * spacing, new HashSet<>(), null);
    islands.put(ownerId, island);
    generateStarterIsland(island);
    saveData();
    return island;
  }

  private void generateStarterIsland(Island island) {
    World world = getOverworld();
    int y = getConfig().getInt("island-y", 88);
    setBiomeArea(world, island.x, island.z, islandBiomeRadius(), Biome.PLAINS);

    for (int x = -4; x <= 4; x++) {
      for (int z = -4; z <= 4; z++) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance > 5.1) {
          continue;
        }
        world.getBlockAt(island.x + x, y, island.z + z).setBiome(Biome.PLAINS);
        world.getBlockAt(island.x + x, y + 1, island.z + z).setBiome(Biome.PLAINS);
        world.getBlockAt(island.x + x, y - 1, island.z + z).setType(Material.DIRT);
        world.getBlockAt(island.x + x, y, island.z + z).setType(distance < 4.5 ? Material.GRASS_BLOCK : Material.DIRT);
      }
    }

    world.getBlockAt(island.x, y - 1, island.z).setType(Material.BEDROCK);
    world.getBlockAt(island.x, y + 1, island.z).setType(Material.LODESTONE);
    buildSimpleTree(world, island.x + 3, y + 1, island.z + 2);

    Block chestBlock = world.getBlockAt(island.x - 2, y + 1, island.z);
    chestBlock.setType(Material.CHEST);
    if (chestBlock.getState() instanceof Chest chest) {
      Inventory inventory = chest.getBlockInventory();
      inventory.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
      inventory.addItem(new ItemStack(Material.ICE, 2));
      inventory.addItem(new ItemStack(Material.OAK_SAPLING, 2));
      inventory.addItem(new ItemStack(Material.BONE_MEAL, 8));
      inventory.addItem(new ItemStack(Material.BREAD, 8));
      inventory.addItem(new ItemStack(Material.DIRT, 16));
      inventory.addItem(new ItemStack(Material.COBBLESTONE, 16));
      inventory.addItem(new ItemStack(Material.STRING, 4));
    }

    world.getBlockAt(island.x + 1, y + 1, island.z - 2).setType(Material.TORCH);
  }

  private void buildSimpleTree(World world, int x, int y, int z) {
    for (int i = 0; i < 5; i++) {
      world.getBlockAt(x, y + i, z).setType(Material.OAK_LOG);
    }
    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        for (int dy = 3; dy <= 5; dy++) {
          if (Math.abs(dx) + Math.abs(dz) + (dy == 5 ? 1 : 0) <= 4) {
            world.getBlockAt(x + dx, y + dy, z + dz).setType(Material.OAK_LEAVES);
          }
        }
      }
    }
  }

  private int[] slotToGrid(int slot) {
    List<int[]> slots = new ArrayList<>();
    int radius = 8;
    while ((radius * 2 + 1) * (radius * 2 + 1) - 1 <= slot) {
      radius++;
    }
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (x == 0 && z == 0) {
          continue;
        }
        slots.add(new int[] {x, z});
      }
    }
    slots.sort(
        (a, b) -> {
          int ad = a[0] * a[0] + a[1] * a[1];
          int bd = b[0] * b[0] + b[1] * b[1];
          if (ad != bd) {
            return Integer.compare(ad, bd);
          }
          return Double.compare(Math.atan2(a[1], a[0]), Math.atan2(b[1], b[0]));
        });
    return slots.get(slot);
  }

  private void ensureCenter(boolean force) {
    World world = getOverworld();
    setBiomeArea(world, 0, 0, getConfig().getInt("center-radius", 160) + 32, Biome.PLAINS);
    nodeMaterialByKey.clear();
    centerStructureBlocks.clear();

    List<Asteroid> asteroids = centerAsteroids();
    List<GeneratedAsteroid> generatedAsteroids = new ArrayList<>();
    for (int index = 0; index < asteroids.size(); index++) {
      Asteroid asteroid = asteroids.get(index);
      AsteroidBlocks blocks = collectAsteroidBlocks(asteroid);
      for (long packed : blocks.solidBlocks) {
        int x = unpackX(packed);
        int y = unpackY(packed);
        int z = unpackZ(packed);
        Block block = world.getBlockAt(x, y, z);
        if (force || block.getType().isAir()) {
          block.setType(centerStoneMaterial(asteroid, x, y, z), false);
        }
      }

      addBaseNodes(world, asteroid, index, force, blocks.surfaceBlocks);
      generatedAsteroids.add(new GeneratedAsteroid(asteroid, index, blocks.surfaceBlocks));
    }

    addCenterStructures(world, generatedAsteroids, force);
    for (GeneratedAsteroid generated : generatedAsteroids) {
      addCenterFeatures(world, generated.asteroid, generated.index, force, generated.surfaceBlocks);
    }

    for (String key : new ArrayList<>(eventNodes)) {
      Block block = blockFromKey(key);
      if (block == null) {
        eventNodes.remove(key);
        minedNodes.remove(key);
        continue;
      }
      MinedNode mined = minedNodes.get(key);
      Material eventMaterial = mined == null ? eventNodeMaterial(block) : mined.material;
      nodeMaterialByKey.put(key, eventMaterial);
      if (!minedNodes.containsKey(key)) {
        block.setType(eventMaterial, false);
      }
    }

    for (String key : new ArrayList<>(eventChests)) {
      Block block = blockFromKey(key);
      if (block == null) {
        eventChests.remove(key);
        continue;
      }
      if (block != null && block.getType() != Material.CHEST) {
        block.setType(Material.CHEST, false);
        fillEventChest(block);
      }
    }
  }

  private void addBaseNodes(
      World world, Asteroid asteroid, int index, boolean force, List<Long> surfaceBlocks) {
    int placed = 0;
    for (long packed : surfaceBlocks) {
      int x = unpackX(packed);
      int y = unpackY(packed);
      int z = unpackZ(packed);
      Material node = surfaceNodeMaterial(asteroid, x, y, z);
      if (node == null) {
        continue;
      }

      registerCenterNode(world, x, y, z, node, force);
      placed++;
    }

    int minimumNodes = Math.max(5, asteroid.tier * 3);
    if (placed < minimumNodes) {
      addFallbackSurfaceNodes(world, asteroid, index, minimumNodes - placed, force, surfaceBlocks);
    }

    addIronSurfaceNodeClusters(world, asteroid, index, force, surfaceBlocks);
  }

  private int addFallbackSurfaceNodes(
      World world, Asteroid asteroid, int index, int needed, boolean force, List<Long> surfaceBlocks) {
    Random random = new Random(asteroid.seed ^ (index * 341873128712L));
    int placed = 0;
    for (int tries = 0; tries < 800 && placed < needed; tries++) {
      Long packed = randomSurfaceBlock(random, surfaceBlocks);
      if (packed == null) {
        break;
      }
      Material node = patchNodeMaterial(asteroid.tier, placed, random);
      registerCenterNode(world, unpackX(packed), unpackY(packed), unpackZ(packed), node, force);
      placed++;
    }
    return placed;
  }

  private void addIronSurfaceNodeClusters(
      World world, Asteroid asteroid, int index, boolean force, List<Long> surfaceBlocks) {
    if (surfaceBlocks.isEmpty()) {
      return;
    }

    Set<Long> surfaceBlockSet = new HashSet<>(surfaceBlocks);
    List<Long> candidates = new ArrayList<>();
    for (long packed : surfaceBlocks) {
      if (isUpperExposedSurfaceBlock(asteroid, packed, surfaceBlockSet)) {
        candidates.add(packed);
      }
    }
    if (candidates.isEmpty()) {
      return;
    }

    Random random = new Random(asteroid.seed ^ 0x25B4951E6F4A3C2DL ^ index * 193L);
    Collections.shuffle(candidates, random);

    Set<Long> selected = new HashSet<>();
    int targetNodes = ironSurfaceNodeTarget(asteroid, index);
    int placed = 0;
    for (long anchor : candidates) {
      if (placed >= targetNodes) {
        break;
      }
      if (selected.contains(anchor)) {
        continue;
      }
      placed +=
          placeIronSurfaceNodeCluster(
              world, asteroid, anchor, surfaceBlockSet, selected, force, targetNodes - placed);
    }
  }

  private int ironSurfaceNodeTarget(Asteroid asteroid, int index) {
    if (index == 0) {
      return 32;
    }
    return 5 + asteroid.tier * 3;
  }

  private int placeIronSurfaceNodeCluster(
      World world,
      Asteroid asteroid,
      long anchor,
      Set<Long> surfaceBlockSet,
      Set<Long> selected,
      boolean force,
      int remaining) {
    int anchorX = unpackX(anchor);
    int anchorY = unpackY(anchor);
    int anchorZ = unpackZ(anchor);
    int clusterSize =
        Math.min(remaining, 2 + Math.floorMod(hash(anchorX, anchorY, anchorZ, asteroid.noiseSalt + 811), 3));
    List<Long> cluster = new ArrayList<>();

    for (int dx = -2; dx <= 2; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        for (int dz = -2; dz <= 2; dz++) {
          if (dx * dx + dy * dy + dz * dz > 5) {
            continue;
          }
          long packed = packBlock(anchorX + dx, anchorY + dy, anchorZ + dz);
          if (selected.contains(packed)
              || !isUpperExposedSurfaceBlock(asteroid, packed, surfaceBlockSet)) {
            continue;
          }
          cluster.add(packed);
        }
      }
    }

    cluster.sort(
        (a, b) -> {
          int distanceCompare = Integer.compare(blockDistanceSquared(anchor, a), blockDistanceSquared(anchor, b));
          if (distanceCompare != 0) {
            return distanceCompare;
          }
          int hashA = hash(unpackX(a), unpackY(a), unpackZ(a), asteroid.noiseSalt + 1499);
          int hashB = hash(unpackX(b), unpackY(b), unpackZ(b), asteroid.noiseSalt + 1499);
          return Integer.compare(hashA, hashB);
        });

    int placed = 0;
    for (long packed : cluster) {
      if (placed >= clusterSize) {
        break;
      }
      selected.add(packed);
      registerCenterNode(world, unpackX(packed), unpackY(packed), unpackZ(packed), Material.IRON_ORE, force);
      placed++;
    }
    return placed;
  }

  private boolean isUpperExposedSurfaceBlock(Asteroid asteroid, long packed, Set<Long> surfaceBlockSet) {
    if (!surfaceBlockSet.contains(packed)) {
      return false;
    }

    int x = unpackX(packed);
    int y = unpackY(packed);
    int z = unpackZ(packed);
    int minimumY = asteroid.y - Math.max(1, asteroid.radiusY / 3);
    return y >= minimumY && !insideAsteroid(asteroid, x, y + 1, z);
  }

  private int blockDistanceSquared(long a, long b) {
    int dx = unpackX(a) - unpackX(b);
    int dy = unpackY(a) - unpackY(b);
    int dz = unpackZ(a) - unpackZ(b);
    return dx * dx + dy * dy + dz * dz;
  }

  private Long randomSurfaceBlock(Random random, List<Long> surfaceBlocks) {
    if (surfaceBlocks.isEmpty()) {
      return null;
    }
    return surfaceBlocks.get(random.nextInt(surfaceBlocks.size()));
  }

  private void addCenterStructures(World world, List<GeneratedAsteroid> generatedAsteroids, boolean force) {
    CenterGeneratorSettings settings = centerGeneratorSettings();
    if (!settings.structuresEnabled || settings.structureCount <= 0 || generatedAsteroids.isEmpty()) {
      return;
    }

    int placed = 0;
    if (settings.centralStructure) {
      GeneratedAsteroid central = generatedAsteroids.get(0);
      Random random = new Random(central.asteroid.seed ^ 0x3A70E8894B5C6D31L);
      if (placeCenterStructure(world, central, CenterStructureType.ANCIENT_GATE, random, force)) {
        placed++;
      }
    }

    List<GeneratedAsteroid> candidates = new ArrayList<>();
    for (GeneratedAsteroid generated : generatedAsteroids) {
      if (generated.index == 0 || generated.asteroid.tier < settings.structureMinTier) {
        continue;
      }
      candidates.add(generated);
    }
    Collections.shuffle(candidates, new Random(centerSeed ^ settings.structureSalt ^ centerGeneratorSignature()));

    for (GeneratedAsteroid generated : candidates) {
      if (placed >= settings.structureCount) {
        break;
      }
      Random random = new Random(generated.asteroid.seed ^ 0x57B68A52622E7F17L ^ generated.index * 131L);
      CenterStructureType type = centerStructureType(generated.asteroid, generated.index, random);
      if (placeCenterStructure(world, generated, type, random, force)) {
        placed++;
      }
    }
  }

  private CenterStructureType centerStructureType(Asteroid asteroid, int index, Random random) {
    return switch (asteroid.archetype.id) {
      case "crystal", "icy" -> CenterStructureType.CRYSTAL_SHRINE;
      case "metallic" -> random.nextBoolean() ? CenterStructureType.MINING_OUTPOST : CenterStructureType.BROKEN_RELAY;
      case "volcanic" -> random.nextBoolean() ? CenterStructureType.BROKEN_RELAY : CenterStructureType.WATCH_SPIRE;
      case "ruined" -> random.nextBoolean() ? CenterStructureType.WATCH_SPIRE : CenterStructureType.ANCIENT_GATE;
      default -> switch (Math.floorMod(index + random.nextInt(16), 4)) {
        case 0 -> CenterStructureType.MINING_OUTPOST;
        case 1 -> CenterStructureType.CRYSTAL_SHRINE;
        case 2 -> CenterStructureType.BROKEN_RELAY;
        default -> CenterStructureType.WATCH_SPIRE;
      };
    };
  }

  private boolean placeCenterStructure(
      World world, GeneratedAsteroid generated, CenterStructureType type, Random random, boolean force) {
    Long anchor = structureAnchor(world, generated, type, random);
    if (anchor == null) {
      return false;
    }

    int baseX = unpackX(anchor);
    int baseY = unpackY(anchor) + 1;
    int baseZ = unpackZ(anchor);
    return switch (type) {
      case ANCIENT_GATE -> buildAncientGate(world, generated.asteroid, baseX, baseY, baseZ, random, force);
      case MINING_OUTPOST -> buildMiningOutpost(world, generated.asteroid, baseX, baseY, baseZ, random, force);
      case CRYSTAL_SHRINE -> buildCrystalShrine(world, generated.asteroid, baseX, baseY, baseZ, random, force);
      case BROKEN_RELAY -> buildBrokenRelay(world, generated.asteroid, baseX, baseY, baseZ, random, force);
      case WATCH_SPIRE -> buildWatchSpire(world, generated.asteroid, baseX, baseY, baseZ, random, force);
    };
  }

  private Long structureAnchor(
      World world, GeneratedAsteroid generated, CenterStructureType type, Random random) {
    Asteroid asteroid = generated.asteroid;
    if (generated.index == 0) {
      Long central = highestAsteroidSurfaceAt(asteroid, asteroid.x, asteroid.z);
      if (central != null && isUsableStructureAnchor(world, asteroid, central, type)) {
        return central;
      }
    }

    List<Long> candidates = new ArrayList<>(generated.surfaceBlocks);
    Collections.shuffle(candidates, random);
    for (long packed : candidates) {
      if (isUsableStructureAnchor(world, asteroid, packed, type)) {
        return packed;
      }
    }
    return null;
  }

  private Long highestAsteroidSurfaceAt(Asteroid asteroid, int x, int z) {
    for (int y = asteroid.y + asteroid.radiusY + 8; y >= asteroid.y - asteroid.radiusY - 4; y--) {
      if (insideAsteroid(asteroid, x, y, z) && !insideAsteroid(asteroid, x, y + 1, z)) {
        return packBlock(x, y, z);
      }
    }
    return null;
  }

  private boolean isUsableStructureAnchor(
      World world, Asteroid asteroid, long packed, CenterStructureType type) {
    int x = unpackX(packed);
    int y = unpackY(packed);
    int z = unpackZ(packed);
    int minimumY = asteroid.y + Math.max(1, asteroid.radiusY / 4);
    if (y < minimumY) {
      return false;
    }

    Block base = world.getBlockAt(x, y, z);
    Block above = world.getBlockAt(x, y + 1, z);
    if (!base.getType().isSolid() || (!above.getType().isAir() && !isReplaceableCenterFeature(above.getType()))) {
      return false;
    }

    int radius = centerStructureRadius(type);
    int supported = 0;
    int checked = 0;
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        if (dx * dx + dz * dz > radius * radius) {
          continue;
        }
        checked++;
        Block support = world.getBlockAt(x + dx, y, z + dz);
        if (support.getType().isSolid() || findSupportY(world, x + dx, y, z + dz, 6) != Integer.MIN_VALUE) {
          supported++;
        }
      }
    }
    return checked == 0 || supported >= Math.max(3, checked / 3);
  }

  private int centerStructureRadius(CenterStructureType type) {
    return switch (type) {
      case ANCIENT_GATE -> 6;
      case MINING_OUTPOST -> 4;
      case CRYSTAL_SHRINE -> 4;
      case BROKEN_RELAY -> 3;
      case WATCH_SPIRE -> 3;
    };
  }

  private int findSupportY(World world, int x, int startY, int z, int depth) {
    for (int y = startY; y >= startY - depth; y--) {
      Material material = world.getBlockAt(x, y, z).getType();
      if (!material.isAir() && !isReplaceableCenterFeature(material)) {
        return y;
      }
    }
    return Integer.MIN_VALUE;
  }

  private boolean buildAncientGate(
      World world, Asteroid asteroid, int baseX, int baseY, int baseZ, Random random, boolean force) {
    buildRoundStructureFloor(
        world,
        baseX,
        baseY,
        baseZ,
        6,
        Material.DEEPSLATE_BRICKS,
        Material.CRACKED_DEEPSLATE_BRICKS,
        Material.COBBLED_DEEPSLATE,
        random,
        force);

    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        Material material = Math.abs(dx) == 2 || Math.abs(dz) == 2 ? Material.OBSIDIAN : Material.CHISELED_DEEPSLATE;
        setCenterStructureBlock(world, baseX + dx, baseY + 1, baseZ + dz, material, force);
      }
    }

    for (int[] offset : new int[][] {{-5, 0}, {5, 0}, {0, -5}, {0, 5}}) {
      int x = baseX + offset[0];
      int z = baseZ + offset[1];
      for (int dy = 1; dy <= 5; dy++) {
        Material material = dy == 5 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
        setCenterStructureBlock(world, x, baseY + dy, z, material, force);
      }
      setCenterStructureBlock(world, x, baseY + 6, z, Material.END_ROD, force);
    }

    for (int i = -4; i <= 4; i += 2) {
      setCenterStructureBlock(world, baseX + i, baseY + 2, baseZ - 6, Material.IRON_CHAIN, force);
      setCenterStructureBlock(world, baseX - 6, baseY + 2, baseZ + i, Material.IRON_CHAIN, force);
    }
    placeStructureChest(world, baseX + 4, baseY + 2, baseZ + 4, asteroid.archetype, random, force);
    maybePlaceStructureSpawner(world, baseX - 3, baseY + 2, baseZ + 3, asteroid.archetype, random, force, 0.35);
    return true;
  }

  private boolean buildMiningOutpost(
      World world, Asteroid asteroid, int baseX, int baseY, int baseZ, Random random, boolean force) {
    buildSquareStructureFloor(
        world,
        baseX,
        baseY,
        baseZ,
        3,
        Material.COBBLED_DEEPSLATE,
        Material.CUT_COPPER,
        Material.DEEPSLATE_BRICKS,
        random,
        force);

    for (int[] corner : new int[][] {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}) {
      for (int dy = 1; dy <= 3; dy++) {
        setCenterStructureBlock(world, baseX + corner[0], baseY + dy, baseZ + corner[1], Material.IRON_BARS, force);
      }
      setCenterStructureBlock(world, baseX + corner[0], baseY + 4, baseZ + corner[1], Material.LANTERN, force);
    }

    for (int dx = -2; dx <= 2; dx++) {
      setCenterStructureBlock(world, baseX + dx, baseY + 1, baseZ - 3, Material.COBBLED_DEEPSLATE_WALL, force);
    }
    setCenterStructureBlock(world, baseX - 1, baseY + 1, baseZ, Material.FURNACE, force);
    setCenterStructureBlock(world, baseX + 1, baseY + 1, baseZ, Material.CRAFTING_TABLE, force);
    setCenterStructureBlock(world, baseX, baseY + 1, baseZ + 2, Material.ANVIL, force);
    placeStructureChest(world, baseX + 2, baseY + 1, baseZ - 1, asteroid.archetype, random, force);
    return true;
  }

  private boolean buildCrystalShrine(
      World world, Asteroid asteroid, int baseX, int baseY, int baseZ, Random random, boolean force) {
    buildRoundStructureFloor(
        world,
        baseX,
        baseY,
        baseZ,
        4,
        Material.CALCITE,
        Material.SMOOTH_BASALT,
        Material.AMETHYST_BLOCK,
        random,
        force);

    setCenterStructureBlock(world, baseX, baseY + 1, baseZ, Material.BUDDING_AMETHYST, force);
    setCenterStructureBlock(world, baseX, baseY + 2, baseZ, Material.AMETHYST_BLOCK, force);
    setCenterStructureBlock(world, baseX, baseY + 3, baseZ, Material.LARGE_AMETHYST_BUD, force);
    for (int[] offset : new int[][] {{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
      setCenterStructureBlock(world, baseX + offset[0], baseY + 1, baseZ + offset[1], Material.SEA_LANTERN, force);
      setCenterStructureBlock(world, baseX + offset[0], baseY + 2, baseZ + offset[1], Material.AMETHYST_CLUSTER, force);
    }
    if (random.nextBoolean()) {
      placeStructureChest(world, baseX + 2, baseY + 1, baseZ + 2, asteroid.archetype, random, force);
    }
    return true;
  }

  private boolean buildBrokenRelay(
      World world, Asteroid asteroid, int baseX, int baseY, int baseZ, Random random, boolean force) {
    buildSquareStructureFloor(
        world,
        baseX,
        baseY,
        baseZ,
        2,
        Material.CUT_COPPER,
        Material.DEEPSLATE_BRICKS,
        Material.COPPER_BLOCK,
        random,
        force);

    for (int dy = 1; dy <= 6; dy++) {
      Material material = dy == 6 ? Material.REDSTONE_BLOCK : (dy % 2 == 0 ? Material.COPPER_BLOCK : Material.CUT_COPPER);
      setCenterStructureBlock(world, baseX, baseY + dy, baseZ, material, force);
    }
    setCenterStructureBlock(world, baseX, baseY + 7, baseZ, Material.END_ROD, force);
    for (int[] offset : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
      setCenterStructureBlock(world, baseX + offset[0], baseY + 1, baseZ + offset[1], Material.IRON_BARS, force);
      setCenterStructureBlock(world, baseX + offset[0], baseY + 2, baseZ + offset[1], Material.IRON_CHAIN, force);
    }
    placeStructureChest(world, baseX - 2, baseY + 1, baseZ - 2, asteroid.archetype, random, force);
    return true;
  }

  private boolean buildWatchSpire(
      World world, Asteroid asteroid, int baseX, int baseY, int baseZ, Random random, boolean force) {
    buildSquareStructureFloor(
        world,
        baseX,
        baseY,
        baseZ,
        2,
        Material.CRACKED_DEEPSLATE_BRICKS,
        Material.CHISELED_DEEPSLATE,
        Material.COBBLED_DEEPSLATE,
        random,
        force);

    for (int dy = 1; dy <= 6; dy++) {
      setCenterStructureBlock(world, baseX, baseY + dy, baseZ, dy % 3 == 0 ? Material.CHISELED_DEEPSLATE : Material.DEEPSLATE_BRICKS, force);
    }
    for (int[] offset : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
      setCenterStructureBlock(world, baseX + offset[0], baseY + 4, baseZ + offset[1], Material.IRON_BARS, force);
      setCenterStructureBlock(world, baseX + offset[0], baseY + 7, baseZ + offset[1], Material.COBBLED_DEEPSLATE_WALL, force);
    }
    setCenterStructureBlock(world, baseX, baseY + 8, baseZ, Material.LANTERN, force);
    placeStructureChest(world, baseX - 2, baseY + 1, baseZ + 2, asteroid.archetype, random, force);
    maybePlaceStructureSpawner(world, baseX + 2, baseY + 1, baseZ, asteroid.archetype, random, force, 0.25);
    return true;
  }

  private void buildRoundStructureFloor(
      World world,
      int baseX,
      int baseY,
      int baseZ,
      int radius,
      Material primary,
      Material secondary,
      Material support,
      Random random,
      boolean force) {
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        int distanceSquared = dx * dx + dz * dz;
        if (distanceSquared > radius * radius) {
          continue;
        }
        Material material = distanceSquared >= (radius - 1) * (radius - 1) || random.nextInt(7) == 0 ? secondary : primary;
        setCenterStructureBlock(world, baseX + dx, baseY, baseZ + dz, material, force);
        buildStructureSupport(world, baseX + dx, baseY - 1, baseZ + dz, support, force);
      }
    }
  }

  private void buildSquareStructureFloor(
      World world,
      int baseX,
      int baseY,
      int baseZ,
      int radius,
      Material primary,
      Material secondary,
      Material support,
      Random random,
      boolean force) {
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        Material material = Math.abs(dx) == radius || Math.abs(dz) == radius || random.nextInt(8) == 0 ? secondary : primary;
        setCenterStructureBlock(world, baseX + dx, baseY, baseZ + dz, material, force);
        buildStructureSupport(world, baseX + dx, baseY - 1, baseZ + dz, support, force);
      }
    }
  }

  private void buildStructureSupport(World world, int x, int startY, int z, Material material, boolean force) {
    for (int y = startY; y >= startY - 6; y--) {
      Material existing = world.getBlockAt(x, y, z).getType();
      if (!existing.isAir() && !isReplaceableCenterFeature(existing)) {
        return;
      }
      if (!setCenterStructureBlock(world, x, y, z, material, force)) {
        return;
      }
    }
  }

  private void placeStructureChest(
      World world, int x, int y, int z, CenterAsteroidArchetype archetype, Random random, boolean force) {
    Block block = world.getBlockAt(x, y, z);
    if (!setCenterStructureChestBlock(block, force)) {
      return;
    }

    CenterGeneratorSettings settings = centerGeneratorSettings();
    fillCenterLootChest(
        block,
        random,
        archetype.crateLoot.isEmpty() ? settings.crateLoot : archetype.crateLoot,
        settings.crateShardMin,
        settings.crateShardMax);
  }

  private boolean setCenterStructureChestBlock(Block block, boolean force) {
    String key = locationKey(block.getLocation());
    if (nodeMaterialByKey.containsKey(key) || eventNodes.contains(key) || eventChests.contains(key)) {
      return false;
    }
    if (!force && temporaryBlocks.contains(key)) {
      return false;
    }

    block.setType(Material.CHEST, false);
    centerStructureBlocks.add(key);
    return true;
  }

  private void maybePlaceStructureSpawner(
      World world,
      int x,
      int y,
      int z,
      CenterAsteroidArchetype archetype,
      Random random,
      boolean force,
      double chance) {
    if (archetype.spawnerTypes.isEmpty() || random.nextDouble() >= chance) {
      return;
    }
    if (setCenterStructureBlock(world, x, y, z, Material.SPAWNER, force)) {
      configureCenterSpawner(world.getBlockAt(x, y, z), archetype, random);
    }
  }

  private boolean setCenterStructureBlock(World world, int x, int y, int z, Material material, boolean force) {
    String key = locationKey(world.getName(), x, y, z);
    if (nodeMaterialByKey.containsKey(key)) {
      return false;
    }
    if (!force && temporaryBlocks.contains(key)) {
      return false;
    }

    Block block = world.getBlockAt(x, y, z);
    Material existing = block.getType();
    if (!force && !existing.isAir() && existing != material && !isReplaceableCenterFeature(existing)) {
      return false;
    }

    block.setType(material, false);
    centerStructureBlocks.add(key);
    return true;
  }

  private boolean isReplaceableCenterFeature(Material material) {
    return material == Material.TORCH
        || material == Material.SOUL_TORCH
        || material == Material.STONE_BUTTON
        || material == Material.COBBLESTONE_WALL
        || material == Material.COBBLED_DEEPSLATE_WALL
        || material == Material.IRON_CHAIN
        || material == Material.IRON_BARS
        || material == Material.LANTERN
        || material == Material.SNOW
        || material == Material.WHITE_CANDLE
        || material == Material.AMETHYST_CLUSTER
        || material == Material.LARGE_AMETHYST_BUD
        || material == Material.MEDIUM_AMETHYST_BUD
        || material == Material.SMALL_AMETHYST_BUD
        || material == Material.MAGMA_BLOCK
        || material == Material.CAMPFIRE
        || material == Material.POWDER_SNOW;
  }

  private void addCenterFeatures(
      World world, Asteroid asteroid, int index, boolean force, List<Long> surfaceBlocks) {
    if (surfaceBlocks.isEmpty()) {
      return;
    }

    CenterGeneratorSettings settings = centerGeneratorSettings();
    CenterAsteroidArchetype archetype = asteroid.archetype;
    Random random = new Random(asteroid.seed ^ 0x4F1BBCDCB70A5C23L ^ index * 97L);

    int decorations =
        Math.min(
            settings.maxDecorationsPerAsteroid,
            Math.max(0, (int) Math.round(surfaceBlocks.size() * archetype.decorationChance)));
    for (int i = 0; i < decorations; i++) {
      Long packed = randomSurfaceBlock(random, surfaceBlocks);
      if (packed != null) {
        placeCenterDecoration(world, packed, archetype.decorations.choose(random), force);
      }
    }

    int hazards =
        Math.min(
            settings.maxHazardsPerAsteroid,
            Math.max(0, (int) Math.round(surfaceBlocks.size() * archetype.hazardChance)));
    for (int i = 0; i < hazards; i++) {
      Long packed = randomSurfaceBlock(random, surfaceBlocks);
      if (packed != null) {
        placeCenterHazard(world, packed, archetype.hazardBlocks.choose(random), force);
      }
    }

    if (random.nextDouble() < archetype.ruinChance) {
      Long packed = randomSurfaceBlock(random, surfaceBlocks);
      if (packed != null) {
        placeCenterRuin(world, packed, archetype, random, force);
      }
    } else if (random.nextDouble() < archetype.crateChance) {
      Long packed = randomSurfaceBlock(random, surfaceBlocks);
      if (packed != null) {
        placeCenterCrate(world, packed, archetype, random, force);
      }
    }
  }

  private void placeCenterDecoration(World world, long packed, Material material, boolean force) {
    int x = unpackX(packed);
    int y = unpackY(packed) + 1;
    int z = unpackZ(packed);
    Block block = world.getBlockAt(x, y, z);
    if (canPlaceCenterFeature(block, force)) {
      block.setType(material, false);
    }
  }

  private void placeCenterHazard(World world, long packed, Material material, boolean force) {
    int x = unpackX(packed);
    int y = unpackY(packed);
    int z = unpackZ(packed);
    Block block = world.getBlockAt(x, y, z);
    if (canPlaceCenterFeature(block, force)) {
      block.setType(material, false);
    }
  }

  private void placeCenterCrate(
      World world, long packed, CenterAsteroidArchetype archetype, Random random, boolean force) {
    int x = unpackX(packed);
    int y = unpackY(packed) + 1;
    int z = unpackZ(packed);
    Block block = world.getBlockAt(x, y, z);
    if (!canPlaceCenterFeature(block, force)) {
      return;
    }

    block.setType(Material.CHEST, false);
    CenterGeneratorSettings settings = centerGeneratorSettings();
    fillCenterLootChest(
        block,
        random,
        archetype.crateLoot.isEmpty() ? settings.crateLoot : archetype.crateLoot,
        settings.crateShardMin,
        settings.crateShardMax);
  }

  private void placeCenterRuin(
      World world, long packed, CenterAsteroidArchetype archetype, Random random, boolean force) {
    int baseX = unpackX(packed);
    int baseY = unpackY(packed) + 1;
    int baseZ = unpackZ(packed);

    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
          continue;
        }
        if (Math.abs(dx) + Math.abs(dz) > 3 && random.nextBoolean()) {
          continue;
        }
        Block floor = world.getBlockAt(baseX + dx, baseY, baseZ + dz);
        if (canPlaceCenterFeature(floor, force)) {
          floor.setType(archetype.accentBlocks.choose(random), false);
        }
      }
    }

    for (int[] corner : new int[][] {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}) {
      int height = 2 + random.nextInt(3);
      for (int dy = 1; dy <= height; dy++) {
        Block column = world.getBlockAt(baseX + corner[0], baseY + dy, baseZ + corner[1]);
        if (canPlaceCenterFeature(column, force)) {
          column.setType(archetype.accentBlocks.choose(random), false);
        }
      }
    }

    if (random.nextDouble() < archetype.crateChance) {
      placeCenterCrate(world, packBlock(baseX, baseY, baseZ), archetype, random, force);
    }

    if (random.nextDouble() < archetype.spawnerChance && !archetype.spawnerTypes.isEmpty()) {
      Block spawnerBlock = world.getBlockAt(baseX + 1, baseY + 1, baseZ);
      if (canPlaceCenterFeature(spawnerBlock, force)) {
        spawnerBlock.setType(Material.SPAWNER, false);
        configureCenterSpawner(spawnerBlock, archetype, random);
      }
    }
  }

  private void configureCenterSpawner(Block spawnerBlock, CenterAsteroidArchetype archetype, Random random) {
    if (archetype.spawnerTypes.isEmpty()
        || !(spawnerBlock.getState() instanceof org.bukkit.block.CreatureSpawner spawner)) {
      return;
    }
    spawner.setSpawnedType(archetype.spawnerTypes.get(random.nextInt(archetype.spawnerTypes.size())));
    spawner.setDelay(160 + random.nextInt(160));
    spawner.setMinSpawnDelay(220);
    spawner.setMaxSpawnDelay(520);
    spawner.setSpawnCount(2);
    spawner.setMaxNearbyEntities(6);
    spawner.setRequiredPlayerRange(12);
    spawner.update(true, false);
  }

  private boolean canPlaceCenterFeature(Block block, boolean force) {
    String key = locationKey(block.getLocation());
    if (nodeMaterialByKey.containsKey(key) || centerStructureBlocks.contains(key)) {
      return false;
    }
    return block.getType().isAir() || force;
  }

  private boolean isAsteroidSurface(Asteroid asteroid, int x, int y, int z) {
    return insideAsteroid(asteroid, x, y, z)
        && (!insideAsteroid(asteroid, x + 1, y, z)
            || !insideAsteroid(asteroid, x - 1, y, z)
            || !insideAsteroid(asteroid, x, y + 1, z)
            || !insideAsteroid(asteroid, x, y - 1, z)
            || !insideAsteroid(asteroid, x, y, z + 1)
            || !insideAsteroid(asteroid, x, y, z - 1));
  }

  private AsteroidBlocks collectAsteroidBlocks(Asteroid asteroid) {
    List<Long> solidBlocks = new ArrayList<>();
    Set<Long> solidBlockSet = new HashSet<>();
    for (int x = asteroid.x - asteroid.radiusX; x <= asteroid.x + asteroid.radiusX; x++) {
      for (int y = asteroid.y - asteroid.radiusY; y <= asteroid.y + asteroid.radiusY; y++) {
        for (int z = asteroid.z - asteroid.radiusZ; z <= asteroid.z + asteroid.radiusZ; z++) {
          if (!insideAsteroid(asteroid, x, y, z)) {
            continue;
          }
          long packed = packBlock(x, y, z);
          solidBlocks.add(packed);
          solidBlockSet.add(packed);
        }
      }
    }

    List<Long> surfaceBlocks = new ArrayList<>();
    for (long packed : solidBlocks) {
      int x = unpackX(packed);
      int y = unpackY(packed);
      int z = unpackZ(packed);
      if (!solidBlockSet.contains(packBlock(x + 1, y, z))
          || !solidBlockSet.contains(packBlock(x - 1, y, z))
          || !solidBlockSet.contains(packBlock(x, y + 1, z))
          || !solidBlockSet.contains(packBlock(x, y - 1, z))
          || !solidBlockSet.contains(packBlock(x, y, z + 1))
          || !solidBlockSet.contains(packBlock(x, y, z - 1))) {
        surfaceBlocks.add(packed);
      }
    }

    return new AsteroidBlocks(solidBlocks, surfaceBlocks);
  }

  private Material surfaceNodeMaterial(Asteroid asteroid, int x, int y, int z) {
    double dx = x - asteroid.x;
    double dz = z - asteroid.z;
    double localX = dx * asteroid.cosYaw - dz * asteroid.sinYaw;
    double localZ = dx * asteroid.sinYaw + dz * asteroid.cosYaw;
    double localY = y - asteroid.y;
    double nx = localX / asteroid.radiusX;
    double ny = localY / asteroid.radiusY;
    double nz = localZ / asteroid.radiusZ;
    double distance = Math.sqrt(nx * nx + ny * ny + nz * nz);
    if (distance <= 0.0001) {
      return null;
    }
    nx /= distance;
    ny /= distance;
    nz /= distance;

    for (OrePatch patch : asteroid.orePatches) {
      double dot = nx * patch.x + ny * patch.y + nz * patch.z;
      if (dot < patch.minDot) {
        continue;
      }
      double centerStrength = (dot - patch.minDot) / (1.0 - patch.minDot);
      double ragged =
          fbm(x * 0.18 + patch.salt, y * 0.18 - patch.salt, z * 0.18, 3, patch.salt);
      int speckle = Math.floorMod(hash(x, y, z, patch.salt), 100);
      if (centerStrength * 0.85 + ragged * 0.35 > 0.46 && speckle < patch.density) {
        return patch.material;
      }
    }
    return null;
  }

  private void registerCenterNode(World world, int x, int y, int z, Material node, boolean force) {
    String key = locationKey(world.getName(), x, y, z);
    MinedNode mined = minedNodes.get(key);
    if (mined != null && mined.respawnAtMillis > System.currentTimeMillis()) {
      nodeMaterialByKey.put(key, mined.material);
      world.getBlockAt(x, y, z).setType(centerStoneMaterial(x, y, z), false);
      return;
    }

    Material material = mined == null ? node : mined.material;
    nodeMaterialByKey.put(key, material);
    minedNodes.remove(key);
    if (force || world.getBlockAt(x, y, z).getType() != material) {
      world.getBlockAt(x, y, z).setType(material, false);
    }
  }

  private void applyMinedNodeState() {
    applyMinedNodeState(minedNodes, nodeMaterialByKey, this::scheduleNodeRespawn, this::respawnCenterNode);
  }

  private void applyMinedNodeState(
      Map<String, MinedNode> nodeStates,
      Map<String, Material> activeNodes,
      BiConsumer<String, Long> scheduler,
      BiConsumer<String, Material> respawner) {
    long now = System.currentTimeMillis();
    List<String> ready = new ArrayList<>();
    for (Map.Entry<String, MinedNode> entry : nodeStates.entrySet()) {
      String key = entry.getKey();
      MinedNode mined = entry.getValue();
      if (mined.respawnAtMillis <= now) {
        ready.add(key);
        continue;
      }
      scheduler.accept(key, mined.respawnAtMillis - now);
    }

    for (String key : ready) {
      MinedNode mined = nodeStates.remove(key);
      if (activeNodes.containsKey(key)) {
        respawner.accept(key, mined.material);
      }
    }
    if (!ready.isEmpty()) {
      saveData();
    }
  }

  private void mineCenterNode(BlockBreakEvent event, String key, Material nodeMaterial) {
    event.setCancelled(true);
    Block block = event.getBlock();
    Material replacement = eventNodes.contains(key) ? Material.AIR : centerStoneMaterial(block.getX(), block.getY(), block.getZ());
    block.setType(replacement, false);

    dropItemsNaturally(block, nodeDrops(nodeMaterial));

    long respawnAt = System.currentTimeMillis() + getConfig().getLong("node-respawn-seconds", 900L) * 1000L;
    Material respawnMaterial = eventNodes.contains(key) ? nodeMaterial : randomRespawnNodeMaterial(block, nodeMaterial);
    minedNodes.put(key, new MinedNode(respawnMaterial, respawnAt));
    saveData();
    scheduleNodeRespawn(key, respawnAt - System.currentTimeMillis());
  }

  private boolean isDepletedNode(String key) {
    return isDepletedNode(minedNodes, key);
  }

  private void scheduleNodeRespawn(String key, long delayMillis) {
    scheduleMinedNodeRespawn(key, delayMillis, minedNodes, nodeMaterialByKey, this::respawnCenterNode, true);
  }

  private void scheduleMinedNodeRespawn(
      String key,
      long delayMillis,
      Map<String, MinedNode> nodeStates,
      Map<String, Material> activeNodes,
      BiConsumer<String, Material> respawner,
      boolean requireLoadedBlock) {
    long ticks = Math.max(20L, delayMillis / 50L);
    Bukkit.getScheduler()
        .runTaskLater(
            this,
            () -> {
              MinedNode mined = nodeStates.get(key);
              if (mined == null || mined.respawnAtMillis > System.currentTimeMillis()) {
                return;
              }
              if (activeNodes.containsKey(key) && (!requireLoadedBlock || blockFromKey(key) != null)) {
                respawner.accept(key, mined.material);
              }
              nodeStates.remove(key);
              saveData();
            },
            ticks);
  }

  private void respawnCenterNode(String key, Material material) {
    respawnNode(key, material, nodeMaterialByKey);
  }

  private Material randomRespawnNodeMaterial(Block block, Material previousMaterial) {
    Asteroid asteroid = asteroidAt(block.getX(), block.getY(), block.getZ());
    int tier = asteroid == null ? 1 : asteroid.tier;
    ThreadLocalRandom random = ThreadLocalRandom.current();
    if (asteroid != null) {
      for (int attempt = 0; attempt < 12; attempt++) {
        Material material = asteroid.archetype.nodeMaterials.choose(random);
        if (material != previousMaterial) {
          return material;
        }
      }
    }
    for (int attempt = 0; attempt < 12; attempt++) {
      Material material = patchNodeMaterial(tier, random.nextInt(64), random);
      if (material != previousMaterial) {
        return material;
      }
    }

    List<Material> materials = nodeMaterialsForTier(tier);
    if (materials.size() <= 1) {
      return previousMaterial;
    }

    Material material = previousMaterial;
    while (material == previousMaterial) {
      material = materials.get(random.nextInt(materials.size()));
    }
    return material;
  }

  private List<Material> nodeMaterialsForTier(int tier) {
    List<Material> materials = new ArrayList<>();
    if (tier >= 4) {
      materials.add(Material.EMERALD_ORE);
    }
    if (tier >= 3) {
      materials.add(Material.DIAMOND_ORE);
    }
    if (tier >= 2) {
      materials.add(Material.GOLD_ORE);
    }
    materials.add(Material.REDSTONE_ORE);
    materials.add(Material.LAPIS_ORE);
    materials.add(Material.IRON_ORE);
    materials.add(Material.COPPER_ORE);
    materials.add(Material.COAL_ORE);
    return materials;
  }

  private List<ItemStack> nodeDrops(Material material) {
    List<ItemStack> drops = new ArrayList<>();
    switch (material) {
      case COAL_ORE, DEEPSLATE_COAL_ORE -> drops.add(new ItemStack(Material.COAL, 2));
      case COPPER_ORE, DEEPSLATE_COPPER_ORE -> drops.add(new ItemStack(Material.RAW_COPPER, 4));
      case IRON_ORE, DEEPSLATE_IRON_ORE -> drops.add(new ItemStack(Material.RAW_IRON, 2));
      case GOLD_ORE, DEEPSLATE_GOLD_ORE -> drops.add(new ItemStack(Material.RAW_GOLD, 2));
      case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> drops.add(new ItemStack(Material.REDSTONE, 5));
      case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> drops.add(new ItemStack(Material.LAPIS_LAZULI, 4));
      case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> drops.add(new ItemStack(Material.EMERALD, 1));
      case ANCIENT_DEBRIS -> {
        drops.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
        drops.add(centerShard(3));
      }
      case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> {
        drops.add(new ItemStack(Material.DIAMOND, 1));
        drops.add(centerShard(2));
      }
      default -> drops.add(new ItemStack(material, 1));
    }
    if (material != Material.DIAMOND_ORE
        && material != Material.DEEPSLATE_DIAMOND_ORE
        && material != Material.ANCIENT_DEBRIS) {
      drops.add(centerShard(1));
    }
    return drops;
  }

  private void applyNetherMinedNodeState() {
    applyMinedNodeState(netherMinedNodes, netherNodeMaterialByKey, this::scheduleNetherNodeRespawn, this::respawnNetherNode);
  }

  private boolean isDepletedNetherNode(String key) {
    return isDepletedNode(netherMinedNodes, key);
  }

  private void mineNetherNode(BlockBreakEvent event, String key, Material nodeMaterial) {
    event.setCancelled(true);
    Block block = event.getBlock();
    block.setType(netherPlaceholderMaterial(nodeMaterial), false);

    boolean hotspot = isInNetherHotspot(block.getLocation());
    dropItemsNaturally(block, netherNodeDrops(nodeMaterial, hotspot));

    long seconds =
        nodeMaterial == Material.ANCIENT_DEBRIS
            ? getConfig().getLong("nether-ancient-debris-respawn-seconds", 7200L)
            : getConfig().getLong("nether-node-respawn-seconds", 1200L);
    long respawnAt = System.currentTimeMillis() + seconds * 1000L;
    netherMinedNodes.put(key, new MinedNode(nodeMaterial, respawnAt));
    saveData();
    scheduleNetherNodeRespawn(key, respawnAt - System.currentTimeMillis());
  }

  private void scheduleNetherNodeRespawn(String key, long delayMillis) {
    scheduleMinedNodeRespawn(key, delayMillis, netherMinedNodes, netherNodeMaterialByKey, this::respawnNetherNode, false);
  }

  private void respawnNetherNode(String key, Material material) {
    respawnNode(key, material, netherNodeMaterialByKey);
  }

  private void respawnNode(String key, Material material, Map<String, Material> activeNodes) {
    Block block = blockFromKey(key);
    if (block != null) {
      block.setType(material, false);
    }
    activeNodes.put(key, material);
  }

  private boolean isDepletedNode(Map<String, MinedNode> nodeStates, String key) {
    MinedNode mined = nodeStates.get(key);
    return mined != null && mined.respawnAtMillis > System.currentTimeMillis();
  }

  private void dropItemsNaturally(Block block, List<ItemStack> drops) {
    Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
    for (ItemStack drop : drops) {
      block.getWorld().dropItemNaturally(dropLocation, drop);
    }
  }

  private Material netherPlaceholderMaterial(Material material) {
    return switch (material) {
      case ANCIENT_DEBRIS, GILDED_BLACKSTONE -> Material.BLACKSTONE;
      case GLOWSTONE -> Material.BASALT;
      case SOUL_SAND -> Material.SOUL_SOIL;
      case MAGMA_BLOCK -> Material.NETHERRACK;
      default -> Material.NETHERRACK;
    };
  }

  private List<ItemStack> netherNodeDrops(Material material, boolean hotspot) {
    int bonus = hotspot && material != Material.ANCIENT_DEBRIS ? 2 : 1;
    List<ItemStack> drops = new ArrayList<>();
    switch (material) {
      case NETHER_QUARTZ_ORE -> drops.add(new ItemStack(Material.QUARTZ, 3 * bonus));
      case GLOWSTONE -> drops.add(new ItemStack(Material.GLOWSTONE_DUST, 4 * bonus));
      case SOUL_SAND -> {
        drops.add(new ItemStack(Material.SOUL_SAND, 1));
        drops.add(new ItemStack(Material.NETHER_WART, 2 * bonus));
      }
      case MAGMA_BLOCK -> {
        drops.add(new ItemStack(Material.MAGMA_BLOCK, 1));
        drops.add(new ItemStack(Material.MAGMA_CREAM, bonus));
      }
      case BLACKSTONE -> drops.add(new ItemStack(Material.BLACKSTONE, 4 * bonus));
      case GILDED_BLACKSTONE -> {
        drops.add(new ItemStack(Material.BLACKSTONE, 2 * bonus));
        drops.add(new ItemStack(Material.GOLD_NUGGET, 6 * bonus));
      }
      case CRIMSON_STEM -> {
        drops.add(new ItemStack(Material.CRIMSON_STEM, 2 * bonus));
        drops.add(new ItemStack(Material.CRIMSON_FUNGUS, bonus));
      }
      case ANCIENT_DEBRIS -> drops.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
      default -> drops.add(new ItemStack(material, bonus));
    }
    return drops;
  }

  private ItemStack centerShard(int amount) {
    ItemStack shard = new ItemStack(SHARD_MATERIAL, amount);
    ItemMeta meta = shard.getItemMeta();
    if (meta != null) {
      meta.displayName(
          Component.text("Center Shard", NamedTextColor.AQUA)
              .decorate(TextDecoration.BOLD)
              .decoration(TextDecoration.ITALIC, false));
      meta.lore(
          List.of(
              Component.text("Earned from asteroid veins.", NamedTextColor.GRAY)
                  .decoration(TextDecoration.ITALIC, false),
              Component.text("Spend it on generator upgrades.", NamedTextColor.DARK_GRAY)
                  .decoration(TextDecoration.ITALIC, false)));
      meta.getPersistentDataContainer().set(shardKey, PersistentDataType.INTEGER, 1);
      shard.setItemMeta(meta);
    }
    return shard;
  }

  private boolean isCenterShard(ItemStack item) {
    if (item == null || item.getType() != SHARD_MATERIAL || !item.hasItemMeta()) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(shardKey, PersistentDataType.INTEGER);
  }

  private void startEvent(CommandSender sender) {
    if (eventActive) {
      sendWarning(sender, "A center event is already active.");
      return;
    }

    if (!eventNodes.isEmpty() || !eventChests.isEmpty()) {
      clearEventContent();
    }

    eventActive = true;
    eventEndsAtMillis =
        System.currentTimeMillis() + getConfig().getInt("event-duration-minutes", 20) * 60_000L;
    spawnEventContent();
    saveData();
    broadcast(uiLine("\u00a7e", "Center event started. Bonus nodes and crates are live in the asteroid field."));
    resumeEvent();
  }

  private void resumeEvent() {
    if (!eventActive) {
      return;
    }

    long remainingMillis = eventEndsAtMillis - System.currentTimeMillis();
    long scheduledEndsAt = eventEndsAtMillis;
    if (remainingMillis <= 0) {
      stopEvent(Bukkit.getConsoleSender(), false);
      return;
    }

    if (remainingMillis > 5 * 60_000L) {
      Bukkit.getScheduler()
          .runTaskLater(
            this,
            () -> {
                if (eventActive && eventEndsAtMillis == scheduledEndsAt) {
                  broadcast(uiLine("\u00a7e", "Center event ends in 5 minutes."));
                }
              },
              (remainingMillis - 5 * 60_000L) / 50L);
    }

    Bukkit.getScheduler()
        .runTaskLater(
            this,
            () -> {
              if (eventActive
                  && eventEndsAtMillis == scheduledEndsAt
                  && System.currentTimeMillis() >= eventEndsAtMillis) {
                stopEvent(Bukkit.getConsoleSender(), false);
              }
            },
            Math.max(20L, remainingMillis / 50L));
  }

  private void stopEvent(CommandSender sender, boolean manual) {
    if (!eventActive && eventNodes.isEmpty() && eventChests.isEmpty()) {
      sendWarning(sender, "No center event is active.");
      return;
    }

    clearEventContent();
    cleanupTemporaryBlocks();
    saveData();
    broadcast(uiLine("\u00a7e", manual ? "Center event stopped." : "Center event ended."));
  }

  private void clearEventContent() {
    eventActive = false;
    eventEndsAtMillis = 0L;

    for (String key : new ArrayList<>(eventNodes)) {
      Material eventMaterial = nodeMaterialByKey.remove(key);
      boolean wasMined = minedNodes.remove(key) != null;
      Block block = blockFromKey(key);
      if (block != null && (wasMined || eventMaterial == null || block.getType() == eventMaterial)) {
        clearBlock(block);
      }
    }
    eventNodes.clear();

    for (String key : new ArrayList<>(eventChests)) {
      Block block = blockFromKey(key);
      if (block != null && block.getType() == Material.CHEST) {
        clearBlock(block);
      }
    }
    eventChests.clear();
  }

  private void spawnEventContent() {
    CenterGeneratorSettings settings = centerGeneratorSettings();
    Random random = new Random(System.currentTimeMillis());

    placeEventContent(
        settings.eventNodeCount,
        32,
        random,
        block -> {
          String key = locationKey(block.getLocation());
          Material material = settings.eventNodeMaterials.choose(random);
          block.setType(material, false);
          eventNodes.add(key);
          nodeMaterialByKey.put(key, material);
        });

    placeEventContent(
        settings.eventCrateCount,
        24,
        random,
        block -> {
          String key = locationKey(block.getLocation());
          block.setType(Material.CHEST, false);
          fillEventChest(block);
          eventChests.add(key);
        });
  }

  private void placeEventContent(
      int targetCount, int minimumAttempts, Random random, Consumer<Block> placer) {
    for (int placed = 0, attempts = 0;
        placed < targetCount && attempts < Math.max(minimumAttempts, targetCount * 16);
        attempts++) {
      Block block = randomAvailableEventContentBlock(random);
      if (block == null) {
        continue;
      }
      placer.accept(block);
      placed++;
    }
  }

  private Block randomAvailableEventContentBlock(Random random) {
    Location surface = randomCenterSurface(random);
    if (surface == null) {
      return null;
    }

    Block block = surface.getBlock();
    String key = locationKey(block.getLocation());
    return isAvailableEventContentBlock(block, key) ? block : null;
  }

  private boolean isAvailableEventContentBlock(Block block, String key) {
    return block.getType().isAir()
        && !nodeMaterialByKey.containsKey(key)
        && !eventNodes.contains(key)
        && !eventChests.contains(key)
        && !temporaryBlocks.contains(key)
        && !centerStructureBlocks.contains(key);
  }

  private Location randomCenterSurface(Random random) {
    World world = getOverworld();
    int radius = Math.max(1, getConfig().getInt("center-radius", 160) - 24);
    int centerY = getConfig().getInt("center-y", 92);
    for (int tries = 0; tries < 120; tries++) {
      int x = random.nextInt(radius * 2 + 1) - radius;
      int z = random.nextInt(radius * 2 + 1) - radius;
      for (int y = centerY + 48; y > centerY - 44; y--) {
        Block base = world.getBlockAt(x, y, z);
        Block above = world.getBlockAt(x, y + 1, z);
        if (base.getType().isSolid() && above.getType().isAir()) {
          return above.getLocation();
        }
      }
    }
    return null;
  }

  private void fillEventChest(Block block) {
    CenterGeneratorSettings settings = centerGeneratorSettings();
    Random random =
        new Random(hash(block.getX(), block.getY(), block.getZ(), settings.eventCrateSalt));
    fillCenterLootChest(
        block,
        random,
        settings.eventCrateLoot,
        settings.eventCrateShardMin,
        settings.eventCrateShardMax);
  }

  private Material eventNodeMaterial(Block block) {
    CenterGeneratorSettings settings = centerGeneratorSettings();
    return settings.eventNodeMaterials.choose(
        Math.floorMod(hash(block.getX(), block.getY(), block.getZ(), settings.eventCrateSalt + 17), Integer.MAX_VALUE));
  }

  private void fillCenterLootChest(
      Block block, Random random, Map<Material, Integer> loot, int shardMin, int shardMax) {
    if (!(block.getState() instanceof Chest chest)) {
      return;
    }

    Inventory inventory = chest.getBlockInventory();
    inventory.clear();
    int min = Math.min(shardMin, shardMax);
    int max = Math.max(shardMin, shardMax);
    if (max > 0) {
      int amount = min + random.nextInt(max - min + 1);
      placeRandomChestItem(inventory, random, centerShard(amount));
    }

    for (Map.Entry<Material, Integer> entry : loot.entrySet()) {
      int amount = Math.max(1, Math.min(entry.getValue(), entry.getKey().getMaxStackSize()));
      placeRandomChestItem(inventory, random, new ItemStack(entry.getKey(), amount));
    }
    chest.update(true, false);
  }

  private void placeRandomChestItem(Inventory inventory, Random random, ItemStack item) {
    for (int tries = 0; tries < inventory.getSize() * 2; tries++) {
      int slot = random.nextInt(inventory.getSize());
      if (inventory.getItem(slot) == null) {
        inventory.setItem(slot, item);
        return;
      }
    }
  }

  private void resetCenter() {
    clearEventContent();
    cleanupTemporaryBlocks();
    minedNodes.clear();
    nodeMaterialByKey.clear();
    cachedCenterAsteroidSeed = Long.MIN_VALUE;
    cachedCenterGeneratorSignature = Integer.MIN_VALUE;
    clearCenterArea();
    ensureCenter(true);
    if (endUnlocked) {
      createEndPortalAtCenter();
    }
    saveData();
  }

  private void cleanupTemporaryBlocks() {
    for (String key : new ArrayList<>(temporaryBlocks)) {
      clearBlock(blockFromKey(key));
    }
    temporaryBlocks.clear();
  }

  private void clearCenterArea() {
    World world = getOverworld();
    int centerY = getConfig().getInt("center-y", 92);
    int radius = getConfig().getInt("center-radius", 160) + 24;
    int radiusSquared = radius * radius;
    int minY = centerY - 72;
    int maxY = centerY + 88;
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (x * x + z * z > radiusSquared) {
          continue;
        }
        for (int y = minY; y <= maxY; y++) {
          clearBlock(world.getBlockAt(x, y, z));
        }
      }
    }
  }

  private int wipeAllIslands() {
    World world = getOverworld();
    int wiped = islands.size();
    for (Island island : new ArrayList<>(islands.values())) {
      clearIslandArea(world, island);
    }
    islands.clear();
    teams.clear();
    teamByMember.clear();
    pendingTeamInvites.clear();
    lastSafeLocations.clear();
    nextIslandSlot = 0;
    saveData();
    updateAllPlayerTabNames();
    return wiped;
  }

  private void clearIslandArea(World world, Island island) {
    int y = getConfig().getInt("island-y", 88);
    int radius = 64;
    for (int x = island.x - radius; x <= island.x + radius; x++) {
      for (int z = island.z - radius; z <= island.z + radius; z++) {
        for (int blockY = y - 8; blockY <= y + 48; blockY++) {
          clearBlock(world.getBlockAt(x, blockY, z));
        }
      }
    }
  }

  private void clearBlock(Block block) {
    if (block != null && !block.getType().isAir()) {
      block.setType(Material.AIR, false);
    }
  }

  private void handleIslandCommand(CommandSender sender, String[] args) {
    if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
      if (!requireAdmin(sender)) {
        return;
      }
      sender.sendMessage("\u00a78---------- \u00a76\u00a7lSkyblock islands \u00a78----------");
      for (Island island : islands.values()) {
        sender.sendMessage(
            "\u00a7e- "
                + island.ownerName
                + " \u00a77slot "
                + island.slot
                + " at "
                + islandCoordinateText(island));
      }
      return;
    }

    if (args.length >= 2 && args[1].equalsIgnoreCase("wipe")) {
      if (!requireAdmin(sender)) {
        return;
      }
      int wiped = wipeAllIslands();
      sendSuccess(sender, "Wiped " + wiped + " island(s) and reset island assignment.");
      return;
    }

    if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
      if (!requireAdmin(sender)) {
        return;
      }
      PlayerIdentity target = resolvePlayerIdentity(args[2]);
      if (target == null) {
        sendError(sender, "That player has not joined yet. Ask them to join once, then create their island.");
        return;
      }
      if (teamByMember.containsKey(target.uuid)) {
        sendError(sender, "That player is on a team and already shares the team island.");
        return;
      }
      Island island = createIsland(target.uuid, target.name);
      sendSuccess(sender, "Created island for " + island.ownerName + " at " + islandCoordinateText(island) + ".");
      Player onlineTarget = Bukkit.getPlayer(target.uuid);
      if (onlineTarget != null) {
        onlineTarget.teleport(islandSpawnLocation(island));
      }
      return;
    }

    if (!(sender instanceof Player player)) {
      sendWarning(sender, "Usage: /msb island list");
      return;
    }

    Island island = getOrCreateEffectiveIsland(player);
    sendInfo(player, "Your island is at " + islandCoordinateText(island) + ".");
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team != null) {
      sendInfo(player, "Team: " + team.name + " (" + playerName(team.leaderId) + "'s shared island).");
    }
    sendInfo(player, "Physical travel only: bridge, fly, portal, or walk to visit friends.");
  }

  private void handleSetSpawnCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sendError(sender, "Only players can set an island spawnpoint.");
      return;
    }
    if (args.length > 0) {
      sendWarning(player, "Usage: /setspawn");
      return;
    }

    Location location = player.getLocation();
    if (!isSkyblockOverworld(location)) {
      sendError(player, "You can only set your island spawnpoint in the Skyblock overworld.");
      return;
    }

    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team != null && !team.isLeader(player.getUniqueId())) {
      sendError(player, "Only the team leader can set the shared team spawnpoint.");
      return;
    }

    Island island = getOrCreateEffectiveIsland(player);

    Island nearest = nearestIsland(location, 96);
    if (nearest == null || !nearest.ownerId.equals(island.ownerId)) {
      sendError(player, "Stand on your own island to set its spawnpoint.");
      return;
    }

    island.spawnPoint = SpawnPoint.fromLocation(location);
    saveData();
    sendSuccess(
        player,
        (team == null ? "Island" : "Team") + " spawnpoint set to " + spawnCoordinateText(location) + ".");
  }

  private void handleTeamCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sendError(sender, "Only players can manage Skyblock teams.");
      return;
    }
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      sendTeamHelp(player);
      return;
    }

    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "create":
        handleTeamCreate(player, args);
        return;
      case "invite":
        handleTeamInvite(player, args);
        return;
      case "accept":
        handleTeamAccept(player, args);
        return;
      case "decline":
        handleTeamDecline(player, args);
        return;
      case "leave":
        handleTeamLeave(player, args);
        return;
      case "kick":
        handleTeamKick(player, args);
        return;
      case "rename":
        handleTeamRename(player, args);
        return;
      case "disband":
        handleTeamDisband(player, args);
        return;
      case "info":
        handleTeamInfo(player);
        return;
      default:
        sendTeamHelp(player);
    }
  }

  private List<String> completeTeamCommand(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return filter(TEAM_SUBCOMMANDS, args[0]);
    }
    if (!(sender instanceof Player player) || args.length != 2) {
      return Collections.emptyList();
    }

    String subcommand = args[0].toLowerCase(Locale.ROOT);
    if (subcommand.equals("invite")) {
      List<String> names =
          Bukkit.getOnlinePlayers().stream()
              .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
              .filter(target -> !teamByMember.containsKey(target.getUniqueId()))
              .map(Player::getName)
              .collect(Collectors.toList());
      return filter(names, args[1]);
    }
    if (subcommand.equals("kick")) {
      SkyblockTeam team = teamByMember.get(player.getUniqueId());
      if (team == null || !team.isLeader(player.getUniqueId())) {
        return Collections.emptyList();
      }
      List<String> names =
          team.memberIds.stream()
              .filter(memberId -> !memberId.equals(player.getUniqueId()))
              .map(this::playerName)
              .collect(Collectors.toList());
      return filter(names, args[1]);
    }
    return Collections.emptyList();
  }

  private void handleTeamCreate(Player player, String[] args) {
    if (args.length < 2) {
      sendWarning(player, "Usage: /team create <name>");
      return;
    }
    if (teamByMember.containsKey(player.getUniqueId())) {
      sendError(player, "You are already on a team.");
      return;
    }

    String name = cleanTeamName(joinArguments(args, 1));
    if (!isValidTeamName(name)) {
      sendError(player, "Team names must be 3-24 letters, numbers, spaces, or hyphens.");
      return;
    }
    String normalizedName = normalizeTeamName(name);
    if (teams.containsKey(normalizedName)) {
      sendError(player, "A team with that name already exists.");
      return;
    }

    getOrCreateEffectiveIsland(player);

    SkyblockTeam team =
        new SkyblockTeam(name, normalizedName, player.getUniqueId(), new ArrayList<>(Collections.singletonList(player.getUniqueId())));
    teams.put(normalizedName, team);
    teamByMember.put(player.getUniqueId(), team);
    pendingTeamInvites.remove(player.getUniqueId());
    saveData();
    updatePlayerTabName(player);
    sendSuccess(player, "Created team " + team.name + " around your island.");
  }

  private void handleTeamInvite(Player player, String[] args) {
    if (args.length != 2) {
      sendWarning(player, "Usage: /team invite <player>");
      return;
    }

    SkyblockTeam team = requireTeamLeader(player);
    if (team == null) {
      return;
    }

    Player target = Bukkit.getPlayerExact(args[1]);
    if (target == null) {
      sendError(player, "That player must be online to receive a team invite.");
      return;
    }
    if (target.getUniqueId().equals(player.getUniqueId())) {
      sendError(player, "You cannot invite yourself.");
      return;
    }
    if (teamByMember.containsKey(target.getUniqueId())) {
      sendError(player, "That player is already on a team.");
      return;
    }

    pendingTeamInvites.put(target.getUniqueId(), new TeamInvite(team, player.getUniqueId()));
    sendSuccess(player, "Invited " + target.getName() + " to team " + team.name + ".");
    sendInfo(target, player.getName() + " invited you to join team " + team.name + ".");
    sendInfo(target, "Use /team accept or /team decline.");
  }

  private void handleTeamAccept(Player player, String[] args) {
    if (args.length != 1) {
      sendWarning(player, "Usage: /team accept");
      return;
    }
    if (teamByMember.containsKey(player.getUniqueId())) {
      sendError(player, "You are already on a team.");
      pendingTeamInvites.remove(player.getUniqueId());
      return;
    }

    TeamInvite invite = pendingTeamInvites.remove(player.getUniqueId());
    if (invite == null || !teams.containsValue(invite.team)) {
      sendError(player, "You do not have a pending team invite.");
      return;
    }

    Island oldIsland = islands.remove(player.getUniqueId());
    if (oldIsland != null) {
      clearIslandArea(getOverworld(), oldIsland);
    }
    lastSafeLocations.remove(player.getUniqueId());

    invite.team.memberIds.add(player.getUniqueId());
    teamByMember.put(player.getUniqueId(), invite.team);
    Island teamIsland = islands.get(invite.team.leaderId);
    if (teamIsland == null) {
      teamIsland = createIsland(invite.team.leaderId, playerName(invite.team.leaderId));
    }
    saveData();
    player.teleport(islandSpawnLocation(teamIsland));
    updatePlayerTabName(player);
    sendSuccess(player, "Joined team " + invite.team.name + ". Your old island was wiped.");

    Player inviter = Bukkit.getPlayer(invite.inviterId);
    if (inviter != null) {
      sendSuccess(inviter, player.getName() + " joined team " + invite.team.name + ".");
    }
  }

  private void handleTeamDecline(Player player, String[] args) {
    if (args.length != 1) {
      sendWarning(player, "Usage: /team decline");
      return;
    }

    TeamInvite invite = pendingTeamInvites.remove(player.getUniqueId());
    if (invite == null) {
      sendWarning(player, "You do not have a pending team invite.");
      return;
    }

    sendSuccess(player, "Declined the invite to team " + invite.team.name + ".");
    Player inviter = Bukkit.getPlayer(invite.inviterId);
    if (inviter != null) {
      sendWarning(inviter, player.getName() + " declined the team invite.");
    }
  }

  private void handleTeamLeave(Player player, String[] args) {
    if (args.length != 1) {
      sendWarning(player, "Usage: /team leave");
      return;
    }

    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team == null) {
      sendWarning(player, "You are not on a team.");
      return;
    }

    if (team.isLeader(player.getUniqueId())) {
      transferLeaderOut(player, team);
    } else {
      removeMemberToFreshIsland(player.getUniqueId(), player.getName(), team, true);
      sendSuccess(player, "You left team " + team.name + " and received a fresh island.");
      saveData();
    }
  }

  private void handleTeamKick(Player player, String[] args) {
    if (args.length != 2) {
      sendWarning(player, "Usage: /team kick <player>");
      return;
    }

    SkyblockTeam team = requireTeamLeader(player);
    if (team == null) {
      return;
    }

    PlayerIdentity target = resolvePlayerIdentity(args[1]);
    if (target == null) {
      sendError(player, "That player has not joined yet.");
      return;
    }
    if (target.uuid.equals(player.getUniqueId())) {
      sendError(player, "Use /team leave to transfer leadership instead.");
      return;
    }
    if (!team.hasMember(target.uuid)) {
      sendError(player, "That player is not on your team.");
      return;
    }

    removeMemberToFreshIsland(target.uuid, target.name, team, true);
    saveData();
    sendSuccess(player, "Kicked " + target.name + " from team " + team.name + ".");
    Player onlineTarget = Bukkit.getPlayer(target.uuid);
    if (onlineTarget != null) {
      sendWarning(onlineTarget, "You were kicked from team " + team.name + " and received a fresh island.");
    }
  }

  private void handleTeamRename(Player player, String[] args) {
    if (args.length < 2) {
      sendWarning(player, "Usage: /team rename <newName>");
      return;
    }

    SkyblockTeam team = requireTeamLeader(player);
    if (team == null) {
      return;
    }

    String name = cleanTeamName(joinArguments(args, 1));
    if (!isValidTeamName(name)) {
      sendError(player, "Team names must be 3-24 letters, numbers, spaces, or hyphens.");
      return;
    }
    String normalizedName = normalizeTeamName(name);
    if (teams.containsKey(normalizedName) && !team.normalizedName.equals(normalizedName)) {
      sendError(player, "A team with that name already exists.");
      return;
    }

    teams.remove(team.normalizedName);
    team.name = name;
    team.normalizedName = normalizedName;
    teams.put(team.normalizedName, team);
    saveData();
    updateTeamTabNames(team);
    sendSuccess(player, "Renamed your team to " + team.name + ".");
  }

  private void handleTeamDisband(Player player, String[] args) {
    if (args.length != 1) {
      sendWarning(player, "Usage: /team disband");
      return;
    }

    SkyblockTeam team = requireTeamLeader(player);
    if (team == null) {
      return;
    }

    transferLeaderOut(player, team);
  }

  private void handleTeamInfo(Player player) {
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team == null) {
      sendInfo(player, "You are not on a team.");
      return;
    }

    sendInfo(player, "Team: " + team.name);
    sendInfo(player, "Leader: " + playerName(team.leaderId));
    sendInfo(
        player,
        "Members: "
            + team.memberIds.stream().map(this::playerName).collect(Collectors.joining(", ")));
  }

  private void sendTeamHelp(CommandSender sender) {
    sender.sendMessage("\u00a78---------- \u00a76\u00a7lSkyblock Teams \u00a78----------");
    sender.sendMessage("\u00a7e/team create <name> \u00a78- \u00a77Create a team around your island.");
    sender.sendMessage("\u00a7e/team invite <player> \u00a78- \u00a77Invite a player to share your island.");
    sender.sendMessage("\u00a7e/team accept \u00a78- \u00a77Join the team that invited you.");
    sender.sendMessage("\u00a7e/team decline \u00a78- \u00a77Decline your pending team invite.");
    sender.sendMessage("\u00a7e/team leave \u00a78- \u00a77Leave your team and get a fresh island.");
    sender.sendMessage("\u00a7e/team kick <player> \u00a78- \u00a77Remove a team member.");
    sender.sendMessage("\u00a7e/team rename <name> \u00a78- \u00a77Rename your team.");
    sender.sendMessage("\u00a7e/team disband \u00a78- \u00a77Transfer leadership out of the team.");
    sender.sendMessage("\u00a7e/team info \u00a78- \u00a77Show team members.");
  }

  private SkyblockTeam requireTeamLeader(Player player) {
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team == null) {
      sendError(player, "You are not on a team.");
      return null;
    }
    if (!team.isLeader(player.getUniqueId())) {
      sendError(player, "Only the team leader can do that.");
      return null;
    }
    return team;
  }

  private void transferLeaderOut(Player player, SkyblockTeam team) {
    UUID oldLeaderId = player.getUniqueId();
    if (!team.isLeader(oldLeaderId)) {
      sendError(player, "Only the team leader can do that.");
      return;
    }

    if (team.memberIds.size() <= 1) {
      removeTeam(team);
      pendingTeamInvites.entrySet().removeIf(entry -> entry.getValue().team == team);
      saveData();
      updatePlayerTabName(player);
      sendSuccess(player, "Removed team " + team.name + ". You kept the island as your solo island.");
      return;
    }

    team.memberIds.remove(oldLeaderId);
    teamByMember.remove(oldLeaderId);
    UUID newLeaderId = team.memberIds.get(0);
    transferTeamIslandOwnership(team, oldLeaderId, newLeaderId);
    team.leaderId = newLeaderId;
    teamByMember.put(newLeaderId, team);
    Island freshIsland = recreateFreshIsland(oldLeaderId, player.getName());
    pendingTeamInvites.remove(oldLeaderId);
    pendingTeamInvites.entrySet().removeIf(entry -> entry.getValue().team == team);
    saveData();

    player.teleport(islandSpawnLocation(freshIsland));
    updatePlayerTabName(player);
    updateTeamTabNames(team);
    sendSuccess(player, "Leadership transferred to " + playerName(newLeaderId) + ". You received a fresh island.");

    Player newLeader = Bukkit.getPlayer(newLeaderId);
    if (newLeader != null) {
      sendSuccess(newLeader, "You are now the leader of team " + team.name + ".");
    }
  }

  private void transferTeamIslandOwnership(SkyblockTeam team, UUID oldLeaderId, UUID newLeaderId) {
    World world = getOverworld();
    Island sharedIsland = islands.remove(oldLeaderId);
    Island staleNewLeaderIsland = islands.remove(newLeaderId);
    if (staleNewLeaderIsland != null) {
      clearIslandArea(world, staleNewLeaderIsland);
    }
    if (sharedIsland == null) {
      islands.put(newLeaderId, createIsland(newLeaderId, playerName(newLeaderId)));
      return;
    }

    Island transferred =
        new Island(
            newLeaderId,
            playerName(newLeaderId),
            sharedIsland.slot,
            sharedIsland.x,
            sharedIsland.z,
            sharedIsland.upgrades,
            sharedIsland.spawnPoint);
    islands.put(newLeaderId, transferred);
    lastSafeLocations.remove(oldLeaderId);
    team.leaderId = newLeaderId;
  }

  private void removeMemberToFreshIsland(UUID memberId, String memberName, SkyblockTeam team, boolean teleportIfOnline) {
    team.memberIds.remove(memberId);
    teamByMember.remove(memberId);
    pendingTeamInvites.remove(memberId);
    Island freshIsland = recreateFreshIsland(memberId, memberName);
    Player onlineMember = Bukkit.getPlayer(memberId);
    if (onlineMember != null) {
      if (teleportIfOnline) {
        onlineMember.teleport(islandSpawnLocation(freshIsland));
      }
      updatePlayerTabName(onlineMember);
    }
  }

  private Island recreateFreshIsland(UUID playerId, String playerName) {
    Island staleIsland = islands.remove(playerId);
    if (staleIsland != null) {
      clearIslandArea(getOverworld(), staleIsland);
    }
    lastSafeLocations.remove(playerId);
    return createIsland(playerId, playerName);
  }

  private void removeTeam(SkyblockTeam team) {
    teams.remove(team.normalizedName);
    for (UUID memberId : new ArrayList<>(team.memberIds)) {
      teamByMember.remove(memberId);
    }
  }

  private boolean isValidTeamName(String name) {
    return name != null && TEAM_NAME_PATTERN.matcher(cleanTeamName(name)).matches();
  }

  private String normalizeTeamName(String name) {
    return cleanTeamName(name).toLowerCase(Locale.ROOT);
  }

  private static String cleanTeamName(String name) {
    if (name == null) {
      return "";
    }
    return name.trim().replace('_', ' ').replaceAll("\\s+", " ");
  }

  private static String joinArguments(String[] args, int startIndex) {
    if (startIndex >= args.length) {
      return "";
    }
    return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
  }

  private String playerName(UUID playerId) {
    Player onlinePlayer = Bukkit.getPlayer(playerId);
    if (onlinePlayer != null) {
      return onlinePlayer.getName();
    }
    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
    String name = offlinePlayer.getName();
    return name == null ? playerId.toString().substring(0, 8) : name;
  }

  private void handleUpgradesCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sendError(sender, "Only players can use generator upgrades.");
      return;
    }

    Island island = getOrCreateEffectiveIsland(player);

    if (args.length >= 3 && args[1].equalsIgnoreCase("buy")) {
      buyUpgrade(player, island, joinArguments(args, 2));
      return;
    }

    openUpgradeMenu(player, island);
  }

  private void openUpgradeMenu(Player player, Island island) {
    UpgradeMenuHolder holder = new UpgradeMenuHolder(island.ownerId);
    Inventory inventory =
        Bukkit.createInventory(
            holder,
            UPGRADE_MENU_SIZE,
            Component.text("Generator upgrades", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    holder.inventory = inventory;

    ItemStack border = menuItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
    ItemStack interior = menuItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      boolean edge = slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8;
      inventory.setItem(slot, edge ? border : interior);
    }

    inventory.setItem(4, islandSummaryItem(player, island));
    inventory.setItem(10, travelRulesItem());
    inventory.setItem(16, centerEventItem());
    inventory.setItem(47, shardWalletItem(player));
    inventory.setItem(49, commandGuideItem());
    inventory.setItem(51, progressionItem(island));

    int index = 0;
    for (Upgrade upgrade : upgrades.values()) {
      if (index >= UPGRADE_MENU_SLOTS.length) {
        break;
      }
      inventory.setItem(UPGRADE_MENU_SLOTS[index], upgradeMenuItem(player, island, upgrade));
      index++;
    }

    player.openInventory(inventory);
  }

  private ItemStack upgradeMenuItem(Player player, Island island, Upgrade upgrade) {
    boolean owned = island.upgrades.contains(upgrade.id);
    boolean affordable = canAffordUpgrade(player, upgrade);
    ItemStack item = new ItemStack(upgrade.icon);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      NamedTextColor nameColor = owned ? NamedTextColor.GREEN : affordable ? NamedTextColor.YELLOW : NamedTextColor.RED;
      meta.displayName(Component.text(upgrade.name, nameColor).decorate(TextDecoration.BOLD));
      List<Component> lore = new ArrayList<>();
      lore.addAll(wrapLore(upgrade.description, NamedTextColor.GRAY));
      lore.add(Component.text(" "));
      lore.add(Component.text("Cost", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
      lore.add(costLine("Center Shards", countShards(player.getInventory()), upgrade.shardCost));
      for (Map.Entry<Material, Integer> cost : upgrade.materialCosts.entrySet()) {
        lore.add(costLine(materialName(cost.getKey()), countMaterial(player.getInventory(), cost.getKey()), cost.getValue()));
      }
      lore.add(Component.text(" "));
      lore.add(
          Component.text(
              owned ? "Owned and active" : affordable ? "Click to unlock" : "Missing resources",
              owned ? NamedTextColor.GREEN : affordable ? NamedTextColor.YELLOW : NamedTextColor.RED));
      meta.lore(lore);
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      meta.getPersistentDataContainer().set(upgradeKey, PersistentDataType.STRING, upgrade.id);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack islandSummaryItem(Player player, Island island) {
    List<Component> lore = new ArrayList<>();
    lore.add(Component.text("Owner: ", NamedTextColor.GRAY).append(Component.text(island.ownerName, NamedTextColor.WHITE)));
    lore.add(Component.text("Home: ", NamedTextColor.GRAY).append(Component.text(islandCoordinateText(island), NamedTextColor.WHITE)));
    lore.add(
        Component.text("Center: ", NamedTextColor.GRAY)
            .append(Component.text(centerDistanceText(player.getLocation()), NamedTextColor.WHITE)));
    lore.add(Component.text(" "));
    lore.add(
        Component.text("Generator progress: ", NamedTextColor.GRAY)
            .append(Component.text(ownedUpgradeCount(island) + "/" + upgrades.size(), NamedTextColor.YELLOW)));
    return menuItem(
        Material.GRASS_BLOCK,
        Component.text("Your island", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
        lore);
  }

  private ItemStack travelRulesItem() {
    return menuItem(
        Material.COMPASS,
        Component.text("Physical travel", NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
        List.of(
            Component.text("Bridge, fly, walk, or portal", NamedTextColor.GRAY),
            Component.text("to reach friends and shared areas.", NamedTextColor.GRAY),
            Component.text("No menu teleports are added here.", NamedTextColor.DARK_GRAY)));
  }

  private ItemStack centerEventItem() {
    List<Component> lore = new ArrayList<>();
    if (eventActive) {
      lore.add(Component.text("Bonus nodes and crates are live.", NamedTextColor.YELLOW));
      lore.add(Component.text("Time left: ", NamedTextColor.GRAY).append(Component.text(eventRemainingText(), NamedTextColor.WHITE)));
    } else {
      lore.add(Component.text("Events are idle right now.", NamedTextColor.GRAY));
      lore.add(Component.text("Watch the center for admin-started runs.", NamedTextColor.DARK_GRAY));
    }
    return menuItem(
        eventActive ? Material.BELL : Material.CLOCK,
        Component.text("Center event", eventActive ? NamedTextColor.YELLOW : NamedTextColor.GRAY).decorate(TextDecoration.BOLD),
        lore);
  }

  private ItemStack shardWalletItem(Player player) {
    int shards = countShards(player.getInventory());
    return menuItem(
        SHARD_MATERIAL,
        Component.text("Center Shards: " + shards, NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
        List.of(
            Component.text("Mine asteroid veins to earn shards.", NamedTextColor.GRAY),
            Component.text("Spend them here on generator upgrades.", NamedTextColor.GRAY)));
  }

  private ItemStack commandGuideItem() {
    return menuItem(
        Material.BOOK,
        Component.text("Quick commands", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
        List.of(
            Component.text("/msb island", NamedTextColor.YELLOW).append(Component.text(" - island info", NamedTextColor.GRAY)),
            Component.text("/msb upgrades", NamedTextColor.YELLOW).append(Component.text(" - generator menu", NamedTextColor.GRAY)),
            Component.text("/setspawn", NamedTextColor.YELLOW).append(Component.text(" - set island spawn", NamedTextColor.GRAY)),
            Component.text("/team", NamedTextColor.YELLOW).append(Component.text(" - team commands", NamedTextColor.GRAY))));
  }

  private ItemStack progressionItem(Island island) {
    int owned = ownedUpgradeCount(island);
    NamedTextColor color = owned >= upgrades.size() ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
    return menuItem(
        owned >= upgrades.size() ? Material.EMERALD : Material.EXPERIENCE_BOTTLE,
        Component.text("Progress " + owned + "/" + upgrades.size(), color).decorate(TextDecoration.BOLD),
        List.of(
            Component.text("Unlock generator tiers to improve", NamedTextColor.GRAY),
            Component.text("your cobblestone output.", NamedTextColor.GRAY)));
  }

  private ItemStack menuItem(Material material, Component name, List<Component> lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(name.decoration(TextDecoration.ITALIC, false));
      meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      item.setItemMeta(meta);
    }
    return item;
  }

  private Component costLine(String label, int have, int required) {
    NamedTextColor amountColor = have >= required ? NamedTextColor.GREEN : NamedTextColor.RED;
    return Component.text("  " + label + ": ", NamedTextColor.GRAY)
        .append(Component.text(have + "/" + required, amountColor));
  }

  private List<Component> wrapLore(String text, NamedTextColor color) {
    List<Component> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String word : text.split(" ")) {
      if (current.length() > 0 && current.length() + word.length() + 1 > 36) {
        lines.add(Component.text(current.toString(), color));
        current.setLength(0);
      }
      if (current.length() > 0) {
        current.append(' ');
      }
      current.append(word);
    }
    if (current.length() > 0) {
      lines.add(Component.text(current.toString(), color));
    }
    return lines;
  }

  private int countMaterial(PlayerInventory inventory, Material material) {
    return countInventoryItems(inventory, item -> item != null && item.getType() == material);
  }

  private int countInventoryItems(PlayerInventory inventory, Predicate<ItemStack> matcher) {
    int count = 0;
    for (ItemStack item : inventory.getContents()) {
      if (matcher.test(item)) {
        count += item.getAmount();
      }
    }
    return count;
  }

  private void removeMaterial(PlayerInventory inventory, Material material, int amount) {
    removeInventoryItems(inventory, amount, item -> item != null && item.getType() == material);
  }

  private void removeInventoryItems(PlayerInventory inventory, int amount, Predicate<ItemStack> matcher) {
    int remaining = amount;
    ItemStack[] contents = inventory.getContents();
    for (int slot = 0; slot < contents.length; slot++) {
      ItemStack item = contents[slot];
      if (!matcher.test(item)) {
        continue;
      }

      int take = Math.min(remaining, item.getAmount());
      item.setAmount(item.getAmount() - take);
      inventory.setItem(slot, item.getAmount() <= 0 ? null : item);
      remaining -= take;
      if (remaining <= 0) {
        return;
      }
    }
  }

  private String eventRemainingText() {
    if (!eventActive) {
      return "idle";
    }
    long remainingSeconds = Math.max(0L, (eventEndsAtMillis - System.currentTimeMillis()) / 1000L);
    long minutes = remainingSeconds / 60L;
    long seconds = remainingSeconds % 60L;
    return minutes + "m " + seconds + "s";
  }

  private static String materialName(Material material) {
    return friendlyName(material.name());
  }

  private static String friendlyName(String rawName) {
    String[] words = rawName.toLowerCase(Locale.ROOT).split("[_\\-\\s]+");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (word.isBlank()) {
        continue;
      }
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
    }
    return result.toString();
  }

  private static String friendlyKey(String rawName) {
    if (rawName == null) {
      return "";
    }
    return rawName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
  }

  private String uiLine(String colorCode, String text) {
    return UI_PREFIX + colorCode + text;
  }

  private void sendSuccess(CommandSender sender, String text) {
    sender.sendMessage(uiLine("\u00a7a", text));
  }

  private void sendInfo(CommandSender sender, String text) {
    sender.sendMessage(uiLine("\u00a77", text));
  }

  private void sendWarning(CommandSender sender, String text) {
    sender.sendMessage(uiLine("\u00a7e", text));
  }

  private void sendError(CommandSender sender, String text) {
    sender.sendMessage(uiLine("\u00a7c", text));
  }

  private void buyUpgrade(Player player, Island island, String upgradeInput) {
    Upgrade upgrade = resolveUpgrade(upgradeInput);
    if (upgrade == null) {
      sendError(player, "Unknown upgrade. Try " + String.join(", ", upgradeBuySuggestions()) + ".");
      return;
    }
    if (island.upgrades.contains(upgrade.id)) {
      sendWarning(player, "You already own that upgrade.");
      return;
    }
    if (countShards(player.getInventory()) < upgrade.shardCost) {
      sendError(player, "You need " + upgrade.shardCost + " Center Shards.");
      return;
    }
    for (Map.Entry<Material, Integer> cost : upgrade.materialCosts.entrySet()) {
      if (countMaterial(player.getInventory(), cost.getKey()) < cost.getValue()) {
        sendError(player, "You need " + cost.getValue() + " " + materialName(cost.getKey()) + ".");
        return;
      }
    }

    removeShards(player.getInventory(), upgrade.shardCost);
    for (Map.Entry<Material, Integer> cost : upgrade.materialCosts.entrySet()) {
      removeMaterial(player.getInventory(), cost.getKey(), cost.getValue());
    }
    island.upgrades.add(upgrade.id);
    saveData();
    sendSuccess(player, "Unlocked " + upgrade.name + ".");
  }

  private Upgrade resolveUpgrade(String input) {
    String key = friendlyKey(input);
    for (Upgrade upgrade : upgrades.values()) {
      if (key.equals(friendlyKey(upgrade.id))
          || key.equals(friendlyKey(upgrade.name))
          || key.equals(friendlyKey(upgradeBuyName(upgrade)))) {
        return upgrade;
      }
    }
    return null;
  }

  private List<String> upgradeBuySuggestions() {
    return upgrades.values().stream().map(this::upgradeBuyName).collect(Collectors.toList());
  }

  private String upgradeBuyName(Upgrade upgrade) {
    String name = upgrade.name.toLowerCase(Locale.ROOT);
    return name.endsWith(" generator") ? name.substring(0, name.length() - " generator".length()) : name;
  }

  private boolean canAffordUpgrade(Player player, Upgrade upgrade) {
    if (countShards(player.getInventory()) < upgrade.shardCost) {
      return false;
    }
    for (Map.Entry<Material, Integer> cost : upgrade.materialCosts.entrySet()) {
      if (countMaterial(player.getInventory(), cost.getKey()) < cost.getValue()) {
        return false;
      }
    }
    return true;
  }

  private void handleEventCommand(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sendWarning(sender, "Usage: /msb event <start|stop>");
      return;
    }
    if (args[1].equalsIgnoreCase("start")) {
      startEvent(sender);
    } else if (args[1].equalsIgnoreCase("stop")) {
      stopEvent(sender, true);
    } else {
      sendWarning(sender, "Usage: /msb event <start|stop>");
    }
  }

  private void handleCenterCommand(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sendWarning(sender, "Usage: /msb center <reset|status>");
      return;
    }

    if (args[1].equalsIgnoreCase("reset")) {
      resetCenter();
      sendSuccess(sender, "Center asteroid field reset.");
      return;
    }

    if (args[1].equalsIgnoreCase("status")) {
      sendCenterStatus(sender);
      return;
    }

    sendWarning(sender, "Usage: /msb center <reset|status>");
  }

  private void sendCenterStatus(CommandSender sender) {
    CenterGeneratorSettings settings = centerGeneratorSettings();
    List<Asteroid> asteroids = centerAsteroids();
    sendInfo(sender, "Center seed: " + centerSeed + ", asteroids: " + asteroids.size() + ".");
    sendInfo(
        sender,
        "Center archetypes: "
            + settings.archetypes.stream().map(archetype -> friendlyName(archetype.id)).collect(Collectors.joining(", "))
            + ".");
    sendInfo(
        sender,
        "Center event: "
            + settings.eventNodeCount
            + " bonus nodes, "
            + settings.eventCrateCount
            + " crates.");
    sendInfo(
        sender,
        "Center structures: "
            + (settings.structuresEnabled ? settings.structureCount + " enabled" : "disabled")
            + ", min tier "
            + settings.structureMinTier
            + ".");
    if (settings.warnings.isEmpty()) {
      sendSuccess(sender, "Center generator config looks good.");
      return;
    }
    for (String warning : settings.warnings) {
      sendWarning(sender, warning);
    }
  }

  private void handleBiomeCommand(CommandSender sender, String[] args) {
    if (args.length < 2 || !args[1].equalsIgnoreCase("fix")) {
      sendWarning(sender, "Usage: /msb biome fix");
      return;
    }

    normalizeManagedWorldBiomes();
    sendSuccess(sender, "Biome data refreshed: islands/center are plains, Nether/End areas are void.");
  }

  private void handleWorldResetCommand(CommandSender sender, String[] args) {
    if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
      sendWarning(sender, "Usage: /msb worldreset confirm");
      sendInfo(sender, "This queues a full Skyblock reset for the next server startup.");
      return;
    }

    try {
      Files.createDirectories(getDataFolder().toPath());
      Path flag = getDataFolder().toPath().resolve("full-world-reset.flag");
      Files.writeString(
          flag,
          "requested-by: " + sender.getName() + System.lineSeparator()
              + "requested-at: " + System.currentTimeMillis() + System.lineSeparator());
      sendSuccess(sender, "Full Skyblock world reset queued.");
      sendInfo(sender, "Stop and start the Skyblock server; the plugin will back up world data before Paper loads it.");
    } catch (IOException e) {
      sendError(sender, "Could not queue reset: " + e.getMessage());
    }
  }

  private void handleUnlockCommand(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sendWarning(sender, "Usage: /msb unlock <nether|end>");
      return;
    }
    if (args[1].equalsIgnoreCase("nether")) {
      netherUnlocked = true;
      ensureNetherWorld();
      saveData();
      broadcast(uiLine("\u00a75", "The Nether has been unlocked."));
    } else if (args[1].equalsIgnoreCase("end")) {
      endUnlocked = true;
      World end = ensureEndWorld();
      ensureEndArena(end);
      createEndPortalAtCenter();
      saveData();
      broadcast(uiLine("\u00a75", "The End has been unlocked. A shared End portal opened at the center."));
    } else {
      sendWarning(sender, "Usage: /msb unlock <nether|end>");
    }
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage("\u00a78---------- \u00a76\u00a7lMineperial Skyblock \u00a78----------");
    sender.sendMessage("\u00a7e/msb island \u00a78- \u00a77Show your island coordinates.");
    sender.sendMessage("\u00a7e/msb upgrades \u00a78- \u00a77Open the generator upgrade menu.");
    sender.sendMessage("\u00a7e/setspawn \u00a78- \u00a77Set your island spawnpoint.");
    sender.sendMessage("\u00a7e/team \u00a78- \u00a77Create or manage a shared island team.");
    if (sender.hasPermission(ADMIN_PERMISSION)) {
      sender.sendMessage("\u00a78Admin");
      sender.sendMessage("\u00a7e/msb island create <player> \u00a78- \u00a77Create a player's island.");
      sender.sendMessage("\u00a7e/msb island list \u00a78- \u00a77List islands.");
      sender.sendMessage("\u00a7e/msb island wipe \u00a78- \u00a77Remove island blocks and reset assignments.");
      sender.sendMessage("\u00a7e/msb event <start|stop> \u00a78- \u00a77Control center events.");
      sender.sendMessage("\u00a7e/msb center <reset|status> \u00a78- \u00a77Manage center asteroids.");
      sender.sendMessage("\u00a7e/msb nether reset \u00a78- \u00a77Rebuild the Nether archipelago.");
      sender.sendMessage("\u00a7e/msb biome fix \u00a78- \u00a77Refresh managed world biomes.");
      sender.sendMessage("\u00a7e/msb worldreset confirm \u00a78- \u00a77Queue a full world/data reset.");
      sender.sendMessage("\u00a7e/msb unlock <nether|end> \u00a78- \u00a77Unlock dimensions.");
      sender.sendMessage("\u00a7e/msb reload \u00a78- \u00a77Reload config/data.");
    }
  }

  private boolean requireAdmin(CommandSender sender) {
    if (!sender.hasPermission(ADMIN_PERMISSION)) {
      sendError(sender, "You do not have permission to use that command.");
      return false;
    }
    return true;
  }

  private void configureManagedWorldGenerators() {
    if (!getConfig().getBoolean("auto-register-world-generators", true)) {
      return;
    }

    Map<String, String> generators = managedWorldGenerators();
    boolean saved = saveBukkitWorldGeneratorConfig(generators);
    boolean patched = patchLoadedBukkitWorldGeneratorConfig(generators);
    if (saved || patched) {
      getLogger().info("Registered Mineperial Skyblock void generators for managed worlds.");
    }
  }

  private Map<String, String> managedWorldGenerators() {
    Map<String, String> generators = new LinkedHashMap<>();
    putManagedWorldGenerator(generators, getWorldName(), GENERATOR_ID_CENTER);
    putManagedWorldGenerator(generators, getNetherWorldName(), GENERATOR_ID_VOID);
    putManagedWorldGenerator(generators, getEndWorldName(), GENERATOR_ID_VOID);
    return generators;
  }

  private void putManagedWorldGenerator(Map<String, String> generators, String worldName, String generatorId) {
    if (worldName == null || worldName.isBlank()) {
      return;
    }
    generators.put(worldName, getName() + ":" + generatorId);
  }

  private boolean saveBukkitWorldGeneratorConfig(Map<String, String> generators) {
    File serverRoot = resolveServerRoot();
    if (serverRoot == null) {
      getLogger().warning("Could not resolve the server root to update bukkit.yml world generators.");
      return false;
    }

    File bukkitConfigFile = new File(serverRoot, "bukkit.yml");
    YamlConfiguration bukkitConfig = YamlConfiguration.loadConfiguration(bukkitConfigFile);
    boolean changed = applyWorldGeneratorConfig(bukkitConfig, generators);
    if (!changed) {
      return false;
    }

    try {
      bukkitConfig.save(bukkitConfigFile);
      return true;
    } catch (IOException e) {
      getLogger().warning("Could not save bukkit.yml world generator settings: " + e.getMessage());
      return false;
    }
  }

  private boolean patchLoadedBukkitWorldGeneratorConfig(Map<String, String> generators) {
    try {
      Field configurationField = Bukkit.getServer().getClass().getDeclaredField("configuration");
      configurationField.setAccessible(true);
      Object value = configurationField.get(Bukkit.getServer());
      if (!(value instanceof YamlConfiguration)) {
        return false;
      }
      return applyWorldGeneratorConfig((YamlConfiguration) value, generators);
    } catch (NoSuchFieldException e) {
      getLogger()
          .warning("Could not patch live Bukkit world generator settings; saved bukkit.yml will apply next restart.");
    } catch (ReflectiveOperationException | RuntimeException e) {
      getLogger().warning("Could not patch live Bukkit world generator settings: " + e.getMessage());
    }
    return false;
  }

  private boolean applyWorldGeneratorConfig(YamlConfiguration bukkitConfig, Map<String, String> generators) {
    boolean changed = false;
    for (Map.Entry<String, String> entry : generators.entrySet()) {
      String path = "worlds." + entry.getKey() + ".generator";
      String current = bukkitConfig.getString(path);
      if (!entry.getValue().equals(current)) {
        bukkitConfig.set(path, entry.getValue());
        changed = true;
      }
    }
    return changed;
  }

  private File resolveServerRoot() {
    File pluginsFolder = getDataFolder().getParentFile();
    if (pluginsFolder != null && pluginsFolder.getParentFile() != null) {
      return pluginsFolder.getParentFile();
    }
    return Bukkit.getWorldContainer();
  }

  private void performPendingFullWorldReset() {
    Path dataFolderPath = getDataFolder().toPath();
    Path flag = dataFolderPath.resolve("full-world-reset.flag");
    if (!Files.exists(flag)) {
      return;
    }

    try {
      File pluginsFolder = getDataFolder().getParentFile();
      File serverRootFile = pluginsFolder == null ? null : pluginsFolder.getParentFile();
      if (serverRootFile == null) {
        getLogger().warning("Full reset flag found, but the server root could not be resolved.");
        return;
      }

      Path serverRoot = serverRootFile.toPath();
      Path backupRoot =
          serverRoot.getParent() == null ? serverRoot.resolve("backups") : serverRoot.getParent().resolve("backups");
      Files.createDirectories(backupRoot);
      String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

      Set<String> worldNames = configuredWorldNamesForReset();
      for (String worldName : worldNames) {
        Path worldPath = serverRoot.resolve(worldName);
        if (!Files.exists(worldPath)) {
          continue;
        }
        Path backupPath = backupRoot.resolve("skyblock-" + worldName + "-reset-" + timestamp);
        Files.move(worldPath, backupPath);
        getLogger().warning("Backed up " + worldName + " to " + backupPath + ".");
      }

      Path dataPath = dataFolderPath.resolve("data.yml");
      if (Files.exists(dataPath)) {
        Path dataBackup = backupRoot.resolve("skyblock-data-reset-" + timestamp + ".yml");
        Files.move(dataPath, dataBackup);
        getLogger().warning("Backed up skyblock plugin data to " + dataBackup + ".");
      }

      Files.deleteIfExists(flag);
      getLogger().warning("Full Skyblock reset applied. Fresh world/data will be generated this startup.");
    } catch (IOException e) {
      getLogger().warning("Could not apply pending full Skyblock reset: " + e.getMessage());
    }
  }

  private Set<String> configuredWorldNamesForReset() {
    Set<String> worldNames = new HashSet<>(Arrays.asList("world", "world_nether", "world_the_end"));
    File configFile = new File(getDataFolder(), "config.yml");
    if (!configFile.exists()) {
      return worldNames;
    }

    YamlConfiguration resetConfig = YamlConfiguration.loadConfiguration(configFile);
    worldNames.add(resetConfig.getString("world-name", "world"));
    worldNames.add(resetConfig.getString("nether-world-name", "world_nether"));
    worldNames.add(resetConfig.getString("end-world-name", "world_the_end"));
    worldNames.removeIf(name -> name == null || name.isBlank());
    return worldNames;
  }

  private World getOverworld() {
    World world = Bukkit.getWorld(getWorldName());
    if (world != null) {
      warnIfUnexpectedWorldGenerator(world, centerGenerator, GENERATOR_ID_CENTER);
      return world;
    }
    return WorldCreator.name(getWorldName())
        .environment(World.Environment.NORMAL)
        .generator(centerGenerator)
        .createWorld();
  }

  private void warnIfUnexpectedWorldGenerator(World world, ChunkGenerator expectedGenerator, String expectedId) {
    if (world == null || worldUsesExpectedGenerator(world, expectedGenerator)) {
      return;
    }

    String key = world.getName() + ":" + expectedId;
    if (!warnedGeneratorMismatches.add(key)) {
      return;
    }

    ChunkGenerator actual = world.getGenerator();
    String actualName = actual == null ? "vanilla" : actual.getClass().getName();
    getLogger()
        .warning(
            "World '"
                + world.getName()
                + "' is loaded with "
                + actualName
                + " instead of "
                + getName()
                + ":"
                + expectedId
                + ". New installs are auto-configured, but existing terrain requires /msb worldreset confirm "
                + "or a manual world reset to become a true void world.");
  }

  private boolean worldUsesExpectedGenerator(World world, ChunkGenerator expectedGenerator) {
    ChunkGenerator actual = world.getGenerator();
    if (expectedGenerator == centerGenerator) {
      return actual instanceof CenterAsteroidChunkGenerator;
    }
    if (expectedGenerator == voidGenerator) {
      return actual != null && actual.getClass() == VoidChunkGenerator.class;
    }
    return actual == expectedGenerator;
  }

  private void setBooleanGameRule(World world, String name, boolean value) {
    try {
      World.class
          .getMethod("setGameRuleValue", String.class, String.class)
          .invoke(world, name, Boolean.toString(value));
    } catch (ReflectiveOperationException e) {
      getLogger().warning("Could not set gamerule " + name + ": " + e.getMessage());
    }
  }

  private PlayerIdentity resolvePlayerIdentity(String name) {
    Player online = Bukkit.getPlayerExact(name);
    if (online != null) {
      return new PlayerIdentity(online.getUniqueId(), online.getName());
    }

    org.bukkit.OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
    if (cached != null) {
      String cachedName = cached.getName() == null ? name : cached.getName();
      return new PlayerIdentity(cached.getUniqueId(), cachedName);
    }

    return null;
  }

  private World ensureNetherWorld() {
    World world = getOrCreateNetherWorld();
    setBiomeArea(world, 0, 0, getConfig().getInt("nether-biome-radius", 160), Biome.THE_VOID);
    ensureNetherArchipelago(world, false);
    return world;
  }

  private World getOrCreateNetherWorld() {
    World world = Bukkit.getWorld(getNetherWorldName());
    if (world != null) {
      warnIfUnexpectedWorldGenerator(world, voidGenerator, GENERATOR_ID_VOID);
      return world;
    }
    return WorldCreator.name(getNetherWorldName())
        .environment(World.Environment.NETHER)
        .generator(voidGenerator)
        .createWorld();
  }

  private void ensureNetherArchipelago(World world, boolean force) {
    netherNodeMaterialByKey.clear();
    netherProtectedBlocks.clear();
    netherHotspots.clear();

    for (NetherIsland island : netherIslandSpecs()) {
      buildNetherIsland(world, island, force);
      buildNetherIslandFeature(world, island, force);
      addNetherIslandNodes(world, island, force);
    }
  }

  private void resetNether() {
    World world = getOrCreateNetherWorld();
    if (netherProtectedBlocks.isEmpty() && netherNodeMaterialByKey.isEmpty()) {
      ensureNetherArchipelago(world, false);
    }
    clearNetherArea(world);
    netherNodeMaterialByKey.clear();
    netherMinedNodes.clear();
    netherPlayerBlocks.clear();
    netherProtectedBlocks.clear();
    netherHotspots.clear();
    ensureNetherArchipelago(world, true);
    applyNetherMinedNodeState();
    refreshNetherMobs();
    saveData();
  }

  private void clearNetherArea(World world) {
    Set<String> keys = new HashSet<>();
    keys.addAll(netherProtectedBlocks);
    keys.addAll(netherNodeMaterialByKey.keySet());
    keys.addAll(netherPlayerBlocks);
    for (String key : keys) {
      Block block = blockFromKey(key);
      if (block != null && block.getWorld().equals(world)) {
        clearBlock(block);
      }
    }
  }

  private void buildNetherIsland(World world, NetherIsland island, boolean force) {
    for (int x = island.x - island.radiusX; x <= island.x + island.radiusX; x++) {
      for (int y = island.y - island.radiusY; y <= island.y + island.radiusY; y++) {
        for (int z = island.z - island.radiusZ; z <= island.z + island.radiusZ; z++) {
          double distance = netherIslandDistance(island, x, y, z);
          double roughness = (Math.floorMod(netherHash(x, y, z, island.salt), 100) / 100.0 - 0.5) * 0.20;
          if (distance > 1.0 + roughness) {
            continue;
          }

          Material material = netherTerrainMaterial(island, distance, x, y, z);
          setNetherGeneratedBlock(world.getBlockAt(x, y, z), material, force);
        }
      }
    }
  }

  private void buildNetherIslandFeature(World world, NetherIsland island, boolean force) {
    int topY = netherSurfaceY(island, island.x, island.z);
    buildNetherAnchorPad(world, island.x, topY + 1, island.z, island.type == NetherIslandType.HUB ? 6 : 3, force);

    switch (island.type) {
      case HUB -> {
        for (int x = -2; x <= 2; x++) {
          for (int z = -2; z <= 2; z++) {
            Material material = Math.abs(x) == 2 || Math.abs(z) == 2 ? Material.OBSIDIAN : Material.POLISHED_BLACKSTONE;
            setNetherGeneratedBlock(world.getBlockAt(island.x + x, topY + 2, island.z + z), material, force);
          }
        }
        setNetherGeneratedBlock(world.getBlockAt(island.x, topY + 3, island.z), Material.RESPAWN_ANCHOR, force);
      }
      case CRIMSON_WELL -> {
        for (int x = -4; x <= 4; x++) {
          for (int z = -4; z <= 4; z++) {
            if (x * x + z * z > 18) {
              continue;
            }
            int surfaceY = netherSurfaceY(island, island.x + x, island.z + z);
            setNetherGeneratedBlock(world.getBlockAt(island.x + x, surfaceY, island.z + z), Material.CRIMSON_NYLIUM, force);
            if (Math.floorMod(netherHash(island.x + x, surfaceY, island.z + z, 41), 5) == 0) {
              setNetherGeneratedBlock(world.getBlockAt(island.x + x, surfaceY + 1, island.z + z), Material.CRIMSON_FUNGUS, force);
            }
          }
        }
      }
      case BLAZE_CRUCIBLE -> {
        for (int x = -4; x <= 4; x++) {
          for (int z = -4; z <= 4; z++) {
            int distance = Math.abs(x) + Math.abs(z);
            if (distance == 4 || distance == 5) {
              setNetherGeneratedBlock(world.getBlockAt(island.x + x, topY + 2, island.z + z), Material.NETHER_BRICKS, force);
            } else if (distance <= 2) {
              setNetherGeneratedBlock(world.getBlockAt(island.x + x, topY + 2, island.z + z), Material.MAGMA_BLOCK, force);
            }
          }
        }
        setNetherGeneratedBlock(world.getBlockAt(island.x, topY + 3, island.z), Material.LAVA, force);
      }
      case BASTION_SPLINTER -> {
        int hotspotRadius = getConfig().getInt("nether-pvp-hotspot-radius", 34);
        netherHotspots.add(new NetherHotspot(world.getName(), island.x, topY + 2, island.z, hotspotRadius));
        for (int x = -5; x <= 5; x++) {
          for (int z = -5; z <= 5; z++) {
            if (Math.abs(x) == 5 || Math.abs(z) == 5 || Math.floorMod(netherHash(island.x + x, topY, island.z + z, 53), 7) == 0) {
              Material material = Math.floorMod(netherHash(island.x + x, topY, island.z + z, 59), 5) == 0
                  ? Material.GILDED_BLACKSTONE
                  : Material.POLISHED_BLACKSTONE_BRICKS;
              setNetherGeneratedBlock(world.getBlockAt(island.x + x, topY + 2, island.z + z), material, force);
            }
          }
        }
        setNetherGeneratedBlock(world.getBlockAt(island.x, topY + 3, island.z), Material.CRYING_OBSIDIAN, force);
      }
      case DEBRIS_RIFT -> {
        for (int x = -5; x <= 5; x++) {
          for (int z = -1; z <= 1; z++) {
            setNetherGeneratedBlock(world.getBlockAt(island.x + x, topY + 1, island.z + z), Material.MAGMA_BLOCK, force);
          }
        }
        setNetherGeneratedBlock(world.getBlockAt(island.x, topY + 2, island.z), Material.CRYING_OBSIDIAN, force);
      }
      default -> {
        for (int i = -2; i <= 2; i++) {
          setNetherGeneratedBlock(world.getBlockAt(island.x + i, topY + 2, island.z - 2), Material.GLOWSTONE, force);
        }
      }
    }
  }

  private void buildNetherAnchorPad(World world, int centerX, int y, int centerZ, int radius, boolean force) {
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (x * x + z * z > radius * radius) {
          continue;
        }
        Material material = Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Material.OBSIDIAN : Material.POLISHED_BLACKSTONE;
        setNetherGeneratedBlock(world.getBlockAt(centerX + x, y, centerZ + z), material, force);
      }
    }
  }

  private void addNetherIslandNodes(World world, NetherIsland island, boolean force) {
    Random random = new Random(netherSeed ^ island.salt * 341873128712L);
    switch (island.type) {
      case QUARTZ_COMET -> {
        placeNetherNodes(world, island, Material.NETHER_QUARTZ_ORE, 9, random, force);
        placeNetherNodes(world, island, Material.GLOWSTONE, 3, random, force);
      }
      case CRIMSON_WELL -> {
        placeNetherNodes(world, island, Material.SOUL_SAND, 6, random, force);
        placeNetherNodes(world, island, Material.CRIMSON_STEM, 4, random, force);
      }
      case BLAZE_CRUCIBLE -> {
        placeNetherNodes(world, island, Material.MAGMA_BLOCK, 6, random, force);
        placeNetherNodes(world, island, Material.GLOWSTONE, 2, random, force);
      }
      case BASTION_SPLINTER -> {
        placeNetherNodes(world, island, Material.BLACKSTONE, 8, random, force);
        placeNetherNodes(world, island, Material.GILDED_BLACKSTONE, 5, random, force);
        placeNetherNodes(world, island, Material.NETHER_QUARTZ_ORE, 3, random, force);
      }
      case DEBRIS_RIFT -> {
        placeNetherNodes(world, island, Material.BLACKSTONE, 5, random, force);
        placeNetherNodes(world, island, Material.MAGMA_BLOCK, 4, random, force);
        placeNetherNodes(world, island, Material.ANCIENT_DEBRIS, 2, random, force);
      }
      default -> {}
    }
  }

  private void placeNetherNodes(
      World world, NetherIsland island, Material material, int count, Random random, boolean force) {
    int placed = 0;
    for (int tries = 0; tries < 120 && placed < count; tries++) {
      if (material == Material.ANCIENT_DEBRIS && countNetherNodes(Material.ANCIENT_DEBRIS) >= getConfig().getInt("nether-ancient-debris-active-cap", 4)) {
        return;
      }

      int x = island.x + random.nextInt(island.radiusX * 2 + 1) - island.radiusX;
      int z = island.z + random.nextInt(island.radiusZ * 2 + 1) - island.radiusZ;
      double horizontal =
          Math.pow((x - island.x) / (double) island.radiusX, 2.0)
              + Math.pow((z - island.z) / (double) island.radiusZ, 2.0);
      if (horizontal > 0.86) {
        continue;
      }

      int y = netherSurfaceY(island, x, z);
      String key = locationKey(world.getName(), x, y, z);
      if (netherNodeMaterialByKey.containsKey(key)) {
        continue;
      }
      registerNetherNode(world, x, y, z, material, force);
      placed++;
    }
  }

  private int countNetherNodes(Material material) {
    int count = 0;
    for (Material node : netherNodeMaterialByKey.values()) {
      if (node == material) {
        count++;
      }
    }
    return count;
  }

  private void registerNetherNode(World world, int x, int y, int z, Material node, boolean force) {
    String key = locationKey(world.getName(), x, y, z);
    netherNodeMaterialByKey.put(key, node);
    netherProtectedBlocks.add(key);

    MinedNode mined = netherMinedNodes.get(key);
    if (mined != null && mined.respawnAtMillis > System.currentTimeMillis()) {
      world.getBlockAt(x, y, z).setType(netherPlaceholderMaterial(node), false);
      return;
    }

    netherMinedNodes.remove(key);
    if (force || world.getBlockAt(x, y, z).getType() != node) {
      world.getBlockAt(x, y, z).setType(node, false);
    }
  }

  private void setNetherGeneratedBlock(Block block, Material material, boolean force) {
    String key = locationKey(block.getLocation());
    netherProtectedBlocks.add(key);
    block.setBiome(Biome.THE_VOID);
    if (force || block.getType().isAir() || netherNodeMaterialByKey.containsKey(key)) {
      block.setType(material, false);
    }
  }

  private double netherIslandDistance(NetherIsland island, int x, int y, int z) {
    double nx = (x - island.x) / (double) island.radiusX;
    double ny = (y - island.y) / (double) island.radiusY;
    double nz = (z - island.z) / (double) island.radiusZ;
    return Math.sqrt(nx * nx + ny * ny + nz * nz);
  }

  private int netherSurfaceY(NetherIsland island, int x, int z) {
    double nx = (x - island.x) / (double) island.radiusX;
    double nz = (z - island.z) / (double) island.radiusZ;
    double horizontal = nx * nx + nz * nz;
    if (horizontal >= 1.0) {
      return island.y;
    }
    return island.y + Math.max(1, (int) Math.round(island.radiusY * Math.sqrt(1.0 - horizontal)));
  }

  private Material netherTerrainMaterial(NetherIsland island, double distance, int x, int y, int z) {
    int roll = Math.floorMod(netherHash(x, y, z, island.salt + 17), 100);
    return switch (island.type) {
      case HUB -> roll < 55 ? Material.POLISHED_BLACKSTONE : Material.BASALT;
      case QUARTZ_COMET -> roll < 48 ? Material.BASALT : roll < 78 ? Material.NETHERRACK : Material.SMOOTH_BASALT;
      case CRIMSON_WELL -> distance > 0.72 ? Material.CRIMSON_NYLIUM : roll < 75 ? Material.NETHERRACK : Material.SOUL_SOIL;
      case BLAZE_CRUCIBLE -> roll < 42 ? Material.BLACKSTONE : roll < 78 ? Material.BASALT : Material.MAGMA_BLOCK;
      case BASTION_SPLINTER -> roll < 52 ? Material.BLACKSTONE : roll < 86 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.GILDED_BLACKSTONE;
      case DEBRIS_RIFT -> roll < 46 ? Material.BLACKSTONE : roll < 78 ? Material.BASALT : Material.MAGMA_BLOCK;
    };
  }

  private List<NetherIsland> netherIslandSpecs() {
    List<NetherIsland> islands = new ArrayList<>();
    addNetherIsland(islands, NetherIslandType.HUB, 0, 78, 0, 24, 7, 24, 11);
    addNetherIsland(islands, NetherIslandType.QUARTZ_COMET, 86, 82, 28, 20, 6, 16, 21);
    addNetherIsland(islands, NetherIslandType.QUARTZ_COMET, -112, 74, -48, 18, 5, 20, 22);
    addNetherIsland(islands, NetherIslandType.CRIMSON_WELL, 160, 78, -96, 22, 7, 18, 31);
    addNetherIsland(islands, NetherIslandType.CRIMSON_WELL, -172, 82, 104, 19, 7, 23, 32);
    addNetherIsland(islands, NetherIslandType.BLAZE_CRUCIBLE, 252, 88, 36, 18, 7, 18, 41);
    addNetherIsland(islands, NetherIslandType.BLAZE_CRUCIBLE, -248, 86, -132, 18, 7, 18, 42);
    addNetherIsland(islands, NetherIslandType.BASTION_SPLINTER, 334, 84, -126, 24, 8, 21, 51);
    addNetherIsland(islands, NetherIslandType.BASTION_SPLINTER, -338, 84, 138, 22, 8, 24, 52);
    addNetherIsland(islands, NetherIslandType.DEBRIS_RIFT, 442, 74, 64, 17, 7, 22, 61);
    addNetherIsland(islands, NetherIslandType.DEBRIS_RIFT, -456, 76, -184, 20, 7, 18, 62);
    return islands;
  }

  private void addNetherIsland(
      List<NetherIsland> islands,
      NetherIslandType type,
      int x,
      int y,
      int z,
      int radiusX,
      int radiusY,
      int radiusZ,
      int salt) {
    int jitterX = Math.floorMod(netherHash(x, y, z, salt), 13) - 6;
    int jitterZ = Math.floorMod(netherHash(x, y, z, salt + 3), 13) - 6;
    int jitterY = Math.floorMod(netherHash(x, y, z, salt + 7), 5) - 2;
    islands.add(new NetherIsland(type, x + jitterX, y + jitterY, z + jitterZ, radiusX, radiusY, radiusZ, salt));
  }

  private void refreshNetherMobs() {
    World world = Bukkit.getWorld(getNetherWorldName());
    if (world == null) {
      return;
    }

    for (NetherIsland island : netherIslandSpecs()) {
      refreshNetherMobs(world, island);
    }
  }

  private void refreshNetherMobs(World world, NetherIsland island) {
    EntityType[] types = netherMobTypes(island.type);
    int target = netherMobTarget(island.type);
    if (target <= 0 || types.length == 0) {
      return;
    }

    Set<EntityType> typeSet = Set.of(types);
    Location center = new Location(world, island.x + 0.5, netherSurfaceY(island, island.x, island.z) + 3.0, island.z + 0.5);
    long existing =
        countNearbyEntities(center, island.radiusX + 12, 18, island.radiusZ + 12, entity -> typeSet.contains(entity.getType()));
    for (int i = (int) existing; i < target; i++) {
      Location spawn = randomNetherMobSpawn(world, island);
      if (spawn == null) {
        return;
      }
      EntityType type = types[ThreadLocalRandom.current().nextInt(types.length)];
      world.spawnEntity(spawn, type);
    }
  }

  private Location randomNetherMobSpawn(World world, NetherIsland island) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int tries = 0; tries < 20; tries++) {
      int x = island.x + random.nextInt(island.radiusX * 2 + 1) - island.radiusX;
      int z = island.z + random.nextInt(island.radiusZ * 2 + 1) - island.radiusZ;
      double horizontal =
          Math.pow((x - island.x) / (double) island.radiusX, 2.0)
              + Math.pow((z - island.z) / (double) island.radiusZ, 2.0);
      if (horizontal > 0.70) {
        continue;
      }

      int y = netherSurfaceY(island, x, z) + 2;
      Block feet = world.getBlockAt(x, y, z);
      Block head = world.getBlockAt(x, y + 1, z);
      if (feet.getType().isAir() && head.getType().isAir()) {
        return new Location(world, x + 0.5, y, z + 0.5);
      }
    }
    return null;
  }

  private EntityType[] netherMobTypes(NetherIslandType type) {
    return switch (type) {
      case CRIMSON_WELL -> new EntityType[] {EntityType.PIGLIN, EntityType.HOGLIN};
      case BLAZE_CRUCIBLE -> new EntityType[] {EntityType.BLAZE};
      case BASTION_SPLINTER -> new EntityType[] {EntityType.PIGLIN, EntityType.PIGLIN_BRUTE};
      case DEBRIS_RIFT -> new EntityType[] {EntityType.WITHER_SKELETON, EntityType.MAGMA_CUBE};
      default -> new EntityType[0];
    };
  }

  private int netherMobTarget(NetherIslandType type) {
    return switch (type) {
      case CRIMSON_WELL -> 5;
      case BLAZE_CRUCIBLE -> 4;
      case BASTION_SPLINTER -> 6;
      case DEBRIS_RIFT -> 4;
      default -> 0;
    };
  }

  private Location netherEntryLocation(World world, Player player) {
    for (NetherIsland island : netherIslandSpecs()) {
      if (island.type == NetherIslandType.HUB) {
        int topY = netherSurfaceY(island, island.x, island.z);
        return new Location(world, island.x + 0.5, topY + 3.0, island.z + 2.5, player.getLocation().getYaw(), 0);
      }
    }
    return new Location(world, 0.5, 84.0, 2.5, player.getLocation().getYaw(), 0);
  }

  private Location netherReturnLocation(Player player) {
    return islandSpawnLocation(getOrCreateEffectiveIsland(player));
  }

  private Location endEntryLocation(World end, Player player) {
    return new Location(end, 0.5, END_ARENA_Y + 2.0, -12.5, player.getLocation().getYaw(), 0);
  }

  private World ensureEndWorld() {
    World world = Bukkit.getWorld(getEndWorldName());
    if (world != null) {
      warnIfUnexpectedWorldGenerator(world, voidGenerator, GENERATOR_ID_VOID);
      prepareEndWorld(world);
      return world;
    }
    world =
        WorldCreator.name(getEndWorldName())
        .environment(World.Environment.THE_END)
        .generator(voidGenerator)
        .createWorld();
    prepareEndWorld(world);
    return world;
  }

  private void prepareEndWorld(World world) {
    setBiomeArea(world, 0, 0, END_ARENA_RADIUS + 32, Biome.THE_VOID);
    keepEndArenaChunksLoaded(world);
  }

  private void keepEndArenaChunksLoaded(World world) {
    for (int chunkX = -END_ARENA_CHUNK_RADIUS; chunkX <= END_ARENA_CHUNK_RADIUS; chunkX++) {
      for (int chunkZ = -END_ARENA_CHUNK_RADIUS; chunkZ <= END_ARENA_CHUNK_RADIUS; chunkZ++) {
        world.loadChunk(chunkX, chunkZ);
        world.addPluginChunkTicket(chunkX, chunkZ, this);
      }
    }
  }

  private void ensureEndArena(World world) {
    int y = END_ARENA_Y;
    prepareEndWorld(world);
    for (int x = -END_ARENA_RADIUS; x <= END_ARENA_RADIUS; x++) {
      for (int z = -END_ARENA_RADIUS; z <= END_ARENA_RADIUS; z++) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance <= END_ARENA_RADIUS) {
          world.getBlockAt(x, y, z).setType(Material.END_STONE, false);
        }
        if (distance <= 8) {
          world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
        }
      }
    }

    DragonBattle battle = world.getEnderDragonBattle();
    if (battle != null) {
      battle.generateEndPortal(false);
    }
    buildSafePlatform(new Location(world, 0, y + 1, -12), Material.OBSIDIAN);
    buildEndPillars(world);
    EnderDragon dragon = ensureEndDragon(world);
    if (dragon != null) {
      prepareEndDragon(dragon, world);
    }
    refreshEndDragonAggro();
  }

  private void buildEndPillars(World world) {
    removeManagedEndCrystals(world);

    int[][] oldPillars = {{28, 0}, {-28, 0}, {0, 28}, {0, -28}, {20, 20}, {-20, 20}, {20, -20}, {-20, -20}};
    for (int[] pillar : oldPillars) {
      clearEndPillar(world, pillar[0], pillar[1], 88);
    }

    for (int[] pillar : END_PILLARS) {
      int x = pillar[0];
      int z = pillar[1];
      int topY = pillar[2];
      clearEndPillar(world, x, z, topY + 3);
      for (int y = END_ARENA_Y + 1; y <= topY; y++) {
        world.getBlockAt(x, y, z).setType(Material.OBSIDIAN, false);
      }
      world.getBlockAt(x, topY + 1, z).setType(Material.BEDROCK, false);
      Location crystalLocation = new Location(world, x + 0.5, topY + 2.0, z + 0.5);
      boolean hasCrystal =
          world.getNearbyEntities(crystalLocation, 1.0, 2.0, 1.0).stream()
              .anyMatch(entity -> entity instanceof EnderCrystal);
      if (!hasCrystal) {
        world.spawn(crystalLocation, EnderCrystal.class);
      }
    }
  }

  private void clearEndPillar(World world, int x, int z, int topY) {
    for (int y = END_ARENA_Y + 1; y <= topY; y++) {
      Material material = world.getBlockAt(x, y, z).getType();
      if (material == Material.OBSIDIAN || material == Material.BEDROCK) {
        world.getBlockAt(x, y, z).setType(Material.AIR, false);
      }
    }
  }

  private void removeManagedEndCrystals(World world) {
    double maxDistanceSquared = Math.pow(END_ARENA_RADIUS + 8, 2);
    Location center = new Location(world, 0, END_ARENA_Y, 0);
    for (EnderCrystal crystal : world.getEntitiesByClass(EnderCrystal.class)) {
      if (crystal.getLocation().distanceSquared(center) <= maxDistanceSquared) {
        crystal.remove();
      }
    }
  }

  private EnderDragon ensureEndDragon(World world) {
    List<EnderDragon> dragons = new ArrayList<>(world.getEntitiesByClass(EnderDragon.class));
    if (!dragons.isEmpty()) {
      return dragons.get(0);
    }
    return world.spawn(new Location(world, 0, END_ARENA_Y + 24, 0), EnderDragon.class);
  }

  private void prepareEndDragon(EnderDragon dragon, World world) {
    dragon.setAI(true);
    dragon.setAware(true);
    dragon.setAggressive(true);
    dragon.setRemoveWhenFarAway(false);
    dragon.setPersistent(true);
    dragon.setPodium(new Location(world, 0.5, END_ARENA_Y + 1, 0.5));
    if (dragon.getPhase() == EnderDragon.Phase.HOVER) {
      dragon.setPhase(EnderDragon.Phase.CIRCLING);
    }
  }

  private void refreshEndDragonAggro() {
    if (!endUnlocked) {
      return;
    }

    World world = Bukkit.getWorld(getEndWorldName());
    if (world == null || world.getEnvironment() != World.Environment.THE_END) {
      return;
    }

    List<Player> targets =
        world.getPlayers().stream()
            .filter(this::isEndDragonTarget)
            .collect(Collectors.toList());
    if (targets.isEmpty()) {
      return;
    }

    keepEndArenaChunksLoaded(world);
    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
      prepareEndDragon(dragon, world);
      Player target = nearestEndDragonTarget(dragon.getLocation(), targets);
      if (target != null) {
        nudgeEndDragonAttack(dragon, target);
      }
    }
  }

  private boolean isEndDragonTarget(Player player) {
    if (!player.isOnline() || player.isDead()) {
      return false;
    }
    GameMode gameMode = player.getGameMode();
    if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
      return false;
    }
    Location center = new Location(player.getWorld(), 0, END_ARENA_Y, 0);
    return player.getLocation().distanceSquared(center)
        <= (double) END_DRAGON_AGGRO_RANGE * END_DRAGON_AGGRO_RANGE;
  }

  private Player nearestEndDragonTarget(Location origin, List<Player> targets) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    for (Player target : targets) {
      double distance = target.getLocation().distanceSquared(origin);
      if (distance < nearestDistance) {
        nearest = target;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  private void nudgeEndDragonAttack(EnderDragon dragon, Player target) {
    if (dragon.getPhase() == EnderDragon.Phase.DYING) {
      return;
    }

    dragon.setTarget(target);
    dragon.setAggressive(true);
    dragon.setNoActionTicks(0);
    EnderDragon.Phase phase = dragon.getPhase();
    if (phase == EnderDragon.Phase.CIRCLING
        || phase == EnderDragon.Phase.HOVER
        || phase == EnderDragon.Phase.STRAFING
        || phase == EnderDragon.Phase.FLY_TO_PORTAL
        || phase == EnderDragon.Phase.LAND_ON_PORTAL
        || phase == EnderDragon.Phase.LEAVE_PORTAL) {
      if (!setDragonStrafeTarget(dragon, target)) {
        dragon.setPhase(EnderDragon.Phase.CIRCLING);
      }
    }
  }

  private boolean setDragonStrafeTarget(EnderDragon dragon, Player target) {
    try {
      dragon.setPhase(EnderDragon.Phase.STRAFING);

      Object dragonHandle = dragon.getClass().getMethod("getHandle").invoke(dragon);
      Object phaseManager = dragonHandle.getClass().getMethod("getPhaseManager").invoke(dragonHandle);
      Class<?> phaseType = Class.forName("net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase");
      Object strafePhase = phaseType.getField("STRAFE_PLAYER").get(null);
      Object strafeInstance = phaseManager.getClass().getMethod("getPhase", phaseType).invoke(phaseManager, strafePhase);
      Object targetHandle = target.getClass().getMethod("getHandle").invoke(target);
      Class<?> livingEntityType = Class.forName("net.minecraft.world.entity.LivingEntity");
      Method setTarget = strafeInstance.getClass().getMethod("setTarget", livingEntityType);
      setTarget.invoke(strafeInstance, targetHandle);
      return true;
    } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
      if (!warnedDragonStrafeReflection) {
        warnedDragonStrafeReflection = true;
        getLogger().warning("Could not directly target Ender Dragon strafe phase: " + e.getMessage());
      }
      return false;
    }
  }

  private void createEndPortalAtCenter() {
    World world = getOverworld();
    int y = getConfig().getInt("center-y", 92) + 20;
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        Material material = Math.abs(x) == 2 || Math.abs(z) == 2 ? Material.OBSIDIAN : Material.END_PORTAL;
        world.getBlockAt(x, y, z).setType(material, false);
      }
    }
    world.getBlockAt(0, y + 1, -3).setType(Material.TORCH, false);
  }

  private void buildSafePlatform(Location location, Material material) {
    World world = location.getWorld();
    setBiomeArea(world, location.getBlockX(), location.getBlockZ(), 24, world.getEnvironment() == World.Environment.NORMAL ? Biome.PLAINS : Biome.THE_VOID);
    int y = Math.max(world.getMinHeight() + 8, location.getBlockY() - 1);
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        world.getBlockAt(location.getBlockX() + x, y, location.getBlockZ() + z).setType(material, false);
      }
    }
  }

  private void normalizeManagedWorldBiomes() {
    for (World world : Bukkit.getWorlds()) {
      Biome biome = world.getEnvironment() == World.Environment.NORMAL ? Biome.PLAINS : Biome.THE_VOID;
      Location spawn = world.getSpawnLocation();
      int radius = world.getEnvironment() == World.Environment.NORMAL ? getConfig().getInt("center-radius", 160) + 32 : 96;
      setBiomeArea(world, spawn.getBlockX(), spawn.getBlockZ(), radius, biome);
    }

    World overworld = getOverworld();
    for (Island island : islands.values()) {
      setBiomeArea(overworld, island.x, island.z, islandBiomeRadius(), Biome.PLAINS);
    }
  }

  private void setBiomeArea(World world, int centerX, int centerZ, int radius, Biome biome) {
    int minY = world.getMinHeight();
    int maxY = world.getMaxHeight();
    for (int x = centerX - radius; x <= centerX + radius; x += 4) {
      for (int z = centerZ - radius; z <= centerZ + radius; z += 4) {
        for (int y = minY; y < maxY; y += 4) {
          world.getBlockAt(x, y, z).setBiome(biome);
        }
      }
    }
  }

  private int islandBiomeRadius() {
    return Math.max(64, getConfig().getInt("grid-spacing", 384) / 2 - 16);
  }

  private boolean shouldKeepOnDeath(ItemStack item) {
    if (item == null || item.getType() == Material.AIR) {
      return false;
    }
    Material material = item.getType();
    String name = material.name();
    return name.endsWith("_HELMET")
        || name.endsWith("_CHESTPLATE")
        || name.endsWith("_LEGGINGS")
        || name.endsWith("_BOOTS")
        || name.endsWith("_SWORD")
        || name.endsWith("_AXE")
        || name.endsWith("_PICKAXE")
        || name.endsWith("_SHOVEL")
        || name.endsWith("_HOE")
        || material == Material.BOW
        || material == Material.CROSSBOW
        || material == Material.TRIDENT
        || material == Material.SHIELD
        || material == Material.ELYTRA
        || material == Material.SHEARS
        || material == Material.FISHING_ROD
        || material == Material.FLINT_AND_STEEL
        || name.equals("MACE");
  }

  private Location getSafeDropLocation(Player player) {
    Location safe = lastSafeLocations.get(player.getUniqueId());
    if (safe != null && safe.getWorld() != null) {
      return safe.clone().add(0, 0.25, 0);
    }
    Island island = effectiveIsland(player.getUniqueId());
    if (island != null) {
      return islandSpawnLocation(island);
    }
    return getOverworld().getSpawnLocation();
  }

  private Player attackingPlayer(Entity damager) {
    if (damager instanceof Player player) {
      return player;
    }
    if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
      return player;
    }
    return null;
  }

  private boolean isProtectedIslandCore(Block block) {
    if ((block.getType() != Material.LODESTONE && block.getType() != Material.BEDROCK)
        || !isSkyblockOverworld(block.getWorld())) {
      return false;
    }
    for (Island island : islands.values()) {
      if (Math.abs(block.getX() - island.x) <= 1
          && Math.abs(block.getZ() - island.z) <= 1
          && Math.abs(block.getY() - getConfig().getInt("island-y", 88)) <= 2) {
        return true;
      }
    }
    return false;
  }

  private boolean isInCenter(Location location) {
    if (!isSkyblockOverworld(location)) {
      return false;
    }
    int radius = getConfig().getInt("center-radius", 160);
    return location.getX() * location.getX() + location.getZ() * location.getZ() <= radius * radius;
  }

  private boolean isPvpAllowed(Location location) {
    return isInCenter(location) || isInNetherHotspot(location);
  }

  private boolean isInManagedNether(Location location) {
    return location != null && isManagedNetherWorld(location.getWorld());
  }

  private boolean isInNetherHotspot(Location location) {
    if (!isInManagedNether(location)) {
      return false;
    }
    for (NetherHotspot hotspot : netherHotspots) {
      if (hotspot.contains(location)) {
        return true;
      }
    }
    return false;
  }

  private String pvpZoneName(Location location) {
    if (isInNetherHotspot(location)) {
      return "Nether hotspot";
    }
    if (isInCenter(location)) {
      return "Center PvP";
    }
    return "Safe zone";
  }

  private void updatePlayerHud(Player player) {
    if (!player.isOnline()) {
      return;
    }

    boolean inPvpZone = isPvpAllowed(player.getLocation());
    String zoneName = pvpZoneName(player.getLocation());
    Boolean previous = pvpZoneState.put(player.getUniqueId(), inPvpZone);
    if (inPvpZone) {
      player.sendActionBar(
          Component.text()
              .append(Component.text("PVP ZONE", NamedTextColor.RED))
              .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
              .append(Component.text(zoneName, NamedTextColor.WHITE))
              .build());
    } else if (Boolean.TRUE.equals(previous)) {
      player.sendActionBar(
          Component.text()
              .append(Component.text("SAFE ZONE", NamedTextColor.GREEN))
              .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
              .append(Component.text("PvP disabled", NamedTextColor.WHITE))
              .build());
    }

    if (previous != null && previous.booleanValue() != inPvpZone) {
      if (inPvpZone) {
        sendError(player, "You entered " + zoneName + ".");
      } else {
        sendSuccess(player, "You left the PvP zone.");
      }
    }

    updateScoreboard(player, inPvpZone);
  }

  private void updateAllPlayerTabNames() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      updatePlayerTabName(player);
    }
  }

  private void updateTeamTabNames(SkyblockTeam team) {
    for (UUID memberId : team.memberIds) {
      Player player = Bukkit.getPlayer(memberId);
      if (player != null) {
        updatePlayerTabName(player);
      }
    }
  }

  private void updatePlayerTabName(Player player) {
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team == null) {
      player.playerListName(Component.text(player.getName(), NamedTextColor.WHITE));
      return;
    }

    player.playerListName(
        Component.text("[" + team.name + "] ", NamedTextColor.GOLD)
            .append(Component.text(player.getName(), NamedTextColor.WHITE)));
  }

  private void updateScoreboard(Player player, boolean inPvpZone) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager == null) {
      return;
    }

    List<String> lines = scoreboardLines(player, inPvpZone);
    UUID playerId = player.getUniqueId();
    if (lines.equals(lastScoreboardLines.get(playerId))) {
      return;
    }
    lastScoreboardLines.put(playerId, new ArrayList<>(lines));

    Scoreboard scoreboard = manager.getNewScoreboard();
    Objective objective =
        scoreboard.registerNewObjective(
            "msb",
            Criteria.DUMMY,
            Component.text("Mineperial", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    int score = lines.size();
    for (String line : lines) {
      objective.getScore(line).setScore(score--);
    }
    player.setScoreboard(scoreboard);
  }

  private List<String> scoreboardLines(Player player, boolean inPvpZone) {
    Island island = effectiveIsland(player.getUniqueId());
    return List.of(
        inPvpZone ? "\u00a7c" + pvpZoneName(player.getLocation()) : "\u00a7aSafe zone",
        "\u00a78 ",
        "\u00a77Island \u00a7f" + islandScoreboardText(island),
        "\u00a77Shards \u00a7b" + countShards(player.getInventory()),
        "\u00a77Generators \u00a7e" + (island == null ? "0" : ownedUpgradeCount(island)) + "\u00a78/\u00a7e" + upgrades.size(),
        "\u00a78  ",
        "\u00a77Center \u00a7f" + centerDistanceText(player.getLocation()),
        "\u00a77Event " + (eventActive ? "\u00a7e" + eventRemainingText() : "\u00a78Idle"));
  }

  private String islandScoreboardText(Island island) {
    if (island == null) {
      return "None";
    }
    return island.x + ", " + island.z;
  }

  private String centerDistanceText(Location location) {
    if (!isSkyblockOverworld(location)) {
      return "other world";
    }
    double distance = Math.sqrt(location.getX() * location.getX() + location.getZ() * location.getZ());
    int radius = getConfig().getInt("center-radius", 160);
    if (distance <= radius) {
      return "inside";
    }
    return Math.round(distance - radius) + "m out";
  }

  private Island effectiveIsland(UUID playerId) {
    SkyblockTeam team = teamByMember.get(playerId);
    if (team != null) {
      return islands.get(team.leaderId);
    }
    return islands.get(playerId);
  }

  private Island getOrCreateEffectiveIsland(Player player) {
    SkyblockTeam team = teamByMember.get(player.getUniqueId());
    if (team == null) {
      Island island = islands.get(player.getUniqueId());
      return island == null ? createIsland(player.getUniqueId(), player.getName()) : island;
    }

    Island island = islands.get(team.leaderId);
    return island == null ? createIsland(team.leaderId, playerName(team.leaderId)) : island;
  }

  private Island nearestIsland(Location location, int radius) {
    if (!isSkyblockOverworld(location)) {
      return null;
    }
    int radiusSquared = radius * radius;
    Island nearest = null;
    double best = Double.MAX_VALUE;
    for (Island island : islands.values()) {
      double dx = location.getX() - island.x;
      double dz = location.getZ() - island.z;
      double distance = dx * dx + dz * dz;
      if (distance <= radiusSquared && distance < best) {
        nearest = island;
        best = distance;
      }
    }
    return nearest;
  }

  private int countShards(PlayerInventory inventory) {
    return countInventoryItems(inventory, this::isCenterShard);
  }

  private void removeShards(PlayerInventory inventory, int amount) {
    removeInventoryItems(inventory, amount, this::isCenterShard);
  }

  private int centerGeneratorSignature() {
    ConfigurationSection section = getConfig().getConfigurationSection("center-generator");
    return section == null ? 0 : section.getValues(true).toString().hashCode();
  }

  private CenterGeneratorSettings centerGeneratorSettings() {
    List<String> warnings = new ArrayList<>();
    ConfigurationSection section = getConfig().getConfigurationSection("center-generator");
    List<CenterAsteroidArchetype> archetypes = centerAsteroidArchetypes(section, warnings);
    CenterAsteroidArchetype centralArchetype =
        archetypeById(archetypes, section == null ? "ruined" : section.getString("central-archetype", "ruined"));
    if (centralArchetype == null) {
      centralArchetype = archetypes.get(0);
      warnings.add("Unknown center-generator.central-archetype; using " + friendlyName(centralArchetype.id) + ".");
    }

    ConfigurationSection eventSection = section == null ? null : section.getConfigurationSection("event");
    ConfigurationSection crateSection = section == null ? null : section.getConfigurationSection("crates");
    ConfigurationSection structureSection = section == null ? null : section.getConfigurationSection("structures");

    return new CenterGeneratorSettings(
        warnings,
        archetypes,
        centralArchetype,
        configInt(section, "central-tier", 3, 1, 4, warnings),
        configInt(section, "central-radius-x", 42, 16, 72, warnings),
        configInt(section, "central-radius-y", 17, 8, 32, warnings),
        configInt(section, "central-radius-z", 37, 16, 72, warnings),
        configInt(section, "lobe-count", 5, 0, 12, warnings),
        configInt(section, "satellite-min", 10, 1, 32, warnings),
        configInt(section, "satellite-max", 18, 1, 40, warnings),
        configInt(section, "max-decorations-per-asteroid", 18, 0, 80, warnings),
        configInt(section, "max-hazards-per-asteroid", 7, 0, 40, warnings),
        configBoolean(structureSection, "enabled", true),
        configBoolean(structureSection, "central", true),
        configInt(structureSection, "count", 7, 0, 24, warnings),
        configInt(structureSection, "min-tier", 2, 1, 4, warnings),
        configInt(eventSection, "nodes", 12, 0, 64, warnings),
        configInt(eventSection, "crates", 5, 0, 24, warnings),
        weightedMaterials(
            eventSection,
            "bonus-nodes",
            materialWeightMap(
                Material.DIAMOND_ORE, 26,
                Material.EMERALD_ORE, 8,
                Material.GOLD_ORE, 22,
                Material.REDSTONE_ORE, 20,
                Material.LAPIS_ORE, 20,
                Material.IRON_ORE, 18),
            Material.DIAMOND_ORE,
            true,
            warnings),
        materialAmountMap(
            eventSection,
            "crate-loot",
            materialAmountMap(
                Material.DIAMOND, 2,
                Material.GOLDEN_APPLE, 1,
                Material.EXPERIENCE_BOTTLE, 10,
                Material.ENDER_PEARL, 4,
                Material.AMETHYST_SHARD, 5),
            false,
            warnings),
        configInt(eventSection, "crate-shards-min", 4, 0, 32, warnings),
        configInt(eventSection, "crate-shards-max", 7, 0, 64, warnings),
        materialAmountMap(
            crateSection,
            "loot",
            materialAmountMap(
                Material.IRON_INGOT, 8,
                Material.GOLD_INGOT, 4,
                Material.EXPERIENCE_BOTTLE, 6,
                Material.AMETHYST_SHARD, 3,
                Material.COAL, 12),
            false,
            warnings),
        configInt(crateSection, "shards-min", 1, 0, 16, warnings),
        configInt(crateSection, "shards-max", 4, 0, 32, warnings),
        91137,
        95321);
  }

  private List<CenterAsteroidArchetype> centerAsteroidArchetypes(
      ConfigurationSection section, List<String> warnings) {
    List<CenterAsteroidArchetype> defaults = defaultCenterAsteroidArchetypes();
    ConfigurationSection archetypeSection = section == null ? null : section.getConfigurationSection("archetypes");
    if (archetypeSection == null) {
      return defaults;
    }

    List<CenterAsteroidArchetype> archetypes = new ArrayList<>();
    for (String id : archetypeSection.getKeys(false)) {
      ConfigurationSection child = archetypeSection.getConfigurationSection(id);
      if (child == null) {
        continue;
      }
      CenterAsteroidArchetype fallback = archetypeById(defaults, id);
      if (fallback == null) {
        fallback = defaults.get(0);
      }
      archetypes.add(parseCenterAsteroidArchetype(id, child, fallback, warnings));
    }
    if (archetypes.isEmpty()) {
      warnings.add("center-generator.archetypes has no valid entries; using built-in center defaults.");
      return defaults;
    }
    return archetypes;
  }

  private CenterAsteroidArchetype parseCenterAsteroidArchetype(
      String id, ConfigurationSection section, CenterAsteroidArchetype fallback, List<String> warnings) {
    return new CenterAsteroidArchetype(
        id.toLowerCase(Locale.ROOT),
        configInt(section, "weight", fallback.weight, 1, 1000, warnings),
        configInt(section, "min-tier", fallback.minTier, 1, 4, warnings),
        configInt(section, "max-tier", fallback.maxTier, 1, 4, warnings),
        configDouble(section, "roughness-bonus", fallback.roughnessBonus, -0.10, 0.24, warnings),
        configDouble(section, "decoration-chance", fallback.decorationChance, 0.0, 1.0, warnings),
        configDouble(section, "hazard-chance", fallback.hazardChance, 0.0, 1.0, warnings),
        configDouble(section, "ruin-chance", fallback.ruinChance, 0.0, 1.0, warnings),
        configDouble(section, "crate-chance", fallback.crateChance, 0.0, 1.0, warnings),
        configDouble(section, "spawner-chance", fallback.spawnerChance, 0.0, 1.0, warnings),
        weightedMaterials(section, "palette", fallback.palette.weights, fallback.palette.fallback, true, warnings),
        weightedMaterials(
            section,
            "surface-palette",
            fallback.surfacePalette.weights,
            fallback.surfacePalette.fallback,
            true,
            warnings),
        weightedMaterials(
            section,
            "core-palette",
            fallback.corePalette.weights,
            fallback.corePalette.fallback,
            true,
            warnings),
        weightedMaterials(
            section,
            "accent-blocks",
            fallback.accentBlocks.weights,
            fallback.accentBlocks.fallback,
            true,
            warnings),
        weightedMaterials(
            section,
            "decorations",
            fallback.decorations.weights,
            fallback.decorations.fallback,
            true,
            warnings),
        weightedMaterials(
            section,
            "hazard-blocks",
            fallback.hazardBlocks.weights,
            fallback.hazardBlocks.fallback,
            true,
            warnings),
        weightedMaterials(section, "nodes", fallback.nodeMaterials.weights, fallback.nodeMaterials.fallback, true, warnings),
        materialAmountMap(section, "crate-loot", fallback.crateLoot, false, warnings),
        entityTypes(section, "spawner-types", fallback.spawnerTypes, warnings));
  }

  private List<CenterAsteroidArchetype> defaultCenterAsteroidArchetypes() {
    List<CenterAsteroidArchetype> archetypes = new ArrayList<>();
    archetypes.add(
        new CenterAsteroidArchetype(
            "rocky",
            26,
            1,
            2,
            0.00,
            0.018,
            0.006,
            0.035,
            0.10,
            0.00,
            weightedMaterials(materialWeightMap(Material.STONE, 38, Material.ANDESITE, 28, Material.TUFF, 22, Material.COBBLED_DEEPSLATE, 12), Material.STONE),
            weightedMaterials(materialWeightMap(Material.ANDESITE, 34, Material.STONE, 28, Material.TUFF, 24, Material.GRAVEL, 8), Material.ANDESITE),
            weightedMaterials(materialWeightMap(Material.DEEPSLATE, 40, Material.TUFF, 24, Material.STONE, 20), Material.DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.CALCITE, 16, Material.DEEPSLATE_BRICKS, 10, Material.GRAVEL, 8), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.STONE_BUTTON, 10, Material.COBBLESTONE_WALL, 8, Material.TORCH, 4), Material.COBBLESTONE_WALL),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 1), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.COAL_ORE, 28, Material.COPPER_ORE, 22, Material.IRON_ORE, 36, Material.REDSTONE_ORE, 5, Material.LAPIS_ORE, 4), Material.COAL_ORE),
            materialAmountMap(Material.COAL, 12, Material.RAW_COPPER, 8, Material.IRON_INGOT, 5, Material.BREAD, 3),
            List.of(EntityType.ZOMBIE, EntityType.SPIDER)));
    archetypes.add(
        new CenterAsteroidArchetype(
            "metallic",
            20,
            2,
            3,
            0.02,
            0.012,
            0.012,
            0.055,
            0.16,
            0.025,
            weightedMaterials(materialWeightMap(Material.DEEPSLATE, 30, Material.TUFF, 22, Material.IRON_BLOCK, 2, Material.COPPER_BLOCK, 3, Material.COBBLED_DEEPSLATE, 26), Material.DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.COBBLED_DEEPSLATE, 30, Material.TUFF, 22, Material.CUT_COPPER, 5, Material.RAW_COPPER_BLOCK, 4), Material.COBBLED_DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.DEEPSLATE, 38, Material.BASALT, 22, Material.SMOOTH_BASALT, 18), Material.DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.CUT_COPPER, 14, Material.IRON_BARS, 14, Material.DEEPSLATE_BRICKS, 10), Material.DEEPSLATE_BRICKS),
            weightedMaterials(materialWeightMap(Material.IRON_BARS, 16, Material.LANTERN, 3), Material.IRON_BARS),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 3), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.IRON_ORE, 28, Material.COPPER_ORE, 24, Material.GOLD_ORE, 18, Material.REDSTONE_ORE, 15, Material.LAPIS_ORE, 8, Material.DIAMOND_ORE, 2), Material.IRON_ORE),
            materialAmountMap(Material.IRON_INGOT, 10, Material.COPPER_INGOT, 12, Material.GOLD_INGOT, 4, Material.REDSTONE, 12),
            List.of(EntityType.SKELETON, EntityType.ZOMBIE)));
    archetypes.add(
        new CenterAsteroidArchetype(
            "crystal",
            18,
            2,
            3,
            0.01,
            0.030,
            0.004,
            0.040,
            0.18,
            0.015,
            weightedMaterials(materialWeightMap(Material.CALCITE, 32, Material.SMOOTH_BASALT, 22, Material.AMETHYST_BLOCK, 5, Material.TUFF, 18), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.CALCITE, 34, Material.AMETHYST_BLOCK, 7, Material.SMOOTH_BASALT, 18, Material.TUFF, 14), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.AMETHYST_BLOCK, 12, Material.CALCITE, 28, Material.SMOOTH_BASALT, 18), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.AMETHYST_BLOCK, 16, Material.BUDDING_AMETHYST, 3, Material.GLOWSTONE, 3), Material.AMETHYST_BLOCK),
            weightedMaterials(materialWeightMap(Material.AMETHYST_CLUSTER, 10, Material.LARGE_AMETHYST_BUD, 8, Material.MEDIUM_AMETHYST_BUD, 6, Material.SMALL_AMETHYST_BUD, 4), Material.AMETHYST_CLUSTER),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 1), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.LAPIS_ORE, 24, Material.REDSTONE_ORE, 22, Material.EMERALD_ORE, 7, Material.DIAMOND_ORE, 5, Material.GOLD_ORE, 10, Material.IRON_ORE, 14), Material.LAPIS_ORE),
            materialAmountMap(Material.AMETHYST_SHARD, 10, Material.LAPIS_LAZULI, 12, Material.EXPERIENCE_BOTTLE, 6, Material.GLOWSTONE_DUST, 8),
            List.of(EntityType.SPIDER, EntityType.CAVE_SPIDER)));
    archetypes.add(
        new CenterAsteroidArchetype(
            "volcanic",
            16,
            2,
            3,
            0.05,
            0.010,
            0.040,
            0.050,
            0.12,
            0.020,
            weightedMaterials(materialWeightMap(Material.BASALT, 30, Material.BLACKSTONE, 25, Material.DEEPSLATE, 16, Material.MAGMA_BLOCK, 5), Material.BASALT),
            weightedMaterials(materialWeightMap(Material.BASALT, 28, Material.BLACKSTONE, 24, Material.MAGMA_BLOCK, 8, Material.TUFF, 10), Material.BASALT),
            weightedMaterials(materialWeightMap(Material.BLACKSTONE, 30, Material.DEEPSLATE, 18, Material.OBSIDIAN, 4), Material.BLACKSTONE),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 18, Material.OBSIDIAN, 5, Material.BLACKSTONE, 12), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.SOUL_TORCH, 6, Material.COBBLED_DEEPSLATE_WALL, 8), Material.COBBLED_DEEPSLATE_WALL),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 10, Material.CAMPFIRE, 1), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.GOLD_ORE, 22, Material.REDSTONE_ORE, 18, Material.IRON_ORE, 16, Material.COAL_ORE, 14, Material.DIAMOND_ORE, 4, Material.EMERALD_ORE, 3), Material.GOLD_ORE),
            materialAmountMap(Material.GOLD_INGOT, 5, Material.MAGMA_CREAM, 3, Material.COAL, 14, Material.OBSIDIAN, 4),
            List.of(EntityType.ZOMBIE, EntityType.SKELETON)));
    archetypes.add(
        new CenterAsteroidArchetype(
            "icy",
            14,
            1,
            2,
            -0.01,
            0.022,
            0.002,
            0.025,
            0.10,
            0.00,
            weightedMaterials(materialWeightMap(Material.PACKED_ICE, 18, Material.BLUE_ICE, 4, Material.CALCITE, 24, Material.STONE, 18, Material.TUFF, 14), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.PACKED_ICE, 20, Material.CALCITE, 24, Material.SNOW_BLOCK, 8, Material.STONE, 16), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.BLUE_ICE, 8, Material.PACKED_ICE, 18, Material.CALCITE, 20), Material.PACKED_ICE),
            weightedMaterials(materialWeightMap(Material.SEA_LANTERN, 2, Material.CALCITE, 12, Material.PACKED_ICE, 12), Material.CALCITE),
            weightedMaterials(materialWeightMap(Material.SNOW, 12, Material.WHITE_CANDLE, 3), Material.SNOW),
            weightedMaterials(materialWeightMap(Material.POWDER_SNOW, 1), Material.POWDER_SNOW),
            weightedMaterials(materialWeightMap(Material.COAL_ORE, 18, Material.COPPER_ORE, 18, Material.IRON_ORE, 34, Material.LAPIS_ORE, 8, Material.REDSTONE_ORE, 5), Material.IRON_ORE),
            materialAmountMap(Material.SNOWBALL, 16, Material.PACKED_ICE, 8, Material.IRON_INGOT, 4, Material.EXPERIENCE_BOTTLE, 4),
            List.of(EntityType.ZOMBIE)));
    archetypes.add(
        new CenterAsteroidArchetype(
            "ruined",
            18,
            3,
            3,
            0.04,
            0.020,
            0.018,
            0.110,
            0.24,
            0.055,
            weightedMaterials(materialWeightMap(Material.DEEPSLATE_BRICKS, 20, Material.CRACKED_DEEPSLATE_BRICKS, 14, Material.DEEPSLATE, 20, Material.TUFF, 16, Material.OBSIDIAN, 3), Material.DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.CRACKED_DEEPSLATE_BRICKS, 20, Material.DEEPSLATE_BRICKS, 16, Material.TUFF, 14, Material.COBBLED_DEEPSLATE, 18), Material.CRACKED_DEEPSLATE_BRICKS),
            weightedMaterials(materialWeightMap(Material.DEEPSLATE, 28, Material.OBSIDIAN, 6, Material.BLACKSTONE, 16), Material.DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.IRON_BARS, 16, Material.LANTERN, 4, Material.CHISELED_DEEPSLATE, 8), Material.CHISELED_DEEPSLATE),
            weightedMaterials(materialWeightMap(Material.IRON_BARS, 8, Material.LANTERN, 5, Material.COBBLED_DEEPSLATE_WALL, 8), Material.COBBLED_DEEPSLATE_WALL),
            weightedMaterials(materialWeightMap(Material.MAGMA_BLOCK, 4, Material.CAMPFIRE, 1), Material.MAGMA_BLOCK),
            weightedMaterials(materialWeightMap(Material.IRON_ORE, 35, Material.GOLD_ORE, 14, Material.REDSTONE_ORE, 12, Material.LAPIS_ORE, 10, Material.DIAMOND_ORE, 5, Material.EMERALD_ORE, 4), Material.IRON_ORE),
            materialAmountMap(Material.DIAMOND, 1, Material.GOLDEN_APPLE, 1, Material.EXPERIENCE_BOTTLE, 8, Material.ENDER_PEARL, 3, Material.IRON_INGOT, 8),
            List.of(EntityType.SKELETON, EntityType.ZOMBIE, EntityType.SPIDER)));
    return archetypes;
  }

  private CenterAsteroidArchetype archetypeById(List<CenterAsteroidArchetype> archetypes, String id) {
    if (id == null) {
      return null;
    }
    for (CenterAsteroidArchetype archetype : archetypes) {
      if (archetype.id.equalsIgnoreCase(id)) {
        return archetype;
      }
    }
    return null;
  }

  private int configInt(ConfigurationSection section, String path, int fallback, int min, int max, List<String> warnings) {
    int value = section == null ? fallback : section.getInt(path, fallback);
    if (value < min || value > max) {
      warnings.add("center-generator." + path + " is outside " + min + "-" + max + "; using " + fallback + ".");
      return fallback;
    }
    return value;
  }

  private double configDouble(
      ConfigurationSection section, String path, double fallback, double min, double max, List<String> warnings) {
    double value = section == null ? fallback : section.getDouble(path, fallback);
    if (value < min || value > max) {
      warnings.add("center-generator." + path + " is outside " + min + "-" + max + "; using " + fallback + ".");
      return fallback;
    }
    return value;
  }

  private boolean configBoolean(ConfigurationSection section, String path, boolean fallback) {
    return section == null ? fallback : section.getBoolean(path, fallback);
  }

  private WeightedMaterialSet weightedMaterials(Map<Material, Integer> weights, Material fallback) {
    return new WeightedMaterialSet(weights, fallback);
  }

  private WeightedMaterialSet weightedMaterials(
      ConfigurationSection section,
      String path,
      Map<Material, Integer> fallback,
      Material fallbackMaterial,
      boolean requireBlock,
      List<String> warnings) {
    Map<Material, Integer> weights = materialWeightMap(section, path, fallback, requireBlock, warnings);
    return new WeightedMaterialSet(weights, fallbackMaterial);
  }

  private Map<Material, Integer> materialWeightMap(Object... pairs) {
    Map<Material, Integer> weights = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      if (pairs[i] instanceof Material material && pairs[i + 1] instanceof Integer weight && weight > 0) {
        weights.put(material, weight);
      }
    }
    return weights;
  }

  private Map<Material, Integer> materialWeightMap(
      ConfigurationSection section,
      String path,
      Map<Material, Integer> fallback,
      boolean requireBlock,
      List<String> warnings) {
    if (section == null || !section.contains(path)) {
      return fallback;
    }

    Map<Material, Integer> weights = new LinkedHashMap<>();
    ConfigurationSection child = section.getConfigurationSection(path);
    if (child != null) {
      for (String key : child.getKeys(false)) {
        Material material = parsedMaterial(key, requireBlock, warnings);
        int weight = child.getInt(key, 0);
        if (material != null && weight > 0) {
          weights.put(material, weight);
        }
      }
    } else {
      for (String materialName : section.getStringList(path)) {
        Material material = parsedMaterial(materialName, requireBlock, warnings);
        if (material != null) {
          weights.put(material, 1);
        }
      }
    }

    if (weights.isEmpty()) {
      warnings.add("center-generator." + path + " has no valid materials; using defaults.");
      return fallback;
    }
    return weights;
  }

  private Map<Material, Integer> materialAmountMap(Object... pairs) {
    Map<Material, Integer> amounts = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      if (pairs[i] instanceof Material material && pairs[i + 1] instanceof Integer amount && amount > 0) {
        amounts.put(material, amount);
      }
    }
    return amounts;
  }

  private Map<Material, Integer> materialAmountMap(
      ConfigurationSection section,
      String path,
      Map<Material, Integer> fallback,
      boolean requireBlock,
      List<String> warnings) {
    if (section == null || !section.contains(path)) {
      return fallback;
    }

    ConfigurationSection child = section.getConfigurationSection(path);
    if (child == null) {
      warnings.add("center-generator." + path + " must be a material-to-amount map; using defaults.");
      return fallback;
    }

    Map<Material, Integer> amounts = new LinkedHashMap<>();
    for (String key : child.getKeys(false)) {
      Material material = parsedMaterial(key, requireBlock, warnings);
      int amount = child.getInt(key, 0);
      if (material != null && amount > 0) {
        amounts.put(material, amount);
      }
    }
    if (amounts.isEmpty()) {
      warnings.add("center-generator." + path + " has no valid loot; using defaults.");
      return fallback;
    }
    return amounts;
  }

  private Material parsedMaterial(String name, boolean requireBlock, List<String> warnings) {
    String materialName = name == null ? "" : name;
    Material material = Material.matchMaterial(materialName);
    if (material == null && materialName.equalsIgnoreCase("CHAIN")) {
      material = Material.matchMaterial("IRON_CHAIN");
    }
    if (material == null) {
      warnings.add("Unknown center generator material: " + name + ".");
      return null;
    }
    if (requireBlock && !material.isBlock()) {
      warnings.add("Center generator material is not a block: " + name + ".");
      return null;
    }
    return material;
  }

  private List<EntityType> entityTypes(
      ConfigurationSection section, String path, List<EntityType> fallback, List<String> warnings) {
    if (section == null || !section.contains(path)) {
      return fallback;
    }
    List<EntityType> types = new ArrayList<>();
    for (String name : section.getStringList(path)) {
      try {
        EntityType type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        if (type.isAlive() && type.isSpawnable()) {
          types.add(type);
        }
      } catch (IllegalArgumentException e) {
        warnings.add("Unknown center generator spawner type: " + name + ".");
      }
    }
    if (types.isEmpty()) {
      warnings.add("center-generator." + path + " has no valid mob types; using defaults.");
      return fallback;
    }
    return types;
  }

  private List<Asteroid> centerAsteroids() {
    int centerY = getConfig().getInt("center-y", 92);
    int centerRadius = Math.max(96, getConfig().getInt("center-radius", 160));
    long seed = centerSeed;
    int generatorSignature = centerGeneratorSignature();
    if (cachedCenterAsteroidSeed == seed
        && cachedCenterAsteroidY == centerY
        && cachedCenterAsteroidRadius == centerRadius
        && cachedCenterGeneratorSignature == generatorSignature) {
      return cachedCenterAsteroids;
    }

    CenterGeneratorSettings settings = centerGeneratorSettings();
    Random random = new Random(seed ^ 0x6C8E9CF570932BD5L);
    List<Asteroid> asteroids = new ArrayList<>();

    addAsteroid(
        asteroids,
        random,
        settings.centralArchetype,
        0,
        centerY,
        0,
        settings.centralRadiusX,
        settings.centralRadiusY,
        settings.centralRadiusZ,
        settings.centralTier,
        seed + 101L);

    double lobeAngle = random.nextDouble() * Math.PI * 2.0;
    for (int i = 0; i < settings.lobeCount; i++) {
      lobeAngle += Math.PI * 0.4 + (random.nextDouble() - 0.5) * 0.45;
      int distance = 18 + random.nextInt(25);
      int x = (int) Math.round(Math.cos(lobeAngle) * distance);
      int z = (int) Math.round(Math.sin(lobeAngle) * distance);
      int y = centerY + random.nextInt(15) - 7;
      int radiusX = 19 + random.nextInt(10);
      int radiusY = 8 + random.nextInt(5);
      int radiusZ = 18 + random.nextInt(10);
      int tier = random.nextDouble() < 0.28 ? 2 : 3;
      addAsteroid(
          asteroids,
          random,
          settings.randomArchetype(random, tier),
          x,
          y,
          z,
          radiusX,
          radiusY,
          radiusZ,
          tier,
          seed + 211L + i * 53L);
    }

    int configuredSatelliteCount =
        settings.satelliteMin + random.nextInt(Math.max(1, settings.satelliteMax - settings.satelliteMin + 1));
    int satelliteCount = Math.max(settings.satelliteMin, Math.min(configuredSatelliteCount, centerRadius / 7));
    double angle = random.nextDouble() * Math.PI * 2.0;
    for (int i = 0; i < satelliteCount; i++) {
      double t = satelliteCount == 1 ? 0.0 : i / (double) (satelliteCount - 1);
      angle += 2.399963229728653 + (random.nextDouble() - 0.5) * 0.52;
      int distance =
          clamp(
              (int)
                  Math.round(
                      58
                          + Math.pow(t, 0.82) * (centerRadius - 82)
                          + random.nextDouble() * 18
                          - 9),
              52,
              centerRadius - 18);
      int x = (int) Math.round(Math.cos(angle) * distance);
      int z = (int) Math.round(Math.sin(angle) * distance);
      int y = centerY + random.nextInt(29) - 14;
      int radiusX = Math.max(9, (int) Math.round(22 - t * 10 + random.nextInt(7) - 3));
      int radiusY = Math.max(5, (int) Math.round(9 - t * 3 + random.nextInt(4) - 1));
      int radiusZ = Math.max(9, (int) Math.round(21 - t * 9 + random.nextInt(7) - 3));
      int tier = t > 0.72 ? 3 : t > 0.38 ? 2 : 1;
      addAsteroid(
          asteroids,
          random,
          settings.randomArchetype(random, tier),
          x,
          y,
          z,
          radiusX,
          radiusY,
          radiusZ,
          tier,
          seed + 701L + i * 97L);
    }

    cachedCenterAsteroidSeed = seed;
    cachedCenterAsteroidY = centerY;
    cachedCenterAsteroidRadius = centerRadius;
    cachedCenterGeneratorSignature = generatorSignature;
    cachedCenterAsteroids = Collections.unmodifiableList(asteroids);
    return cachedCenterAsteroids;
  }

  private void addAsteroid(
      List<Asteroid> asteroids,
      Random random,
      CenterAsteroidArchetype archetype,
      int x,
      int y,
      int z,
      int radiusX,
      int radiusY,
      int radiusZ,
      int tier,
      long asteroidSeed) {
    double yaw = random.nextDouble() * Math.PI * 2.0;
    int salt = (int) (asteroidSeed ^ (asteroidSeed >>> 32));
    double roughness = 0.14 + random.nextDouble() * 0.12 + tier * 0.012 + archetype.roughnessBonus;
    List<Crater> craters = new ArrayList<>();
    int craterCount = Math.max(2, Math.min(10, (radiusX + radiusY + radiusZ) / 12));
    for (int i = 0; i < craterCount; i++) {
      double[] direction = randomDirection(random);
      double radius = 0.16 + random.nextDouble() * 0.19;
      if (radiusX < 16 || radiusZ < 16) {
        radius *= 0.8;
      }
      double depth = 0.035 + random.nextDouble() * 0.075;
      double rim = 0.010 + random.nextDouble() * 0.024;
      craters.add(new Crater(direction[0], direction[1], direction[2], radius, depth, rim));
    }

    List<OrePatch> orePatches = new ArrayList<>();
    int patchCount = Math.max(3, Math.min(9, (radiusX + radiusZ) / 13 + tier));
    for (int i = 0; i < patchCount; i++) {
      double[] direction = randomDirection(random);
      double radius = 0.13 + random.nextDouble() * (0.05 + tier * 0.012);
      int density = 16 + random.nextInt(16) + tier * 2;
      Material material = archetype.nodeMaterials.choose(random);
      orePatches.add(new OrePatch(direction[0], direction[1], direction[2], radius, density, material, salt + i * 131));
    }

    asteroids.add(
        new Asteroid(
            archetype,
            x,
            y,
            z,
            radiusX,
            radiusY,
            radiusZ,
            Math.cos(yaw),
            Math.sin(yaw),
            roughness,
            salt,
            tier,
            asteroidSeed,
            craters,
            orePatches));
  }

  private double[] randomDirection(Random random) {
    double y = random.nextDouble() * 2.0 - 1.0;
    double angle = random.nextDouble() * Math.PI * 2.0;
    double horizontal = Math.sqrt(Math.max(0.0, 1.0 - y * y));
    return new double[] {Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal};
  }

  private boolean insideAsteroid(Asteroid asteroid, int x, int y, int z) {
    double distance = asteroidDistance(asteroid, x, y, z);
    if (distance > 1.34) {
      return false;
    }
    return distance <= asteroidSurfaceThreshold(asteroid, x, y, z);
  }

  private Material centerStoneMaterial(int x, int y, int z) {
    Asteroid asteroid = asteroidAt(x, y, z);
    if (asteroid != null) {
      return centerStoneMaterial(asteroid, x, y, z);
    }

    int value = Math.floorMod(hash(x, y, z, 19), 10);
    if (value < 5) {
      return Material.STONE;
    }
    if (value < 8) {
      return Material.DEEPSLATE;
    }
    return Material.TUFF;
  }

  private Asteroid asteroidAt(int x, int y, int z) {
    for (Asteroid asteroid : centerAsteroids()) {
      if (insideAsteroid(asteroid, x, y, z)) {
        return asteroid;
      }
    }
    return null;
  }

  private Material centerStoneMaterial(Asteroid asteroid, int x, int y, int z) {
    double distance = asteroidDistance(asteroid, x, y, z);
    double strata =
        fbm(
            x * 0.035 + asteroid.noiseSalt,
            y * 0.075,
            z * 0.035 - asteroid.noiseSalt,
            4,
            asteroid.noiseSalt + 503);
    double crack =
        1.0
            - Math.abs(
                fbm(
                    x * 0.095,
                    y * 0.055 + asteroid.noiseSalt,
                    z * 0.095,
                    3,
                    asteroid.noiseSalt + 719));
    int speckle = Math.floorMod(hash(x, y, z, asteroid.noiseSalt + 37), 100);

    CenterAsteroidArchetype archetype = asteroid.archetype;
    int roll = hash(x, y, z, asteroid.noiseSalt + 907);
    if (distance > 0.50 && strata > 0.52 && crack > 0.93 && speckle < 10) {
      return archetype.accentBlocks.choose(roll);
    }
    if (distance > 0.86) {
      return archetype.surfacePalette.choose(roll);
    }
    if (distance < 0.40) {
      return archetype.corePalette.choose(roll + 31);
    }
    if (strata > 0.58 && speckle < 18) {
      return archetype.surfacePalette.choose(roll + 53);
    }
    return archetype.palette.choose(roll + (int) Math.round(strata * 100.0));
  }

  private Material patchNodeMaterial(int tier, int patchIndex, Random random) {
    int roll = random.nextInt(100);
    if (tier >= 4 && roll < 6) {
      return Material.EMERALD_ORE;
    }
    if (tier >= 3 && roll < 16) {
      return Material.DIAMOND_ORE;
    }
    if (tier >= 2 && roll < 30) {
      return Material.GOLD_ORE;
    }
    if (roll < 44) {
      return patchIndex % 2 == 0 ? Material.REDSTONE_ORE : Material.LAPIS_ORE;
    }
    if (roll < 64) {
      return Material.IRON_ORE;
    }
    if (roll < 84) {
      return Material.COPPER_ORE;
    }
    return Material.COAL_ORE;
  }

  private double asteroidDistance(Asteroid asteroid, int x, int y, int z) {
    double dx = x - asteroid.x;
    double dz = z - asteroid.z;
    double localX = dx * asteroid.cosYaw - dz * asteroid.sinYaw;
    double localZ = dx * asteroid.sinYaw + dz * asteroid.cosYaw;
    double localY = y - asteroid.y;
    double nx = localX / asteroid.radiusX;
    double ny = localY / asteroid.radiusY;
    double nz = localZ / asteroid.radiusZ;
    return Math.sqrt(nx * nx + ny * ny + nz * nz);
  }

  private double asteroidSurfaceThreshold(Asteroid asteroid, int x, int y, int z) {
    double dx = x - asteroid.x;
    double dz = z - asteroid.z;
    double localX = dx * asteroid.cosYaw - dz * asteroid.sinYaw;
    double localZ = dx * asteroid.sinYaw + dz * asteroid.cosYaw;
    double localY = y - asteroid.y;
    double nx = localX / asteroid.radiusX;
    double ny = localY / asteroid.radiusY;
    double nz = localZ / asteroid.radiusZ;
    double distance = Math.sqrt(nx * nx + ny * ny + nz * nz);

    double macro =
        fbm(
            (localX + asteroid.noiseSalt) * 0.035,
            (localY - asteroid.noiseSalt) * 0.035,
            localZ * 0.035,
            4,
            asteroid.noiseSalt + 11);
    double detail =
        fbm(localX * 0.13, localY * 0.13, localZ * 0.13, 3, asteroid.noiseSalt + 29);
    double threshold = 0.96 + macro * asteroid.roughness + detail * 0.055;

    if (distance > 0.55) {
      threshold += craterAdjustment(asteroid, nx / distance, ny / distance, nz / distance);
    }

    return clamp(threshold, 0.70, 1.22);
  }

  private double craterAdjustment(Asteroid asteroid, double x, double y, double z) {
    double adjustment = 0.0;
    for (Crater crater : asteroid.craters) {
      double dot = x * crater.x + y * crater.y + z * crater.z;
      if (dot <= crater.minDot) {
        continue;
      }
      double t = (dot - crater.minDot) / (1.0 - crater.minDot);
      adjustment -= crater.depth * t * t;
      double rim = 1.0 - Math.abs(t - 0.12) / 0.12;
      if (rim > 0.0) {
        adjustment += crater.rim * rim;
      }
    }
    return adjustment;
  }

  private double fbm(double x, double y, double z, int octaves, int salt) {
    double total = 0.0;
    double amplitude = 1.0;
    double frequency = 1.0;
    double normalizer = 0.0;
    for (int octave = 0; octave < octaves; octave++) {
      total += (valueNoise(x * frequency, y * frequency, z * frequency, salt + octave * 149) * 2.0 - 1.0) * amplitude;
      normalizer += amplitude;
      amplitude *= 0.5;
      frequency *= 2.0;
    }
    return total / normalizer;
  }

  private double valueNoise(double x, double y, double z, int salt) {
    int x0 = (int) Math.floor(x);
    int y0 = (int) Math.floor(y);
    int z0 = (int) Math.floor(z);
    double tx = x - x0;
    double ty = y - y0;
    double tz = z - z0;
    double u = fade(tx);
    double v = fade(ty);
    double w = fade(tz);

    double x00 = lerp(hashUnit(x0, y0, z0, salt), hashUnit(x0 + 1, y0, z0, salt), u);
    double x10 = lerp(hashUnit(x0, y0 + 1, z0, salt), hashUnit(x0 + 1, y0 + 1, z0, salt), u);
    double x01 = lerp(hashUnit(x0, y0, z0 + 1, salt), hashUnit(x0 + 1, y0, z0 + 1, salt), u);
    double x11 =
        lerp(hashUnit(x0, y0 + 1, z0 + 1, salt), hashUnit(x0 + 1, y0 + 1, z0 + 1, salt), u);
    double y0Value = lerp(x00, x10, v);
    double y1Value = lerp(x01, x11, v);
    return lerp(y0Value, y1Value, w);
  }

  private double hashUnit(int x, int y, int z, int salt) {
    return Math.floorMod(hash(x, y, z, salt), 10_000) / 10_000.0;
  }

  private double fade(double value) {
    return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
  }

  private double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private int hash(int x, int y, int z, int salt) {
    long h = x * 73428767L ^ y * 912931L ^ z * 438289L ^ salt * 83492791L;
    h ^= centerSeed;
    h ^= h >>> 33;
    h *= 0xff51afd7ed558ccdL;
    h ^= h >>> 33;
    return (int) h;
  }

  private int netherHash(int x, int y, int z, int salt) {
    long h = x * 912931L ^ y * 73428767L ^ z * 438289L ^ salt * 19349663L;
    h ^= netherSeed;
    h ^= h >>> 33;
    h *= 0xc4ceb9fe1a85ec53L;
    h ^= h >>> 33;
    return (int) h;
  }

  private Block blockFromKey(String key) {
    String[] parts = key.split(",");
    if (parts.length != 4) {
      return null;
    }
    World world = Bukkit.getWorld(parts[0]);
    if (world == null) {
      return null;
    }
    try {
      return world.getBlockAt(
          Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static long packBlock(int x, int y, int z) {
    return (((long) x & PACKED_XZ_MASK) << PACKED_X_SHIFT)
        | (((long) y & PACKED_Y_MASK) << PACKED_Y_SHIFT)
        | ((long) z & PACKED_XZ_MASK);
  }

  private static int unpackX(long packed) {
    return unpackSigned((packed >> PACKED_X_SHIFT) & PACKED_XZ_MASK, PACKED_XZ_BITS);
  }

  private static int unpackY(long packed) {
    return unpackSigned((packed >> PACKED_Y_SHIFT) & PACKED_Y_MASK, PACKED_Y_BITS);
  }

  private static int unpackZ(long packed) {
    return unpackSigned(packed & PACKED_XZ_MASK, PACKED_XZ_BITS);
  }

  private static int unpackSigned(long value, int bits) {
    long signBit = 1L << (bits - 1);
    return (int) ((value ^ signBit) - signBit);
  }

  private String locationKey(Location location) {
    return locationKey(
        location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
  }

  private String locationKey(String world, int x, int y, int z) {
    return world + "," + x + "," + y + "," + z;
  }

  private String getWorldName() {
    return getConfiguredWorldName("world-name", "world");
  }

  private String getNetherWorldName() {
    return getConfiguredWorldName("nether-world-name", "world_nether");
  }

  private String getEndWorldName() {
    return getConfiguredWorldName("end-world-name", "world_the_end");
  }

  private String getConfiguredWorldName(String path, String fallback) {
    String worldName = getConfig().getString(path, fallback);
    return worldName == null || worldName.isBlank() ? fallback : worldName.trim();
  }

  private boolean isSkyblockOverworld(Location location) {
    return location != null && isSkyblockOverworld(location.getWorld());
  }

  private boolean isSkyblockOverworld(World world) {
    return isWorld(world, getWorldName());
  }

  private boolean isManagedNetherWorld(World world) {
    return isWorld(world, getNetherWorldName());
  }

  private boolean isWorld(World world, String worldName) {
    return world != null && world.getName().equals(worldName);
  }

  private Location islandSpawnLocation(Island island) {
    return island.spawnLocation(getOverworld(), getConfig().getInt("island-y", 88));
  }

  private String islandCoordinateText(Island island) {
    return "x=" + island.x + ", y=" + (getConfig().getInt("island-y", 88) + 2) + ", z=" + island.z;
  }

  private String spawnCoordinateText(Location location) {
    return String.format(
        Locale.ROOT,
        "x=%.1f, y=%.1f, z=%.1f",
        location.getX(),
        location.getY(),
        location.getZ());
  }

  private void broadcast(String message) {
    Bukkit.getConsoleSender().sendMessage(message);
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(message);
    }
  }

  private List<String> filter(List<String> values, String prefix) {
    String lower = prefix.toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
  }

  private static final class CenterGeneratorSettings {
    final List<String> warnings;
    final List<CenterAsteroidArchetype> archetypes;
    final CenterAsteroidArchetype centralArchetype;
    final int centralTier;
    final int centralRadiusX;
    final int centralRadiusY;
    final int centralRadiusZ;
    final int lobeCount;
    final int satelliteMin;
    final int satelliteMax;
    final int maxDecorationsPerAsteroid;
    final int maxHazardsPerAsteroid;
    final boolean structuresEnabled;
    final boolean centralStructure;
    final int structureCount;
    final int structureMinTier;
    final int eventNodeCount;
    final int eventCrateCount;
    final WeightedMaterialSet eventNodeMaterials;
    final Map<Material, Integer> eventCrateLoot;
    final int eventCrateShardMin;
    final int eventCrateShardMax;
    final Map<Material, Integer> crateLoot;
    final int crateShardMin;
    final int crateShardMax;
    final int eventCrateSalt;
    final int structureSalt;

    CenterGeneratorSettings(
        List<String> warnings,
        List<CenterAsteroidArchetype> archetypes,
        CenterAsteroidArchetype centralArchetype,
        int centralTier,
        int centralRadiusX,
        int centralRadiusY,
        int centralRadiusZ,
        int lobeCount,
        int satelliteMin,
        int satelliteMax,
        int maxDecorationsPerAsteroid,
        int maxHazardsPerAsteroid,
        boolean structuresEnabled,
        boolean centralStructure,
        int structureCount,
        int structureMinTier,
        int eventNodeCount,
        int eventCrateCount,
        WeightedMaterialSet eventNodeMaterials,
        Map<Material, Integer> eventCrateLoot,
        int eventCrateShardMin,
        int eventCrateShardMax,
        Map<Material, Integer> crateLoot,
        int crateShardMin,
        int crateShardMax,
        int eventCrateSalt,
        int structureSalt) {
      this.warnings = warnings;
      this.archetypes = archetypes;
      this.centralArchetype = centralArchetype;
      this.centralTier = centralTier;
      this.centralRadiusX = centralRadiusX;
      this.centralRadiusY = centralRadiusY;
      this.centralRadiusZ = centralRadiusZ;
      this.lobeCount = lobeCount;
      this.satelliteMin = Math.min(satelliteMin, satelliteMax);
      this.satelliteMax = Math.max(satelliteMin, satelliteMax);
      this.maxDecorationsPerAsteroid = maxDecorationsPerAsteroid;
      this.maxHazardsPerAsteroid = maxHazardsPerAsteroid;
      this.structuresEnabled = structuresEnabled;
      this.centralStructure = centralStructure;
      this.structureCount = structureCount;
      this.structureMinTier = structureMinTier;
      this.eventNodeCount = eventNodeCount;
      this.eventCrateCount = eventCrateCount;
      this.eventNodeMaterials = eventNodeMaterials;
      this.eventCrateLoot = eventCrateLoot;
      this.eventCrateShardMin = eventCrateShardMin;
      this.eventCrateShardMax = eventCrateShardMax;
      this.crateLoot = crateLoot;
      this.crateShardMin = crateShardMin;
      this.crateShardMax = crateShardMax;
      this.eventCrateSalt = eventCrateSalt;
      this.structureSalt = structureSalt;
    }

    CenterAsteroidArchetype randomArchetype(Random random, int tier) {
      int totalWeight = 0;
      for (CenterAsteroidArchetype archetype : archetypes) {
        if (tier >= archetype.minTier && tier <= archetype.maxTier) {
          totalWeight += archetype.weight;
        }
      }
      if (totalWeight <= 0) {
        return archetypes.get(random.nextInt(archetypes.size()));
      }

      int roll = random.nextInt(totalWeight);
      for (CenterAsteroidArchetype archetype : archetypes) {
        if (tier < archetype.minTier || tier > archetype.maxTier) {
          continue;
        }
        roll -= archetype.weight;
        if (roll < 0) {
          return archetype;
        }
      }
      return archetypes.get(0);
    }
  }

  private static final class CenterAsteroidArchetype {
    final String id;
    final int weight;
    final int minTier;
    final int maxTier;
    final double roughnessBonus;
    final double decorationChance;
    final double hazardChance;
    final double ruinChance;
    final double crateChance;
    final double spawnerChance;
    final WeightedMaterialSet palette;
    final WeightedMaterialSet surfacePalette;
    final WeightedMaterialSet corePalette;
    final WeightedMaterialSet accentBlocks;
    final WeightedMaterialSet decorations;
    final WeightedMaterialSet hazardBlocks;
    final WeightedMaterialSet nodeMaterials;
    final Map<Material, Integer> crateLoot;
    final List<EntityType> spawnerTypes;

    CenterAsteroidArchetype(
        String id,
        int weight,
        int minTier,
        int maxTier,
        double roughnessBonus,
        double decorationChance,
        double hazardChance,
        double ruinChance,
        double crateChance,
        double spawnerChance,
        WeightedMaterialSet palette,
        WeightedMaterialSet surfacePalette,
        WeightedMaterialSet corePalette,
        WeightedMaterialSet accentBlocks,
        WeightedMaterialSet decorations,
        WeightedMaterialSet hazardBlocks,
        WeightedMaterialSet nodeMaterials,
        Map<Material, Integer> crateLoot,
        List<EntityType> spawnerTypes) {
      this.id = id;
      this.weight = weight;
      this.minTier = Math.min(minTier, maxTier);
      this.maxTier = Math.max(minTier, maxTier);
      this.roughnessBonus = roughnessBonus;
      this.decorationChance = decorationChance;
      this.hazardChance = hazardChance;
      this.ruinChance = ruinChance;
      this.crateChance = crateChance;
      this.spawnerChance = spawnerChance;
      this.palette = palette;
      this.surfacePalette = surfacePalette;
      this.corePalette = corePalette;
      this.accentBlocks = accentBlocks;
      this.decorations = decorations;
      this.hazardBlocks = hazardBlocks;
      this.nodeMaterials = nodeMaterials;
      this.crateLoot = crateLoot;
      this.spawnerTypes = spawnerTypes;
    }
  }

  private static final class WeightedMaterialSet {
    final Map<Material, Integer> weights;
    final Material fallback;
    final int totalWeight;

    WeightedMaterialSet(Map<Material, Integer> weights, Material fallback) {
      this.weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
      this.fallback = fallback;
      int total = 0;
      for (int weight : weights.values()) {
        total += Math.max(0, weight);
      }
      this.totalWeight = total;
    }

    Material choose(Random random) {
      if (totalWeight <= 0) {
        return fallback;
      }
      return choose(random.nextInt(totalWeight));
    }

    Material choose(int roll) {
      if (totalWeight <= 0) {
        return fallback;
      }
      int remaining = Math.floorMod(roll, totalWeight);
      for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
        remaining -= Math.max(0, entry.getValue());
        if (remaining < 0) {
          return entry.getKey();
        }
      }
      return fallback;
    }
  }

  private enum NetherIslandType {
    HUB,
    QUARTZ_COMET,
    CRIMSON_WELL,
    BLAZE_CRUCIBLE,
    BASTION_SPLINTER,
    DEBRIS_RIFT
  }

  private static final class NetherIsland {
    final NetherIslandType type;
    final int x;
    final int y;
    final int z;
    final int radiusX;
    final int radiusY;
    final int radiusZ;
    final int salt;

    NetherIsland(
        NetherIslandType type,
        int x,
        int y,
        int z,
        int radiusX,
        int radiusY,
        int radiusZ,
        int salt) {
      this.type = type;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radiusX = radiusX;
      this.radiusY = radiusY;
      this.radiusZ = radiusZ;
      this.salt = salt;
    }
  }

  private static final class NetherHotspot {
    final String worldName;
    final int x;
    final int y;
    final int z;
    final int radius;

    NetherHotspot(String worldName, int x, int y, int z, int radius) {
      this.worldName = worldName;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
    }

    boolean contains(Location location) {
      if (location == null || location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
        return false;
      }
      double dx = location.getX() - x;
      double dz = location.getZ() - z;
      return dx * dx + dz * dz <= radius * radius && Math.abs(location.getY() - y) <= 48;
    }
  }

  private static final class SkyblockTeam {
    String name;
    String normalizedName;
    UUID leaderId;
    final List<UUID> memberIds;

    SkyblockTeam(String name, String normalizedName, UUID leaderId, List<UUID> memberIds) {
      this.name = name;
      this.normalizedName = normalizedName;
      this.leaderId = leaderId;
      this.memberIds = memberIds;
    }

    boolean isLeader(UUID playerId) {
      return leaderId.equals(playerId);
    }

    boolean hasMember(UUID playerId) {
      return memberIds.contains(playerId);
    }
  }

  private static final class TeamInvite {
    final SkyblockTeam team;
    final UUID inviterId;

    TeamInvite(SkyblockTeam team, UUID inviterId) {
      this.team = team;
      this.inviterId = inviterId;
    }
  }

  private static final class Island {
    final UUID ownerId;
    final String ownerName;
    final int slot;
    final int x;
    final int z;
    final Set<String> upgrades;
    SpawnPoint spawnPoint;

    Island(UUID ownerId, String ownerName, int slot, int x, int z, Set<String> upgrades, SpawnPoint spawnPoint) {
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.slot = slot;
      this.x = x;
      this.z = z;
      this.upgrades = upgrades;
      this.spawnPoint = spawnPoint;
    }

    Location spawnLocation(World world, int islandY) {
      if (spawnPoint != null) {
        return spawnPoint.toLocation(world);
      }
      return new Location(world, x + 0.5, islandY + 2.0, z + 0.5);
    }
  }

  private static final class SpawnPoint {
    final double x;
    final double y;
    final double z;
    final float yaw;
    final float pitch;

    SpawnPoint(double x, double y, double z, float yaw, float pitch) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
    }

    static SpawnPoint fromLocation(Location location) {
      return new SpawnPoint(
          location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    Location toLocation(World world) {
      return new Location(world, x, y, z, yaw, pitch);
    }
  }

  private static final class MinedNode {
    final Material material;
    final long respawnAtMillis;

    MinedNode(Material material, long respawnAtMillis) {
      this.material = material;
      this.respawnAtMillis = respawnAtMillis;
    }
  }

  private static final class Upgrade {
    final String id;
    final String name;
    final String description;
    final Material icon;
    final int shardCost;
    final Map<Material, Integer> materialCosts;

    Upgrade(
        String id,
        String name,
        String description,
        Material icon,
        int shardCost,
        Map<Material, Integer> materialCosts) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.icon = icon;
      this.shardCost = shardCost;
      this.materialCosts = materialCosts;
    }

    String costText() {
      String materials =
          materialCosts.entrySet().stream()
              .map(entry -> entry.getValue() + " " + materialName(entry.getKey()))
              .collect(Collectors.joining(", "));
      if (materials.isEmpty()) {
        return shardCost + " shards";
      }
      return shardCost + " shards, " + materials;
    }
  }

  private enum CenterStructureType {
    ANCIENT_GATE,
    MINING_OUTPOST,
    CRYSTAL_SHRINE,
    BROKEN_RELAY,
    WATCH_SPIRE
  }

  private static final class GeneratedAsteroid {
    final Asteroid asteroid;
    final int index;
    final List<Long> surfaceBlocks;

    GeneratedAsteroid(Asteroid asteroid, int index, List<Long> surfaceBlocks) {
      this.asteroid = asteroid;
      this.index = index;
      this.surfaceBlocks = surfaceBlocks;
    }
  }

  private static final class AsteroidBlocks {
    final List<Long> solidBlocks;
    final List<Long> surfaceBlocks;

    AsteroidBlocks(List<Long> solidBlocks, List<Long> surfaceBlocks) {
      this.solidBlocks = solidBlocks;
      this.surfaceBlocks = surfaceBlocks;
    }
  }

  private static final class Asteroid {
    final CenterAsteroidArchetype archetype;
    final int x;
    final int y;
    final int z;
    final int radiusX;
    final int radiusY;
    final int radiusZ;
    final double cosYaw;
    final double sinYaw;
    final double roughness;
    final int noiseSalt;
    final int tier;
    final long seed;
    final List<Crater> craters;
    final List<OrePatch> orePatches;

    Asteroid(
        CenterAsteroidArchetype archetype,
        int x,
        int y,
        int z,
        int radiusX,
        int radiusY,
        int radiusZ,
        double cosYaw,
        double sinYaw,
        double roughness,
        int noiseSalt,
        int tier,
        long seed,
        List<Crater> craters,
        List<OrePatch> orePatches) {
      this.archetype = archetype;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radiusX = radiusX;
      this.radiusY = radiusY;
      this.radiusZ = radiusZ;
      this.cosYaw = cosYaw;
      this.sinYaw = sinYaw;
      this.roughness = roughness;
      this.noiseSalt = noiseSalt;
      this.tier = tier;
      this.seed = seed;
      this.craters = craters;
      this.orePatches = orePatches;
    }
  }

  private static final class Crater {
    final double x;
    final double y;
    final double z;
    final double minDot;
    final double depth;
    final double rim;

    Crater(double x, double y, double z, double radius, double depth, double rim) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.minDot = Math.cos(radius);
      this.depth = depth;
      this.rim = rim;
    }
  }

  private static final class OrePatch {
    final double x;
    final double y;
    final double z;
    final double minDot;
    final int density;
    final Material material;
    final int salt;

    OrePatch(double x, double y, double z, double radius, int density, Material material, int salt) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.minDot = Math.cos(radius);
      this.density = density;
      this.material = material;
      this.salt = salt;
    }
  }

  private final class CenterAsteroidChunkGenerator extends VoidChunkGenerator {
    @Override
    public void generateNoise(
        WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
      if (worldInfo.getEnvironment() != World.Environment.NORMAL || !centerChunkIntersectsField(chunkX, chunkZ)) {
        return;
      }

      int chunkMinX = chunkX << 4;
      int chunkMinZ = chunkZ << 4;
      int chunkMaxX = chunkMinX + 15;
      int chunkMaxZ = chunkMinZ + 15;
      int minHeight = chunkData.getMinHeight();
      int maxHeight = chunkData.getMaxHeight() - 1;

      for (Asteroid asteroid : centerAsteroids()) {
        int minX = Math.max(chunkMinX, asteroid.x - asteroid.radiusX - 8);
        int maxX = Math.min(chunkMaxX, asteroid.x + asteroid.radiusX + 8);
        int minZ = Math.max(chunkMinZ, asteroid.z - asteroid.radiusZ - 8);
        int maxZ = Math.min(chunkMaxZ, asteroid.z + asteroid.radiusZ + 8);
        if (minX > maxX || minZ > maxZ) {
          continue;
        }

        int minY = Math.max(minHeight, asteroid.y - asteroid.radiusY - 8);
        int maxY = Math.min(maxHeight, asteroid.y + asteroid.radiusY + 8);
        for (int x = minX; x <= maxX; x++) {
          int localX = x - chunkMinX;
          for (int z = minZ; z <= maxZ; z++) {
            int localZ = z - chunkMinZ;
            for (int y = minY; y <= maxY; y++) {
              if (insideAsteroid(asteroid, x, y, z)) {
                chunkData.setBlock(localX, y, localZ, centerStoneMaterial(asteroid, x, y, z));
              }
            }
          }
        }
      }
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return worldInfo.getEnvironment() == World.Environment.NORMAL && centerChunkIntersectsField(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateNoise() {
      return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
      return new Location(world, 0.5, getConfig().getInt("center-y", 92) + 24.0, 0.5);
    }

    private boolean centerChunkIntersectsField(int chunkX, int chunkZ) {
      int chunkMinX = chunkX << 4;
      int chunkMinZ = chunkZ << 4;
      int chunkMaxX = chunkMinX + 15;
      int chunkMaxZ = chunkMinZ + 15;
      int radius = Math.max(96, getConfig().getInt("center-radius", 160)) + 80;
      int closestX = clamp(0, chunkMinX, chunkMaxX);
      int closestZ = clamp(0, chunkMinZ, chunkMaxZ);
      return closestX * closestX + closestZ * closestZ <= radius * radius;
    }
  }

  private static final class PlayerIdentity {
    final UUID uuid;
    final String name;

    PlayerIdentity(UUID uuid, String name) {
      this.uuid = uuid;
      this.name = name;
    }
  }

  private static final class UpgradeMenuHolder implements InventoryHolder {
    final UUID ownerId;
    Inventory inventory;

    UpgradeMenuHolder(UUID ownerId) {
      this.ownerId = ownerId;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  private static class VoidChunkGenerator extends ChunkGenerator {
    private final BiomeProvider managedBiomeProvider = new ManagedBiomeProvider();

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
      return managedBiomeProvider;
    }

    @Override
    public void generateNoise(
        WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public void generateSurface(
        WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public void generateBedrock(
        WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public void generateCaves(
        WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
      return worldInfo.getMinHeight();
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
      return createChunkData(world);
    }

    @Override
    public boolean shouldGenerateNoise() {
      return false;
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
      return false;
    }

    @Override
    public boolean shouldGenerateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
      return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
      return false;
    }

    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
      return false;
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
      return false;
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
      return false;
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
      return false;
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
      return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
      return new Location(world, 0.5, 116, 0.5);
    }
  }

  private static final class ManagedBiomeProvider extends BiomeProvider {
    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
      return worldInfo.getEnvironment() == World.Environment.NORMAL ? Biome.PLAINS : Biome.THE_VOID;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
      return Collections.singletonList(
          worldInfo.getEnvironment() == World.Environment.NORMAL ? Biome.PLAINS : Biome.THE_VOID);
    }
  }
}
