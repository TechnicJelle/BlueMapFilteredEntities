package com.technicjelle.bluemapfilteredentities;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.BMCopy;
import com.technicjelle.UpdateChecker;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class BlueMapFilteredEntities extends JavaPlugin {
	private static final String CONF_EXT = ".conf";
	private static final String NODE_FILTERS = "filters";

	private UpdateChecker updateChecker;
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	//TODO: Replace String with BlueMapMap when the bmAPI is updated to include the BlueMapMap hashcode:
	private final Map<String, List<Filter>> trackingMaps = new HashMap<>();

	@Override
	public void onLoad() {
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	@Override
	public void onEnable() {
		new Metrics(this, 21976);

		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapFilteredEntities", getDescription().getVersion());
		updateChecker.checkAsync();
	}

	private final Consumer<BlueMapAPI> onEnableListener = api -> {
		updateChecker.logUpdateMessage(getLogger());

		try {
			BMCopy.jarResourceToWebApp(api, getClassLoader(), "bmfe.js", "bmfe.js", true);
			BMCopy.jarResourceToWebApp(api, getClassLoader(), "bmfe.css", "bmfe.css", true);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to copy resources to BlueMap webapp!", e);
		}

		// First time? Create configs
		if (getDataFolder().mkdirs()) {
			getLogger().info("Created plugin config directory");

			for (BlueMapMap map : api.getMaps()) {
				getLogger().info("Creating default config for map: " + map.getId());

				Path mapConfigPath = getDataFolder().toPath().resolve(map.getId() + CONF_EXT);
				try {
					Files.copy(Objects.requireNonNull(getResource("default.conf")), mapConfigPath);
				} catch (IOException e) {
					getLogger().log(Level.SEVERE, "Failed to copy default config for map " + map.getId(), e);
				}
			}
		}

		// Load configs
		getLogger().info("Loading existing configs");

		File configPath = getDataFolder();
		File[] files = configPath.listFiles();
		if (files == null) return;

		trackingMaps.clear();
		for (File file : files) {
			if (!file.getName().endsWith(CONF_EXT)) continue;

			String mapId = file.getName().substring(0, file.getName().length() - CONF_EXT.length());

			Optional<BlueMapMap> oMap = api.getMap(mapId);
			if (oMap.isEmpty()) {
				getLogger().warning("Map not found: " + mapId);
				continue;
			}
			BlueMapMap map = oMap.get();

			getLogger().info("Loading config for map: " + map.getId());

			List<Filter> filters = new ArrayList<>();
			trackingMaps.put(mapId, filters);

			HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
					.defaultOptions(options -> options.implicitInitialization(false))
					.path(file.toPath()).build();

			CommentedConfigurationNode root;
			try {
				root = loader.load();
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Failed to load config for map " + map.getId(), e);
				continue;
			}
			if (root == null) {
				getLogger().warning("Failed to load config root for map " + map.getId());
				continue;
			}

			try {
				ConfigurationNode filtersNode = root.node(NODE_FILTERS);
				if (filtersNode.virtual()) throw new Exception("filters property is required");
				List<? extends ConfigurationNode> children = filtersNode.childrenList();
				for (ConfigurationNode child : children) {
					Filter filter = child.get(Filter.class);
					if (filter == null) {
						throw new Exception("Filter was null: " + child);
					}
					boolean valid = filter.checkValidAndInit(getLogger());
					if (valid) {
						filters.add(filter);
					}
				}
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Failed to load filters for map " + map.getId(), e);
				continue;
			}

			getLogger().info("Loaded config for map: " + map.getId());
		}

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> processMaps(api), 0, 20 * 10);
	};

	private void processMaps(BlueMapAPI api) {
		long millisAtStart = System.currentTimeMillis();

		CompletableFuture<Void>[] futures = new CompletableFuture[trackingMaps.size()];
		int i = 0;
		for (Map.Entry<String, List<Filter>> entry : trackingMaps.entrySet()) {
			BlueMapMap map = api.getMap(entry.getKey()).orElse(null);
			if (map == null) {
				getLogger().warning("Failed to get BlueMapMap for map: " + entry.getKey());
				continue;
			}
			List<Filter> filters = entry.getValue();

			if (filters.isEmpty()) continue;

			World world = findBukkitWorldFromBlueMapWorld(api, map.getWorld());
			if (world == null) {
				getLogger().warning("Failed to get Bukkit world for BlueMapMap: " + map.getId());
				continue;
			}
			List<Entity> entities = world.getEntities();

			//TODO: Actually filter the entities here
//			for (Filter filter : filters) {
//				getLogger().info(filter.print());
//			}

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processEntities(map, entities), executorService);
			futures[i] = future;
			i++;
		}

		CompletableFuture.allOf(futures).thenRun(() ->
				getLogger().info("Took " + (System.currentTimeMillis() - millisAtStart) + "ms to add entity markers for all maps."));
	}

	private static World findBukkitWorldFromBlueMapWorld(BlueMapAPI api, BlueMapWorld targetBMWorld) {
		//TODO: Replace this with a normal hashcode-based comparison when the bmAPI is updated
		String targetBMWorldId = targetBMWorld.getId();
		for (World world : Bukkit.getWorlds()) {
			BlueMapWorld tryBMWorld = api.getWorld(world.getName()).orElse(null);
			if (tryBMWorld != null && tryBMWorld.getId().equals(targetBMWorldId)) {
				return world;
			}
		}
		return null;
	}

	private void processEntities(BlueMapMap map, List<Entity> entities) {
		String key = map.getId() + "_entities";
		MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(key, id -> MarkerSet.builder()
				.label("Entities")
				.toggleable(true)
				.defaultHidden(true)
				.build());

		markerSet.getMarkers().clear();

		for (Entity entity : entities) {
			if (entity instanceof Player) continue;

			//TODO: Add special data for Item Frames
			//TODO: Add special data for Armor Stands

			String entityInfo;
			{
				StringBuilder sb = new StringBuilder();
				sb.append("Type: ").append(entity.getType());
				sb.append("\nName: ").append(entity.getName());
				sb.append("\nUUID: ").append(entity.getUniqueId());
				sb.append("\nSpawn Reason: ").append(entity.getEntitySpawnReason());
				sb.append("\nCustom Name: ").append(getCustomName(entity));
				Location location = entity.getLocation();
				sb.append("\nLocation: ").append(location.getBlockX()).append(", ").append(location.getBlockY()).append(", ").append(location.getBlockZ()).append(" (").append(location.getWorld().getName()).append(")");
				sb.append("\nScoreboard Tags: [ ").append(String.join(", ", entity.getScoreboardTags())).append(" ]");
				entityInfo = sb.toString();
			}
//			getLogger().info(entityInfo);

			Vector3d position = new Vector3d(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ());
			POIMarker marker = POIMarker.builder()
					.label(entity.getName())
					.detail(entityInfo.replace("\n", "<br>"))
					.styleClasses("bmfe-entity")
					.position(position)
					.build();
			markerSet.put("bmfe." + entity.getUniqueId(), marker);
		}
	}

	@Nullable
	public static String getCustomName(Entity entity) {
		if (entity.getType() == EntityType.DROPPED_ITEM) {
			Item item = (Item) entity;
			ItemMeta itemMeta = item.getItemStack().getItemMeta();
			if (itemMeta.hasDisplayName()) {
				return itemMeta.getDisplayName();
			}
		}

		return entity.getCustomName();
	}

	private final Consumer<BlueMapAPI> onDisableListener = api -> Bukkit.getScheduler().cancelTasks(this);

	@Override
	public void onDisable() {
		BlueMapAPI.unregisterListener(onEnableListener);
		BlueMapAPI.unregisterListener(onDisableListener);
	}
}
