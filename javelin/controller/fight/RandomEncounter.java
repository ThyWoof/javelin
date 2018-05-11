package javelin.controller.fight;

import java.util.ArrayList;

import javelin.Debug;
import javelin.JavelinApp;
import javelin.controller.exception.battle.StartBattle;
import javelin.controller.terrain.Terrain;
import javelin.model.unit.Combatant;
import tyrant.mikera.engine.RPG;

/**
 * Fight that happens on the overworld map.
 * 
 * @author alex
 */
public class RandomEncounter extends Fight {
	@Override
	public Integer getel(int teamel) {
		return Terrain.current().getel(teamel);
	}

	@Override
	public ArrayList<Combatant> getmonsters(Integer teamel) {
		return null;
	}

	/**
	 * @param chance
	 *            % chance of starting a battle.
	 * @throws StartBattle
	 */
	static public void encounter(double chance) {
		if (RPG.random() < chance && !Debug.disablecombat) {
			Fight f = JavelinApp.context.encounter();
			if (f != null) {
				throw new StartBattle(f);
			}
		}
	}
}