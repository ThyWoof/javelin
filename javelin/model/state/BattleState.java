package javelin.model.state;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.Point;
import javelin.controller.ai.ActionProvider;
import javelin.controller.ai.ChanceNode;
import javelin.controller.ai.Node;
import javelin.controller.exception.battle.EndBattle;
import javelin.controller.fight.Fight;
import javelin.controller.walker.ClearPath;
import javelin.controller.walker.ObstructedPath;
import javelin.controller.walker.Step;
import javelin.controller.walker.Walker;
import javelin.controller.walker.pathing.DirectPath;
import javelin.model.TeamContainer;
import javelin.model.unit.Combatant;

/**
 * Javelin's implementation of {@link Node}.
 *
 * {@link #clone()} is used for cloning the state but it does not clone the
 * {@link Combatant} instances! You need to use {@link #clone(Combatant))}
 * afterwards to do so.
 *
 * @see #cloneifdifferent(Combatant, Combatant)
 *
 * @author alex
 */
public class BattleState implements Node, TeamContainer {
	/**
	 * @see BattleState#haslineofsight(Point, Point, int, String)
	 * @author alex
	 */
	public enum Vision {
		/** Full vision. */
		CLEAR,
		/** Partial vision. */
		COVERED,
		/** No vision */
		BLOCKED,
	}

	/** Player units. */
	public ArrayList<Combatant> blueTeam;
	/** Computer units. */
	public ArrayList<Combatant> redTeam;
	/** Units that ran away from the fight. */
	public ArrayList<Combatant> fleeing;
	/** Dead and unconscious units. */
	public ArrayList<Combatant> dead;
	/** @see Meld */
	public ArrayList<Meld> meld;
	/**
	 * Since it's immutable no need to clone it.
	 */
	transient public Square[][] map;
	/**
	 * Next unit to act.
	 *
	 * @see #next()
	 */
	public Combatant next;
	/**
	 * Period of the day, affecting visibility.
	 *
	 * @see Fight#period
	 * @see Javelin#getDayPeriod()
	 */
	public String period;

	/**
	 * Constructor.
	 *
	 * @see #clone()
	 */
	public BattleState(final ArrayList<Combatant> blueTeam,
			final ArrayList<Combatant> redTeam, ArrayList<Combatant> dead,
			ArrayList<Combatant> fleeing, final Square[][] map, String period,
			ArrayList<Meld> meld) {
		this.map = map;
		this.period = period;
		this.dead = (ArrayList<Combatant>) dead.clone();
		this.blueTeam = (ArrayList<Combatant>) blueTeam.clone();
		this.redTeam = (ArrayList<Combatant>) redTeam.clone();
		this.meld = (ArrayList<Meld>) meld.clone();
		next();
	}

	/**
	 * @param f
	 *            Creates the initial state given a controller.
	 */
	public BattleState(Fight f) {
		map = f.map == null ? null : f.map.map;
		period = f.period;
		fleeing = new ArrayList<>();
		dead = new ArrayList<>();
		blueTeam = new ArrayList<>();
		redTeam = new ArrayList<>();
		meld = new ArrayList<>();
		next();

	}

	@Override
	public BattleState clone() {
		try {
			final BattleState clone = (BattleState) super.clone();
			clone.fleeing = (ArrayList<Combatant>) clone.fleeing.clone();
			clone.dead = (ArrayList<Combatant>) clone.dead.clone();
			clone.blueTeam = (ArrayList<Combatant>) blueTeam.clone();
			clone.redTeam = (ArrayList<Combatant>) redTeam.clone();
			clone.meld = (ArrayList<Meld>) meld.clone();
			next();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException();
		}
	}

	/** Updates {@link #next}. */
	public void next() {
		final List<Combatant> combatants = getcombatants();
		if (combatants.isEmpty()) {
			next = null;
			return;
		}
		next = combatants.get(0);
		final int ncombatants = combatants.size();
		for (int i = 1; i < ncombatants; i++) {
			final Combatant c = combatants.get(i);
			if (c.ap < next.ap) {
				next = c;
			}
		}
		if (next.source.passive) {
			next.act(this);
			next();
		}
	}

	@Override
	public Iterable<List<ChanceNode>> getsucessors() {
		return new ActionProvider(this);
	}

	@Override
	public ArrayList<Combatant> getcombatants() {
		final ArrayList<Combatant> list = (ArrayList<Combatant>) blueTeam
				.clone();
		list.addAll(redTeam);
		return list;
	}

