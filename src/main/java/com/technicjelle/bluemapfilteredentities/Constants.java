package com.technicjelle.bluemapfilteredentities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class Constants {
	public static final String CONF_EXT = ".conf";
	public static final String NODE_FILTER_SETS = "filter-sets";

	public static final String ENTITY_PROPERTY_TYPE = "{type}";
	public static final String ENTITY_PROPERTY_NAME = "{name}";
	public static final String ENTITY_PROPERTY_UUID = "{uuid}";
	public static final String ENTITY_PROPERTY_SPAWN_REASON = "{spawn-reason}";
	public static final String ENTITY_PROPERTY_CUSTOM_NAME = "{custom-name}";
	public static final String ENTITY_PROPERTY_X = "{x}";
	public static final String ENTITY_PROPERTY_Y = "{y}";
	public static final String ENTITY_PROPERTY_Z = "{z}";
	public static final String ENTITY_PROPERTY_WORLD = "{world}";
	public static final String ENTITY_PROPERTY_SCOREBOARD_TAGS = "{scoreboard-tags}";

	@NotNull
	public static String getNullableString(String toNotNull) {
		return toNotNull == null ? "null" : toNotNull;
	}

	@NotNull
	public static String collectionToString(Collection<String> list) {
		return "[ " + String.join(", ", list) + " ]";
	}

	@Nullable
	public static Class<?> tryGetClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
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
}
