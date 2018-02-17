package javelin.controller.generator.dungeon.template.corridor;

import javelin.controller.Point;
import javelin.controller.generator.dungeon.VirtualMap;
import javelin.controller.generator.dungeon.template.Template;
import tyrant.mikera.engine.RPG;

public class LinearCorridor extends Template {
	public LinearCorridor() {
		corridor = true;
	}

	@Override
	public void generate() {
		init(RPG.chancein(4) ? 2 : 1, RPG.r(3, 7));
	}

	public static void clear(Template t, Point cursor, Point door,
			Template next, Point doorb, VirtualMap map) {
		if (t instanceof LinearCorridor && next instanceof LinearCorridor) {
			map.set(FLOOR, cursor.x + door.x, cursor.y + door.y);
			next.tiles[doorb.x][doorb.y] = FLOOR;
		}
	}
}