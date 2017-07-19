package javelin.model.controller.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javelin.Javelin;
import javelin.controller.action.world.Guide;
import javelin.controller.action.world.WorldMove;
import javelin.controller.db.StateManager;
import javelin.controller.fight.RandomEncounter;
import javelin.controller.fight.minigame.Minigame;
import javelin.controller.generator.feature.FeatureGenerator;
import javelin.controller.generator.feature.GenerationData;
import javelin.controller.kit.Kit;
import javelin.controller.terrain.Desert;
import javelin.controller.terrain.Hill;
import javelin.controller.terrain.Plains;
import javelin.controller.terrain.Water;
import javelin.controller.terrain.hazard.Hazard;
import javelin.controller.upgrade.Upgrade;
import javelin.model.Realm;
import javelin.model.item.Key;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;
import javelin.model.world.Actor;
import javelin.model.world.Caravan;
import javelin.model.world.Incursion;
import javelin.model.world.World;
import javelin.model.world.location.Location;
import javelin.model.world.location.Portal;
import javelin.model.world.location.fortification.Fortification;
import javelin.model.world.location.fortification.Trove;
import javelin.model.world.location.town.Town;
import javelin.model.world.location.town.labor.Deck;
import javelin.model.world.location.unique.Haxor;
import javelin.model.world.location.unique.UniqueLocation;
import javelin.view.mappanel.world.WorldTile;
import javelin.view.screen.SquadScreen;
import javelin.view.screen.haxor.Win;
import tyrant.mikera.engine.RPG;

/**
 * Scenario mode is a much faster type of gameplay than the main
 * {@link Campaign} mode. It's supposed to be finished on anywhere from 2 hours
 * of play to an afternoon (but of course it can be saved an resumed too).
 * 
 * The world is a lot more static in this mode. Several features and
 * {@link Location}s are disabled - including {@link RandomEncounter}s in ohe
 * overworld map and {@link Hazard}s. The only "moving pieces" in the world map
 * are yourself and {@link Incursion}s.
 * 
 * The {@link FeatureGenerator} is disabled after the original world is created,
 * meaning that, wuthout random encounters and other infinite means of gaining
 * experience and loot, you are on a race against time to conquer all hostile
 * {@link Town}s - 1 to 3, with varying degress of power according to the
 * quantity in each game.
 * 
 * There is only one enemy {@link Realm} per game and the starting features are
 * roughly made to be 1/3 neutral and 2/3 hostile.
 */
public class Scenario implements Serializable {
	/** {@link World} size. */
	public int size = 30;
	/**
	 * Allow access to {@link Minigame}s or not.
	 * 
	 * TODO may allow access but should fix them for scenario mode where their
	 * {@link UniqueLocation}s aren't present.
	 */
	public boolean minigames = false;
	/**
	 * Starting {@link Town} population. See {@link #statictowns}.
	 */
	public int startingpopulation = 6;
	/**
	 * If <code>true</code>, {@link Town}s will be overidden after {@link World}
	 * generation according to {@link #getscenariochallenge()}.
	 */
	public boolean statictowns = true;
	/**
	 * If not <code>null</code>, this amount will be seeded during {@link World}
	 * generation. It will also be the cap as per {@link GenerationData#max}.
	 * 
	 * Number of starting dungeons in the {@link World} map. Since {@link Key}s
	 * are important to {@link Win}ning the game this should be a fair amount,
	 * otherwise the player will depend only on {@link Caravan}s if too many
	 * dungeons are destroyed or if unable to find the proper {@link Chest}
	 * inside the dungeons he does find. Not that dungeons also spawn during the
	 * course of a game but since this is highly randomized a late-game player
	 * who ran out of dungeons should not be required to depend on that alone.
	 * 
	 * @see Actor#destroy(Incursion)
	 * @see FeatureGenerator
	 */
	public Integer startingdungeons = null;
	public boolean respawnlocations = false;
	public boolean normalizemap = true;
	/** Wheter {@link Key}s should exist at all. */
	public boolean allowkeys = false;
	/**
	 * Wheter to link {@link Plains} and {@link Hill} {@link Town}s.through a
	 * {@link Portal}.
	 */
	public boolean linktowns = false;
	/** Minimum distance between {@link Desert} and {@link Water}. */
	public int desertradius = 1;
	/** Number of {@link Town}s in the {@link World}. */
	public int towns = RPG.r(1 + 1, 1 + 3);
	/**
	 * Will clear locations as indicated by {@link Fortification#clear}.
	 */
	public boolean clearlocations = true;
	/** Wheter a full {@link Deck} should be allowed. */
	public boolean allowlabor = false;
	/** Place {@link Haxor} or not. */
	public boolean haxor = false;
	/**
	 * If <code>false</code>, only allow Actors marked as
	 * {@link Actor#allowedinscenario}.
	 */
	public boolean allowallactors = false;
	/** Ask for {@link Monster} names on {@link SquadScreen}. */
	public boolean asksquadnames = false;
	/** Wheter to cover {@link WorldTile}s. */
	public boolean fogofwar = false;
	/**
	 * Wheter {@link RandomEncounter}s and {@link Hazard}s should be triggered
	 * during {@link WorldMove}s.
	 */
	public boolean worldexploration = false;
	/** Save prefix for {@link StateManager}. */
	public String saveprefix = "scenario";
	/** File name for the F1 help {@link Guide}. */
	public String helpfile = "Scenarios";
	public boolean record = false;
	public boolean dominationwin = true;
	/** Number of {@link Location}s to spawn. See {@link FeatureGenerator}. */
	public int startingfeatures = size * size / 7;
	/** {@link Trove}s will only offer gold and experience rewards. */
	public boolean simpletroves = true;

	/**
	 * @return Starting encounter level for each hostile town in
	 *         {@link #SCENARIO} mode. 20 for 1 hostile town, 15 for 2 or 10 for
	 *         3.
	 */
	public static int getscenariochallenge() {
		return new int[] { 20, 15, 10 }[Town.gettowns().size() - 2];
	}

	/**
	 * {@link Upgrade} or not the starting squad after it's been selected.
	 * 
	 * @see SquadScreen
	 */
	public void upgradesquad(ArrayList<Combatant> squad) {
		ArrayList<Combatant> members = new ArrayList<Combatant>(squad);
		while (!members.isEmpty()) {
			ArrayList<Kit> kits = new ArrayList<Kit>(Kit.KITS);
			Collections.shuffle(kits);
			for (Kit k : kits) {
				Combatant c = members.get(0);
				if (Kit.getpossiblekits(c.source).contains(k)) {
					c.source.customName = Character.toUpperCase(
							k.name.charAt(0)) + k.name.substring(1);
					while (c.source.challengerating < 6) {
						c.upgrade(k.upgrades);
					}
					members.remove(0);
					if (members.isEmpty()) {
						return;
					}
				}
			}
		}
	}

	/**
	 * @return <code>true</code> if the current selection is enough to start the
	 *         game.
	 * @see SquadScreen
	 */
	public boolean checkfullsquad(ArrayList<Combatant> squad) {
		return squad.size() >= 4;
	}

	public boolean win() {
		for (Town t : Town.gettowns()) {
			if (t.ishostile()) {
				return false;
			}
		}
		Javelin.show(
				"Congratulations, you have won this scenario!\nThanks for playing!");
		return true;
	}
}