	/**
	 * @return All units surrounding the given {@link Combatant}.
	 */
	public ArrayList<Combatant> getsurroundings(final Combatant surrounded) {
		final ArrayList<Combatant> surroundings = new ArrayList<>();
		final int[] location = surrounded.location;
		final ArrayList<Combatant> combatants = getcombatants();
		location: for (final Combatant c : combatants) {
			for (int x = location[0] - 1; x <= location[0] + 1; x++) {
				for (int y = location[1] - 1; y <= location[1] + 1; y++) {
					if (x == location[0] && y == location[1]) {
						/* center */
						continue;
					}
					if (c.location[0] == x && c.location[1] == y) {
						surroundings.add(c);
						continue location;
					}
				}
			}
		}
		return surroundings;
	}

	@Override
	public List<Combatant> getblueteam() {
		return blueTeam;
	}

	@Override
	public List<Combatant> getredteam() {
		return redTeam;
	}

	public Combatant clone(Combatant c) {
		final ArrayList<Combatant> team = getteam(c);
		final int index = team.indexOf(c);
		if (index == -1) {
			return null;
		}
		c = team.get(index).clone();
		c.refresh();
		team.set(index, c);
		if (next.equals(c)) {
			next = c;
		}
		return c;
	}

	/**
	 * @return Unit at given coordinate or <code>null</code> if none.
	 */
	public Combatant getcombatant(final int x, final int y) {
		for (final Combatant c : getcombatants()) {
			if (c.location[0] == x && c.location[1] == y) {
				return c;
			}
		}
		return null;
	}

	/**
	 * @param c
	 *            Removes this unit from battle.
	 */
	public void remove(Combatant c) {
		// c = clone(c);
		if (!blueTeam.remove(c)) {
			redTeam.remove(c);
		}
		if (c.equals(next)) {
			next();
		}
	}

