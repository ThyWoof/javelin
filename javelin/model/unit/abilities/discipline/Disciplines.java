package javelin.model.unit.abilities.discipline;

import java.util.ArrayList;
import java.util.HashMap;

import javelin.model.unit.Monster;
import javelin.model.unit.abilities.discipline.expertise.CombatExpertiseDiscipline;
import javelin.model.unit.abilities.discipline.serpent.SteelSerpent;
import javelin.model.unit.attack.Combatant;

public class Disciplines extends HashMap<String, Maneuvers> {
	public static final Discipline[] ALL = new Discipline[] {
			CombatExpertiseDiscipline.INSTANCE, SteelSerpent.INSTANCE };

	public Disciplines() {
		super(0);
	}

	public Disciplines(Disciplines d) {
		super(d);
	}

	public ArrayList<Maneuver> getmaneuvers() {
		ArrayList<Maneuver> maneuvers = new ArrayList<Maneuver>();
		for (Maneuvers discipline : values()) {
			maneuvers.addAll(discipline);
		}
		return maneuvers;
	}

	/**
	 * @param m
	 *            Given an item...
	 * @return an equal item in this instance. Mostly useful for finding the
	 *         same maneuver after its {@link Combatant} or {@link Monster} has
	 *         been cloned.
	 */
	public Maneuver find(Maneuver m) {
		for (Maneuvers discipline : values()) {
			int i = discipline.indexOf(m);
			if (i >= 0) {
				return discipline.get(i);
			}
		}
		return null;
	}

	@Override
	public Disciplines clone() {
		Disciplines disciplines = (Disciplines) super.clone();
		for (String discipline : disciplines.keySet()) {
			disciplines.put(discipline, get(discipline).clone());
		}
		return disciplines;
	}

	/**
	 * Use {@link Combatant#addmaneuver(Discipline, Maneuver)} if you need
	 * validation. This method simply registers the given {@link Maneuver}
	 * blindly.
	 */
	public void add(Discipline d, Maneuver m) {
		Maneuvers maneuvers = get(d.name);
		if (maneuvers == null) {
			maneuvers = new Maneuvers();
			put(d.name, maneuvers);
		}
		maneuvers.add(m);
	}
}
