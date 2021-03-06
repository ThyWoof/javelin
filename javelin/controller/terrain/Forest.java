package javelin.controller.terrain;

import java.util.HashSet;
import java.util.Set;

import javelin.controller.terrain.hazard.Break;
import javelin.controller.terrain.hazard.FallingTrees;
import javelin.controller.terrain.hazard.GettingLost;
import javelin.controller.terrain.hazard.Hazard;
import javelin.controller.terrain.map.Maps;
import javelin.controller.terrain.map.forest.Dense;
import javelin.controller.terrain.map.forest.Medium;
import javelin.controller.terrain.map.forest.Sparse;
import javelin.controller.terrain.map.tyrant.DarkForest;
import javelin.model.world.World;
import tyrant.mikera.engine.Point;

/**
 * Dense forest but not quite a jungle.
 * 
 * @author alex
 */
public class Forest extends Terrain {
	/** Constructor. */
	public Forest() {
		this.name = "forest";
		this.difficulty = 0;
		this.difficultycap = -3;
		this.speedtrackless = 1 / 2f;
		this.speedroad = 1f;
		this.speedhighway = 1f;
		this.visionbonus = -4;
		representation = 'F';
	}

	@Override
	public Maps getmaps() {
		Maps m = new Maps();
		m.add(new Sparse());
		m.add(new Medium());
		m.add(new Dense());
		m.add(new DarkForest());
		return m;
	}

	@Override
	public HashSet<Point> generate(World world) {
		return gettiles(world);
	}

	@Override
	public Set<Hazard> gethazards(boolean special) {
		Set<Hazard> hazards = super.gethazards(special);
		hazards.add(new GettingLost(16));
		if (special) {
			hazards.add(new FallingTrees());
			hazards.add(new Break());
		}
		return hazards;
	}
}