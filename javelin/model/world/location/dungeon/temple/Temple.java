package javelin.model.world.location.dungeon.temple;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javelin.Javelin;
import javelin.controller.Point;
import javelin.controller.fight.Fight;
import javelin.controller.fight.TempleEncounter;
import javelin.controller.terrain.Terrain;
import javelin.model.Realm;
import javelin.model.item.Key;
import javelin.model.item.relic.Relic;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;
import javelin.model.unit.Skills;
import javelin.model.unit.Squad;
import javelin.model.world.location.dungeon.Chest;
import javelin.model.world.location.dungeon.Dungeon;
import javelin.model.world.location.dungeon.Feature;
import javelin.model.world.location.dungeon.temple.features.Altar;
import javelin.model.world.location.unique.Haxor;
import javelin.model.world.location.unique.UniqueLocation;
import javelin.view.Images;
import javelin.view.screen.haxor.Win;

/**
 * Temples are the key to winning Javelin. Each temple is locked and needs to be
 * unlocked by a {@link Key}, brute {@link Monster#strength} or
 * {@link Skills#disabledevice}. Inside the Temple there will be a Relic, and
 * once all of those are collected they can be taken to {@link Haxor} to finish
 * the game. Each temple is a multi-level, permanent {@link Dungeon}, where on
 * each level can be found a ruby and the Relic sits on the last level.
 * 
 * Battles inside temples consist of upgraded units.
 * 
 * @see Win
 * @see Haxor#rubies
 * @see TempleEncounter
 * @author alex
 */
public abstract class Temple extends UniqueLocation {
	final static int DEPTH = 3;

	/**
	 * Create the temples during world generation.
	 */
	public static void generatetemples() {
		LinkedList<Integer> els = new LinkedList<Integer>();
		for (int el : new int[] { 3, 5, 8, 10, 13, 15, 18 }) {
			els.add(el);
		}
		Collections.shuffle(els);
		new AirTemple(els.pop()).place();
		new EarthTemple(els.pop()).place();
		new FireTemple(els.pop()).place();
		new WaterTemple(els.pop()).place();
		new EvilTemple(els.pop()).place();
		new GoodTemple(els.pop()).place();
		new MagicTemple(els.pop()).place();
		assert els.isEmpty();
	}

	/**
	 * Reward found on the deepest of the {@link #floors}.
	 * 
	 * @see Altar
	 * @see TempleDungeon#deepest
	 */
	public Relic relic;
	/**
	 * A temple needs to be opened by a {@link Key} or other method before being
	 * explored.
	 * 
	 * @see #open()
	 */
	public boolean open = false;
	/**
	 * Level from 1 to 20 (typical d20 character level for a group of 4).
	 * 
	 * @see #el
	 * @see #leveltoel(int)
	 */
	public int level;
	/**
	 * Each floor has a {@link Chest} with a ruby in it and there is also an
	 * {@link Altar} on the deepest level.
	 */
	public List<TempleDungeon> floors = new ArrayList<TempleDungeon>();
	/** Encounter level equivalent for {@link #level}. */
	public int el;
	String fluff;
	/** If not <code>null</code> will override {@link Dungeon#floor}. */
	public String floor = null;
	/** If not <code>null</code> will override {@link Dungeon#wall}. */
	public String wall = null;
	/**
	 * TODO there's gotta be a better way to do this
	 */
	static public boolean climbing = false;
	/** TODO same as {@link #climbing} */
	public static boolean leavingfight = false;

	/**
	 * @param r
	 *            Temple's defining characteristic.
	 * @param fluffp
	 *            Text description of temple and surrounding area.
	 */
	public Temple(Realm r, int level, Relic relicp, String fluffp) {
		super("Temple of " + r.getname(), "Temple of " + r.getname(), level,
				level);
		realm = r;
		this.level = level;
		this.el = leveltoel(level);
		relic = relicp;
		fluff = fluffp;
		for (int i = 0; i < DEPTH; i++) {
			floors.add(new TempleDungeon(this, i == DEPTH - 1));
		}
	}

	@Override
	protected void generategarrison(int minlevel, int maxlevel) {
		// no outside garrison
	}

	@Override
	public Image getimage() {
		return Images
				.getImage("locationtemple" + realm.getname().toLowerCase());
	}

	@Override
	public boolean interact() {
		if (!open) {
			if (!javelin.controller.db.Preferences.DEBUGUNLOCKTEMPLES
					&& !open()) {
				return true;
			}
			open = true;
			Javelin.message(fluff, true);
		}
		return true;
	}

	boolean open() {
		@SuppressWarnings("deprecation")
		Key key = new Key(realm);
		if (Squad.active.equipment.pop(key) != null) {
			Javelin.message("Temple entrance opened by the "
					+ key.toString().toLowerCase() + "!", true);
			return true;
		}
		Combatant unlock = unlock();
		if (unlock != null) {
			Javelin.message("Temple entrance unlocked by " + unlock + "!",
					true);
			return true;
		}
		Combatant force = force();
		if (force != null) {
			Javelin.message("Temple entrance forced by " + force + "!", true);
			return true;
		}
		Javelin.message("The " + descriptionknown + " is locked.", true);
		return false;
	}

	Combatant force() {
		Combatant best = null;
		for (Combatant c : Squad.active.members) {
			int roll = Monster.getbonus(c.source.strength);
			if (roll < level) {
				continue;
			}
			if (best == null || roll > Monster.getbonus(best.source.strength)) {
				best = c;
			}
		}
		return best;
	}

	Combatant unlock() {
		Combatant best = null;
		for (Combatant c : Squad.active.members) {
			int roll = c.source.disarm();
			if (roll - 10 < level) {
				continue;
			}
			if (best == null || roll > best.source.disarm()) {
				best = c;
			}
		}
		return best;
	}

	@Override
	public Realm getrealmoverlay() {
		return null;
	}

	/**
	 * @return Starts a {@link TempleEncounter}.
	 */
	public Fight encounter() {
		return new TempleEncounter(this);
	}

	/** See {@link Fight#validate(ArrayList)}. */
	public boolean validate(ArrayList<Combatant> foes) {
		return true;
	}

	/** See {@link Fight#getterrains(Terrain)}; */
	public ArrayList<Terrain> getterrains(Terrain t) {
		ArrayList<Terrain> terrains = new ArrayList<Terrain>();
		terrains.add(Terrain.UNDERGROUND);
		terrains.add(terrain);
		return terrains;
	}

	@Override
	protected void generate() {
		if (terrain == null || terrain.equals(Terrain.WATER)) {
			super.generate();
		} else {
			while (x == -1 || !Terrain.get(x, y).equals(terrain)) {
				super.generate();
			}
		}
	}

	/**
	 * The given parameters are meant to be used with
	 * {@link Dungeon#findspot(java.util.Collection, Set)}.
	 * 
	 * @param used
	 *            Don't forget to update this if creating multiple features.
	 * @param templeDungeon
	 * @return a new {@link Feature} to be placed.
	 */
	public List<Feature> getfeatures(Set<Point> free, Set<Point> used,
			TempleDungeon dungeon) {
		return new ArrayList<Feature>();
	}

	/**
	 * See {@link Dungeon#hazard()}.
	 * 
	 * @param templeDungeon
	 */
	public void hazard(TempleDungeon templeDungeon) {
		// some dungoen have Features instead of Hazards
	}

	@Override
	public List<Combatant> getcombatants() {
		return null;
	}
}
