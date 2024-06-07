package com.technicjelle.bluemapfilteredentities;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;
import java.util.logging.Logger;

@ConfigSerializable
public class FilterSet {
	@Nullable
	@Comment("Label of the filter set, aka the name on the website")
	private String label;

	@Nullable
	@Comment("Whether the filter set can be toggled on the website")
	private Boolean toggleable;

	@Nullable
	@Comment("Whether the filter set is hidden by default on the website")
	private Boolean defaultHidden;

	@Nullable
	@Comment("List of filters to apply")
	private List<Filter> filters;

	public boolean checkValidAndInit(Logger logger, BlueMapAPI bmApi) {
		boolean valid = true;
		if (label == null) {
			logger.warning("Label is missing!");
			valid = false;
		}

		if (toggleable == null) {
			toggleable = true;
		}
		if (defaultHidden == null) {
			defaultHidden = true;
		}

		if (filters == null) {
			logger.warning("Filters property is missing!");
			valid = false;
		} else if (filters.isEmpty()) {
			logger.warning("Filters list is empty!");
			valid = false;
		} else {
			for (Filter filter : filters) {
				if (!filter.checkValidAndInit(logger, bmApi)) {
					valid = false;
				}
			}
		}

		return valid;
	}

	public @Nullable List<Filter> getFilters() {
		return filters;
	}

	public MarkerSet createMarkerset() {
		return MarkerSet.builder()
				.label(label)
				.toggleable(toggleable)
				.defaultHidden(defaultHidden)
				.build();
	}
}
