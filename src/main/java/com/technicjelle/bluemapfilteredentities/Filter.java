package com.technicjelle.bluemapfilteredentities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConfigSerializable
public class Filter {
	@Nullable
	@Comment("Type of entity to filter")
	private String type;

	@Nullable
	@Comment("Name of the entity to filter")
	private String name;

	@Nullable
	@Comment("Custom name of the entity to filter")
	private String customName;

	@Nullable
	@Comment("UUID of the entity to filter")
	private String uuid;

	@Nullable
	@Comment("Spawn reason of the entity to filter")
	private String spawnReason;

	@Nullable
	@Comment("Minimum X coordinate of the entity to filter")
	private Float minX;

	@Nullable
	@Comment("Maximum X coordinate of the entity to filter")
	private Float maxX;

	@Nullable
	@Comment("Minimum Z coordinate of the entity to filter")
	private Float minZ;

	@Nullable
	@Comment("Maximum Z coordinate of the entity to filter")
	private Float maxZ;

	@Nullable
	@Comment("Minimum Y coordinate of the entity to filter")
	private Float minY;

	@Nullable
	@Comment("Maximum Y coordinate of the entity to filter")
	private Float maxY;

	@Nullable
	@Comment("Scoreboard tags of the entity to filter")
	private String[] scoreboardTags;

	@Nullable
	@Comment("Sub-filters to exclude entities from the filter")
	private List<Filter> exclude;

	@Nullable
	private transient EntityType entityType;

	@Nullable
	private transient UUID entityUUID;

	@Nullable
	private transient CreatureSpawnEvent.SpawnReason entitySpawnReason;

	public boolean checkValidAndInit(Logger logger) {
		boolean valid = true;

		if (type != null) {
			try {
				entityType = EntityType.valueOf(type.strip().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				logger.log(Level.SEVERE, "Invalid entity type: " + type);
				valid = false;
			}
		}

		if (name != null) {
			if (name.isBlank()) {
				logger.log(Level.SEVERE, "Name defined, but empty");
				valid = false;
			}
		}

		if (customName != null) {
			if (customName.isBlank()) {
				logger.log(Level.SEVERE, "Custom name defined, but empty");
				valid = false;
			}
		}

		if (uuid != null) {
			try {
				entityUUID = UUID.fromString(uuid);
			} catch (IllegalArgumentException e) {
				logger.log(Level.SEVERE, "Invalid UUID: " + uuid);
				valid = false;
			}
		}

		if (spawnReason != null) {
			try {
				entitySpawnReason = CreatureSpawnEvent.SpawnReason.valueOf(spawnReason.strip().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				logger.log(Level.SEVERE, "Invalid spawn reason: " + spawnReason);
				valid = false;
			}
		}

		if (minX != null && maxX != null && minX > maxX) {
			logger.log(Level.SEVERE, "min-x is greater than max-x");
			valid = false;
		}
		if (minZ != null && maxZ != null && minZ > maxZ) {
			logger.log(Level.SEVERE, "min-z is greater than max-z");
			valid = false;
		}
		if (minY != null && maxY != null && minY > maxY) {
			logger.log(Level.SEVERE, "min-y is greater than max-y");
			valid = false;
		}

		if (exclude != null) {
			for (Filter filter : exclude) {
				if (!filter.checkValidAndInit(logger)) {
					valid = false;
				}
			}
		}

		return valid;
	}

	public boolean matches(Entity e, Logger logger) {
		if (entityType != null && e.getType() != entityType) return false;
		if (name != null && !e.getName().equals(name)) return false;
		if (customName != null && !Objects.equals(e.getCustomName(), customName)) return false;
		if (entityUUID != null && !e.getUniqueId().equals(entityUUID)) return false;
		if (entitySpawnReason != null && e.getEntitySpawnReason().equals(entitySpawnReason)) return false;

		if (minX != null && e.getLocation().getX() < minX) return false;
		if (maxX != null && e.getLocation().getX() > maxX) return false;
		if (minZ != null && e.getLocation().getZ() < minZ) return false;
		if (maxZ != null && e.getLocation().getZ() > maxZ) return false;
		if (minY != null && e.getLocation().getY() < minY) return false;
		if (maxY != null && e.getLocation().getY() > maxY) return false;

		//TODO: Implement scoreboard tags

		logger.info("Entity matched: " + e.getType());
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\n");
		if (entityType != null) sb.append(" type: ").append(entityType).append("\n");
		if (name != null) sb.append(" name: ").append(name).append("\n");
		if (customName != null) sb.append(" custom-name: ").append(customName).append("\n");
		if (entityUUID != null) sb.append(" UUID: ").append(entityUUID).append("\n");
		if (entitySpawnReason != null) sb.append(" spawn-reason: ").append(entitySpawnReason).append("\n");
		if (minX != null) sb.append(" min-x: ").append(minX).append("\n");
		if (maxX != null) sb.append(" max-x: ").append(maxX).append("\n");
		if (minZ != null) sb.append(" min-z: ").append(minZ).append("\n");
		if (maxZ != null) sb.append(" max-z: ").append(maxZ).append("\n");
		if (minY != null) sb.append(" min-y: ").append(minY).append("\n");
		if (maxY != null) sb.append(" max-y: ").append(maxY).append("\n");
		if (scoreboardTags != null)
			sb.append(" scoreboard-tags: [ ").append(String.join(", ", scoreboardTags)).append(" ]\n");
		if (exclude != null) {
			sb.append(" exclude:");
			for (Filter filter : exclude) {
				sb.append(filter.toString().replace("\n", "\n  "));
				sb.append("\n   ---");
			}
		}
		return sb.toString().stripTrailing();
	}
}
