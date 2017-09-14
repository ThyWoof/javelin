package javelin.model.unit.abilities.discipline.serpent;

import javelin.controller.upgrade.ability.RaiseWisdom;
import javelin.controller.upgrade.skill.Heal;
import javelin.model.unit.abilities.discipline.Discipline;
import javelin.model.unit.abilities.discipline.Maneuver;
import javelin.model.world.location.town.labor.military.BuildDisciplineAcademy;

public class SteelSerpent extends Discipline {
	public static final Maneuver[] MANEUVERS = new Maneuver[] {
			new DizzyingVenomPrana(), new IronFang(),
			new SickeningVenomStrike(), new StingOfTheAdder(),
			new StingOfTheAsp(), new StingOfTheRattler(), new TearingFang(),
			new WeakeningVenomPrana() };
	public static final Discipline INSTANCE = new SteelSerpent();
	public static final BuildDisciplineAcademy LABOR = INSTANCE.buildacademy();

	public SteelSerpent() {
		super("Steel serpent", RaiseWisdom.SINGLETON, Heal.SINGLETON);
	}

	@Override
	protected Maneuver[] getmaneuvers() {
		return MANEUVERS;
	}
}
