package com.technicjelle.bluemapfilteredentities;

import com.flowpowered.math.vector.Vector2i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class Vector2 {
	@Nullable
	@Comment("X coordinate")
	private Float x;

	@Nullable
	@Comment("Y coordinate")
	private Float y;

	public boolean checkInvalid() {
		return x == null || y == null;
	}

	public @NotNull Vector2i toVector2i() {
		if (x == null || y == null) return new Vector2i();
		return new Vector2i(x.intValue(), y.intValue());
	}

	@Override
	public String toString() {
		return "Vector2 { x: " + x + ", y: " + y + " }";
	}
}
