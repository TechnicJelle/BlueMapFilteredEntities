package com.technicjelle.bluemapfilteredentities;

import com.flowpowered.math.vector.Vector3d;
import com.google.gson.reflect.TypeToken;
import com.technicjelle.BMUtils.BMCopy;
import com.technicjelle.UpdateChecker;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
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
import java.util.stream.Stream;

import static com.technicjelle.bluemapfilteredentities.Constants.*;

public final class BlueMapFilteredEntities extends JavaPlugin {
	private UpdateChecker updateChecker;
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	//TODO: Replace String with BlueMapMap when the bmAPI is updated to include the BlueMapMap hashcode:
	private final Map<HashedBlueMapMap, Map<String, FilterSet>> trackingMaps = new HashMap<>();

	@Override
	public void onLoad() {
		new Metrics(this, 21976);

		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapFilteredEntities", getDescription().getVersion());
		updateChecker.checkAsync();

		BlueMapAPI.onEnable(onEnableListenerConfig);
		BlueMapAPI.onDisable(onDisableListener);
	}

	@Override
	public void onEnable() {
		BlueMapAPI.onEnable(onEnableListenerMaps);
	}

	private final Consumer<BlueMapAPI> onEnableListenerConfig = api -> {
		// Copy script & style to webapp
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
					getLogger().log(Level.SEVERE, "Failed to copy default config for map: " + map.getId(), e);
				}
			}
		}

		// Copy all files in the icon folder to the webapp
		Path iconFolder = getDataFolder().toPath().resolve("icons");
		try {
			Files.createDirectories(iconFolder);
			try (final Stream<Path> files = Files.walk(iconFolder)) {
				files.filter(Files::isRegularFile).forEach(path -> {
					Path relativeToIconFolder = iconFolder.relativize(path);
					try {
						BMCopy.fileToWebApp(api, path, "bmfe-icons/" + relativeToIconFolder, true);
					} catch (IOException e) {
						getLogger().log(Level.SEVERE, "Failed to copy icon '" + path + "' to BlueMap webapp!", e);
					}
				});
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "Failed to walk icon folder!", e);
			}
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to create icon folder!", e);
		}

		// Load configs
		File configPath = getDataFolder();
		File[] files = configPath.listFiles();
		if (files == null) return;

		trackingMaps.clear();
		for (File file : files) {
			if (!file.getName().endsWith(CONF_EXT)) continue;

			String mapId = file.getName().substring(0, file.getName().length() - CONF_EXT.length());

			Optional<BlueMapMap> oMap = api.getMap(mapId);
			if (oMap.isEmpty()) {
				getLogger().log(Level.SEVERE, "BlueMap Map not found: " + mapId);
				continue;
			}
			BlueMapMap map = oMap.get();

			getLogger().info("Loading config for map: " + map.getId());

			HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
					.defaultOptions(options -> options.implicitInitialization(false))
					.path(file.toPath()).build();

			CommentedConfigurationNode root;
			try {
				root = loader.load();
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Failed to load config for map: " + map.getId(), e);
				continue;
			}
			if (root == null) {
				getLogger().log(Level.SEVERE, "Failed to load config root for map: " + map.getId());
				continue;
			}

			try {
				ConfigurationNode configFilterSetsNode = root.node(NODE_FILTER_SETS);
				if (configFilterSetsNode.virtual()) throw new Exception("filter-sets property is required");
				Type filterSetType = new TypeToken<Map<String, FilterSet>>() {}.getType();
				Object configFilterSetsMaybe = configFilterSetsNode.get(filterSetType);
				Map<String, FilterSet> configFilterSets = (Map<String, FilterSet>) configFilterSetsMaybe;
				if (configFilterSets == null) throw new Exception("filter-sets property was null");

				Map<String, FilterSet> validFilterSets = new HashMap<>();
				for (var entry : configFilterSets.entrySet()) {
					String filterSetId = entry.getKey();
					FilterSet filterSet = entry.getValue();
					getLogger().info("Loading filter set: " + filterSetId);
					if (filterSet == null) {
						getLogger().log(Level.SEVERE, "Filter Set '" + filterSetId + "' is null");
						continue;
					}
					boolean valid = filterSet.checkValidAndInit(getLogger(), api);
					if (valid) {
						validFilterSets.put(filterSetId, filterSet);
					}
					if (!validFilterSets.isEmpty())
						trackingMaps.put(new HashedBlueMapMap(map), validFilterSets);
				}
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Failed to load filters for map: " + map.getId(), e);
			}
		}
	};

	private final Consumer<BlueMapAPI> onEnableListenerMaps = api -> {
		updateChecker.logUpdateMessage(getLogger());

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> processMaps(api), 0, 20 * 10);
	};

	private void processMaps(BlueMapAPI api) {
		if (trackingMaps.isEmpty()) return;
		long millisAtStart = System.currentTimeMillis();

		CompletableFuture<Void>[] futures = new CompletableFuture[trackingMaps.size()];
		int i = 0;
		for (var entry : trackingMaps.entrySet()) {
			BlueMapMap map = entry.getKey().getMap();
			Map<String, FilterSet> filterSetMap = entry.getValue();

			if (filterSetMap.isEmpty()) continue;

			World world = findBukkitWorldFromBlueMapWorld(api, map.getWorld());
			if (world == null) {
				getLogger().warning("Failed to get Bukkit world for BlueMapMap: " + map.getId());
				continue;
			}
			List<Entity> entities = world.getEntities();

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processEntities(map, filterSetMap, entities), executorService);
			futures[i] = future;
			i++;
		}

		CompletableFuture.allOf(futures)
				.thenRun(() -> {
					long diff = System.currentTimeMillis() - millisAtStart;
					if (diff > 15) {
						getLogger().warning("Took " + (System.currentTimeMillis() - millisAtStart) + "ms to add entity markers for all maps!\n" +
								"This is fine for the first run, but if it more often, you might want to reduce the number of entities or filters.");
					}
				})
		;
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

	private void processEntities(BlueMapMap map, Map<String, FilterSet> filterSetMap, List<Entity> worldEntities) {
		for (var entry : filterSetMap.entrySet()) {
			String filterSetId = entry.getKey();
			FilterSet filterSet = entry.getValue();
			assert filterSet.getFilters() != null;

			Map<Entity, Filter> entityMatchedByFilterMap = new HashMap<>();
			List<Entity> filteredEntities = new ArrayList<>();
			for (Entity entity : worldEntities) {
				if (entity instanceof Player) continue;
				for (Filter filter : filterSet.getFilters()) {
					if (filter.matches(entity)) {
						entityMatchedByFilterMap.put(entity, filter);
						filteredEntities.add(entity);
						break;
					}
				}
			}

			String key = map.getId() + "_" + filterSetId + "_entities";
			MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(key, id -> filterSet.createMarkerset());

			markerSet.getMarkers().clear();

			for (Entity entity : filteredEntities) {
				Filter matchedFilter = entityMatchedByFilterMap.get(entity);

				//TODO: Add special data for Item Frames
				//TODO: Add special data for Armor Stands

				String entityInfoTemplate = matchedFilter.getPopupInfoWithTemplate();
				assert entityInfoTemplate != null;
				String entityInfo = entityInfoTemplate.
						replace(ENTITY_PROPERTY_TYPE, entity.getType().name()).
						replace(ENTITY_PROPERTY_NAME, entity.getName()).
						replace(ENTITY_PROPERTY_UUID, entity.getUniqueId().toString()).
						replace(ENTITY_PROPERTY_SPAWN_REASON, entity.getEntitySpawnReason().name()).
						replace(ENTITY_PROPERTY_CUSTOM_NAME, getNullableString(getCustomName(entity))).
						replace(ENTITY_PROPERTY_X, String.valueOf(entity.getLocation().getBlockX())).
						replace(ENTITY_PROPERTY_Y, String.valueOf(entity.getLocation().getBlockY())).
						replace(ENTITY_PROPERTY_Z, String.valueOf(entity.getLocation().getBlockZ())).
						replace(ENTITY_PROPERTY_WORLD, entity.getWorld().getName()).
						replace(ENTITY_PROPERTY_SCOREBOARD_TAGS, collectionToString(entity.getScoreboardTags()));

				double yOffset = entity.getHeight() / 2.0;
				Vector3d position = new Vector3d(entity.getLocation().getX(), entity.getLocation().getY() + yOffset, entity.getLocation().getZ());
				POIMarker marker = POIMarker.builder()
						.label(entityInfo.split("\n")[0]) // First line of entityInfo
						.detail(entityInfo.replace("\n", "<br>"))
						.styleClasses("bmfe-entity")
						.position(position)
						.build();

				String icon = matchedFilter.getIcon();
				if (icon != null) {
					marker.setIcon("assets/bmfe-icons/" + icon, matchedFilter.getAnchor());
				}

				Double maxDistance = matchedFilter.getMaxDistance();
				if (maxDistance != null) {
					marker.setMaxDistance(maxDistance);
				}

				markerSet.put("bmfe." + entity.getUniqueId(), marker);
			}
		}
	}

	private final Consumer<BlueMapAPI> onDisableListener = api -> Bukkit.getScheduler().cancelTasks(this);

	@Override
	public void onDisable() {
		BlueMapAPI.unregisterListener(onEnableListenerConfig);
		BlueMapAPI.unregisterListener(onEnableListenerMaps);
		BlueMapAPI.unregisterListener(onDisableListener);
	}
}
