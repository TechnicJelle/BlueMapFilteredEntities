package com.technicjelle.bluemapfilteredentities;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.technicjelle.bluemapfilteredentities.Constants.*;

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
	private Set<String> scoreboardTags;

	@Nullable
	@Comment("Path to the icon to use for entities that were matched by this filter")
	private String icon;

	@Nullable
	@Comment("The information that should be displayed when the entity's marker is clicked on the web map")
	private String popupInfoTemplate;

	@Nullable
	@Comment("Sub-filters to exclude entities from the filter")
	private List<Filter> exclude;

	@Nullable
	private transient EntityType entityType;

	@Nullable
	private transient UUID entityUUID;

	@Nullable
	private transient CreatureSpawnEvent.SpawnReason entitySpawnReason;

	public boolean checkValidAndInit(Logger logger, BlueMapAPI bmApi) {
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

		if (icon != null) {
			if (icon.isBlank()) {
				logger.log(Level.SEVERE, "Icon defined, but empty");
				valid = false;
			} else if (!Files.exists(bmApi.getWebApp().getWebRoot().resolve("assets/bmfe-icons").resolve(icon))) {
				logger.log(Level.SEVERE, "Icon file does not exist: " + icon);
				valid = false;
			}
		}

		if (popupInfoTemplate == null) {
			popupInfoTemplate = "Type: " + ENTITY_PROPERTY_TYPE + "\n" +
					"Name: " + ENTITY_PROPERTY_NAME + "\n" +
					"UUID: " + ENTITY_PROPERTY_UUID + "\n" +
					"Spawn Reason: " + ENTITY_PROPERTY_SPAWN_REASON + "\n" +
					"Custom Name: " + ENTITY_PROPERTY_CUSTOM_NAME + "\n" +
					"Location: " + ENTITY_PROPERTY_X + ", " + ENTITY_PROPERTY_Y + ", " + ENTITY_PROPERTY_Z + " (" + ENTITY_PROPERTY_WORLD + ")\n" +
					"Scoreboard Tags: " + ENTITY_PROPERTY_SCOREBOARD_TAGS;
		} else {
			if (popupInfoTemplate.isBlank()) {
				logger.log(Level.SEVERE, "Popup info template defined, but empty");
				valid = false;
			}
			popupInfoTemplate = popupInfoTemplate.strip();
		}

		if (exclude != null) {
			for (Filter filter : exclude) {
				if (!filter.checkValidAndInit(logger, bmApi)) {
					valid = false;
				}
			}
		}

		//check if there's ONLY an "exclude" in this filter
		if (exclude != null &&
				type == null &&
				name == null &&
				customName == null &&
				uuid == null &&
				spawnReason == null &&
				minX == null &&
				maxX == null &&
				minZ == null &&
				maxZ == null &&
				minY == null &&
				maxY == null &&
				scoreboardTags == null) {
			logger.log(Level.SEVERE, "Filter has only an exclude filter, which is not allowed");
			valid = false;
		}

		return valid;
	}

	public @Nullable String getIcon() {
		return icon;
	}

	public @Nullable String getPopupInfoWithTemplate() {
		return popupInfoTemplate;
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

		if (scoreboardTags != null && !e.getScoreboardTags().containsAll(scoreboardTags)) return false;

		//TODO: Implement exclusion filters

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
		if (scoreboardTags != null) sb.append(" scoreboard-tags: ").append(listToString(scoreboardTags)).append("\n");
		if (icon != null) sb.append(" icon: ").append(icon).append("\n");
		if (popupInfoTemplate != null) sb.append(" popup-info-template: ").append(popupInfoTemplate).append("\n");
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
