package javelin.model.world.location.dungeon.feature.door;

import javelin.model.item.key.door.StoneKey;

public class StoneDoor extends Door {
	public StoneDoor() {
		super("dungeondoorstone", 28, 28, StoneKey.class);
	}
}