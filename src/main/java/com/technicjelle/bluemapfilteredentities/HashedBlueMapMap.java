package com.technicjelle.bluemapfilteredentities;

import de.bluecolored.bluemap.api.BlueMapMap;

public class HashedBlueMapMap {
	//TODO: Replace with BlueMap's own BlueMapMap again,
	// once https://github.com/BlueMap-Minecraft/BlueMap/commit/757979b7b4635fda4bd0ae9837b103e5bde33cb6 is released
	private final BlueMapMap map;

	public HashedBlueMapMap(BlueMapMap map) {
		this.map = map;
	}

	public BlueMapMap getMap() {
		return map;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		HashedBlueMapMap that = (HashedBlueMapMap) obj;
		return map.getId().equals(that.map.getId());
	}

	@Override
	public int hashCode() {
		return map.getId().hashCode();
	}

	@Override
	public String toString() {
		return map.getId();
	}
}
