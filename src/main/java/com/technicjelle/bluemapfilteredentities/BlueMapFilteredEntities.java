package com.technicjelle.bluemapfilteredentities;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.UpdateChecker;
import com.technicjelle.BMCopy;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class BlueMapFilteredEntities extends JavaPlugin {
	private UpdateChecker updateChecker;
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Override
	public void onLoad() {
		BlueMapAPI.onEnable(api -> {
			try {
				BMCopy.jarResourceToWebApp(api, getClassLoader(), "bmfe-marker-animator.js", "bmfe-marker-animator.js", true);
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "Failed to copy resources to BlueMap webapp!", e);
			}
		});
	}

	@Override
	public void onEnable() {
		new Metrics(this, 21976);

		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapFilteredEntities", getDescription().getVersion());
		updateChecker.checkAsync();

		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	private final Consumer<BlueMapAPI> onEnableListener = api -> {
		updateChecker.logUpdateMessage(getLogger());
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> processWorlds(api), 0, 20 * 10);
	};

	private void processWorlds(BlueMapAPI api) {
		long millisAtStart = System.currentTimeMillis();

		List<World> worlds = getServer().getWorlds();

		CompletableFuture<Void>[] futures = new CompletableFuture[worlds.size()];

		for (int i = 0; i < worlds.size(); i++) {
			World world = worlds.get(i);
			BlueMapWorld blueMapWorld = api.getWorld(world).orElse(null);
			if (blueMapWorld == null) {
				getLogger().warning("Failed to get BlueMapWorld for world: " + world.getName());
				continue;
			}

			if (blueMapWorld.getMaps().isEmpty()) continue;

			List<Entity> entities = world.getEntities();
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processEntities(blueMapWorld, entities), executorService);
			futures[i] = future;
		}

		CompletableFuture.allOf(futures).thenRun(() ->
				getLogger().info("Took " + (System.currentTimeMillis() - millisAtStart) + "ms to add entity markers for all worlds."));
	}

	private void processEntities(BlueMapWorld blueMapWorld, List<Entity> entities) {
		MarkerSet markerSet = MarkerSet.builder()
				.label("Entities")
				.toggleable(true)
				.defaultHidden(true)
				.build();

		for (BlueMapMap map : blueMapWorld.getMaps()) {
			map.getMarkerSets().put(map.getId() + "_entities", markerSet);
		}

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
					.position(position)
					.build();
					markerSet.put("bmfe." + entity.getUniqueId(), marker);
		}
	}

	@Nullable
	String getCustomName(Entity entity) {
		if (entity.getType() == EntityType.DROPPED_ITEM) {
			Item item = (Item) entity;
			ItemMeta itemMeta = item.getItemStack().getItemMeta();
			if (itemMeta.hasDisplayName()) {
				return itemMeta.getDisplayName();
			}
		}

		return entity.getCustomName();
	}

	private final Consumer<BlueMapAPI> onDisableListener = api -> getServer().getScheduler().cancelTasks(this);

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}