	/**
	 * To avoid having to implement attacks-of-opporutnity gonna simply prohibit
	 * that anything that would cause an aoo is simply prohibited. since the
	 * game is more fluid with movement/turns now this shouldn't be a problem.
	 *
	 * Disengaging is simply forcing a 5-foot step to avoid aoo as per the core
	 * rules.
	 */
	public boolean isengaged(final Combatant c) {
		if (c.burrowed) {
			return false;
		}
		for (final Combatant nearby : getsurroundings(c)) {
			if (!c.isally(nearby, this)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param range
	 *            How many 5-feet squares ahead the active combatant can see.
	 * @param periodperception
	 *            Represents the light perception for the active combatant since
	 *            some monsters may have the darkvision quality or similar. If
	 *            night or evening the character will not able to see past
	 *            obstacles in the map.
	 */
	public Vision haslineofsight(final Point me, final Point target, int range,
			String periodperception) {
		if (Walker.distance(me.x, me.y, target.x, target.y) <= 1) {
			return Vision.CLEAR;
		}
		final ArrayList<Step> clear = new ClearPath(me, target,
				new DirectPath(), this).walk();
		final ArrayList<Step> covered = periodperception == Javelin.PERIODEVENING
				|| periodperception == Javelin.PERIODNIGHT ? null
						: new ObstructedPath(me, target, new DirectPath(), this)
								.walk();
		if (clear == null && covered == null) {
			return Vision.BLOCKED;
		}
		final ArrayList<Step> steps;
		if (clear == null) {
			steps = covered;
		} else if (covered == null) {
			steps = clear;
		} else if (clear.size() <= covered.size()) {
			steps = clear;
		} else {
			steps = covered;
		}
		if (steps.size() >= range) {
			return Vision.BLOCKED;
		}
		return steps == clear ? Vision.CLEAR : Vision.COVERED;
	}

	/**
	 * @return As {@link #gettargets(Combatant, List)} but default to targetting
	 *         only units in the opposite team.
	 */
	public List<Combatant> gettargets(Combatant combatant) {
		return gettargets(combatant,
				getteam(combatant) == blueTeam ? redTeam : blueTeam);
	}

	/**
	 * @param team
	 *            From the units in this team...
	 * @return all that can be seen by the given {@link Combatant}.
	 */
	public List<Combatant> gettargets(Combatant combatant,
			List<Combatant> team) {
		final List<Combatant> targets = new ArrayList<>();
		for (final Combatant targetc : team) {
			if (haslineofsight(combatant, targetc) != Vision.BLOCKED) {
				targets.add(targetc);
			}
		}
		return targets;
	}

	/**
	 * @return <code>true</code> if the target {@link Combatant} can be seen.
	 */
	public Vision haslineofsight(Combatant me, Combatant target) {
		return haslineofsight(me,
				new Point(target.location[0], target.location[1]));
	}

	/**
	 * @return <code>true</code> if the target {@link Point} can be seen.
	 */
	public Vision haslineofsight(Combatant me, Point target) {
		return haslineofsight(new Point(me.location[0], me.location[1]),
				new Point(target.x, target.y), me.view(period),
				me.perceive(period));
	}

	/**
	 * @return The team this unit is in. Assumes it is in one of them.
	 * @see #blueTeam
	 * @see #redTeam
	 */
	public ArrayList<Combatant> getteam(Combatant c) {
		return blueTeam.contains(c) ? blueTeam : redTeam;
	}

	@Override
	public String toString() {
		String out = blueTeam + "\n" + redTeam + "\n\n";
		for (Square[] line : map) {
			for (Square s : line) {
				out += s;
			}
			out += "\n";
		}
		return out;
	}

	/**
	 * This is necessary because sometimes you don't want to clone a unit twice
	 * (which would result in 2 different clones). For example: if you have a
	 * Caster and a Target it just could be they are both the same unit
	 * (self-targetting) and in this case you don't want to clone them twice.
	 *
	 * @param c
	 *            If not equal, will clone and return this unit.
	 * @param same
	 *            If equal will return this unit.
	 */
	public Combatant cloneifdifferent(Combatant c, Combatant same) {
		return c.equals(same) ? same : clone(c);
	}

	@Override
	public BattleState clonedeeply() {
		BattleState cl = clone();
		cl.blueTeam.clear();
		cl.redTeam.clear();
		for (Combatant c : blueTeam) {
			cl.blueTeam.add(c.clone());
		}
		for (Combatant c : redTeam) {
			cl.redTeam.add(c.clone());
		}
		return cl;
	}

	/**
	 * @return Meld at the given coordinate or <code>null</code> if none.
	 */
	public Meld getmeld(int x, int y) {
		for (Meld m : meld) {
			if (m.x == x && m.y == y) {
				return m;
			}
		}
		return null;
	}

	public void flee(Combatant c) {
		fleeing.add(c);
		remove(c);
	}

	/**
	 * This method returns a subset of fleeing characters. Note that
	 * {@link #blueTeam} and {@link #redTeam} are modified for several
	 * {@link EndBattle} purposes so it might be preferable to use
	 * {@link Fight#originalblueteam} and {@link Fight#originalredteam} instead.
	 *
	 * @param team
	 *            A list containing the combatants that should not be excluded
	 *            from {@link #fleeing}.
	 * @return a new list (safe for modification) containing only the units
	 *         provided.
	 */
	public ArrayList<Combatant> getfleeing(List<Combatant> team) {
		ArrayList<Combatant> fleeing = new ArrayList<>();
		for (Combatant c : this.fleeing) {
			if (team.contains(c)) {
				fleeing.add(c);
			}
		}
		return fleeing;
	}

	/**
	 * Utility method to find a clone in this state intance. To be used with
	 * {@link #getteam(Combatant)}, {@link #getallcombatants()},
	 * {@link #getcombatants()}, {@link #fleeing}, {@link #dead}, etc.
	 *
	 * @return The clone of the given combatant or <code>null</code>.
	 */
	static public Combatant getcombatant(Combatant c,
			List<Combatant> combatants) {
		int i = combatants.indexOf(c);
		return i >= 0 ? combatants.get(i) : null;
	}

	/**
	 * New list with {@link #redTeam}, {@link #blueTeam}, {@link #fleeing} and
	 * {@link #dead}.
	 */
	public void getallcombatants() {
		ArrayList<Combatant> all = getcombatants();
		all.addAll(fleeing);
		all.addAll(dead);
	}

	public ArrayList<Combatant> getopponents(Combatant c) {
		return getteam(c) == blueTeam ? redTeam : blueTeam;
	}

	public void swapteam(Combatant c) {
		if (blueTeam.contains(c)) {
			blueTeam.remove(c);
			redTeam.add(c);
		} else {
			redTeam.remove(c);
			blueTeam.add(c);
		}
	}
}
