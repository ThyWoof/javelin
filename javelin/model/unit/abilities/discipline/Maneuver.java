package javelin.model.unit.abilities.discipline;

import java.io.Serializable;
import java.util.List;

import javelin.controller.action.Action;
import javelin.controller.action.ai.AiAction;
import javelin.controller.action.maneuver.ExecuteManeuver;
import javelin.controller.ai.ChanceNode;
import javelin.model.state.BattleState;
import javelin.model.unit.Monster;
import javelin.model.unit.attack.Combatant;

public abstract class Maneuver
		implements Serializable, javelin.model.Cloneable {
	public boolean spent = false;
	public String name;
	/**
	 * {@link Combatant#ap} cost for this action.
	 */
	public float ap;

	public Maneuver(String name) {
		this.name = name;
	}

	public void spend() {
		spent = true;
	}

	@Override
	public Maneuver clone() {
		try {
			return (Maneuver) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Delegate to {@link Action#perform(Combatant)}. The implementations need
	 * not worry about handling the {@link Combatant#ready(Maneuver)} or
	 * {@link Maneuver#spend()}.
	 * 
	 * @see ExecuteManeuver
	 */
	abstract public boolean perform(Combatant c);

	/**
	 * A delegate for {@link AiAction#getoutcomes(Combatant, BattleState)}. By
	 * the time this is called, all parameters have already been properly
	 * cloned. The implementations need not worry about handling the
	 * {@link Combatant#ready(Maneuver)} flow but are responsible for calling
	 * {@link Maneuver#spend()}.
	 * 
	 * @see ExecuteManeuver
	 */
	abstract public List<List<ChanceNode>> getoutcomes(Combatant c,
			BattleState s, Maneuver m);

	@Override
	public boolean equals(Object obj) {
		return name.equals(((Maneuver) obj).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	static public int getinitiationmodifier(Combatant c) {
		final Monster m = c.source;
		final int modifier = Math.max(m.intelligence,
				Math.max(m.wisdom, m.charisma));
		return m.hd.count() / 2 + Monster.getbonus(modifier);
	}

	public boolean validate(Combatant c) {
		return true;
	}

	@Override
	public String toString() {
		return name + (spent ? "*" : "");
	}

}
