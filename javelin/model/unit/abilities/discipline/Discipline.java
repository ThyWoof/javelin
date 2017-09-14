package javelin.model.unit.abilities.discipline;

import java.io.Serializable;

import javelin.controller.action.maneuver.ExecuteManeuver;
import javelin.controller.generator.feature.FeatureGenerator;
import javelin.controller.upgrade.Upgrade;
import javelin.controller.upgrade.ability.RaiseAbility;
import javelin.controller.upgrade.classes.ClassLevelUpgrade;
import javelin.model.unit.abilities.discipline.serpent.SteelSerpent;
import javelin.model.unit.feat.MartialTraining;
import javelin.model.unit.feat.attack.expertise.CombatExpertise;
import javelin.model.world.location.town.labor.military.Academy;
import javelin.model.world.location.town.labor.military.BuildDisciplineAcademy;
import javelin.model.world.location.town.labor.military.DisciplineAcademy;

/**
 * Represent a martial-arts discipline from the Path of War books, which is
 * basically a collection of {@link Maneuver}s.
 * 
 * Each discipline should have at least 2 {@link Maneuver}s per level.
 * Technically, everything is fine as long as there at least 2 disciplines per
 * leve of {@link MartialTraining} the unit can upgrade to - so having 8 level 1
 * Maneuvers is enough to get a discipline to level 4 (even if not ideal).
 * 
 * @author alex
 * @see Disciplines#ALL
 */
public abstract class Discipline implements Serializable {
	public static final Discipline[] DISCIPLINES = new Discipline[] {
			SteelSerpent.INSTANCE };

	/**
	 * Whether {@link FeatureGenerator} should generate a DisciplineAcademy or
	 * not. Useful for emulating abilities that are not {@link Maneuvers} as
	 * disciplines for the sake of using the existing {@link ExecuteManeuver}
	 * integrated user interface.
	 * 
	 * Note that some disciplines (such as {@link CombatExpertise}) have an
	 * {@link Academy} - but they are not actual instances of DisciplineAcademy
	 * and are generateed in some other way. In these cases, {@link #hasacademy}
	 * would always be <code>false</code>.
	 * 
	 * @see FeatureGenerator
	 * @see #generateacademy()
	 */
	public boolean hasacademy = true;

	public RaiseAbility abilityupgrade;
	public ClassLevelUpgrade classupgrade;
	public Upgrade skillupgrade;
	public String name;

	public Discipline(String name, ClassLevelUpgrade classupgrade,
			RaiseAbility abilityupgrade, Upgrade skillupgrade) {
		this(name);
		this.abilityupgrade = abilityupgrade;
		this.classupgrade = classupgrade;
		this.skillupgrade = skillupgrade;
	}

	/** Use only for emulated {@link Discipline}s. */
	public Discipline(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		return name.equals(((Maneuver) obj).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/** See {@link #hasacademy}. */
	public DisciplineAcademy generateacademy() {
		return new DisciplineAcademy(this);
	}

	public BuildDisciplineAcademy buildacademy() {
		return new BuildDisciplineAcademy(this);
	}

	/**
	 * @return A static, non-serializable list of {@link Maneuver}s that are
	 *         part of this martial path. This is to reduce the number of bugs,
	 *         prevent duplication of these lists when each new instance of the
	 *         game is opened and to make debug easier without losing game
	 *         state.
	 */
	protected abstract Maneuver[] getmaneuvers();

	/**
	 * @param maxlevel
	 *            Any {@link Maneuver} higher than this level will not be
	 *            returned. If <code>null</code>, return all {@link Maneuver}s.
	 * @return A new list with the filtered maneuvers from this discipline.
	 */
	public Maneuvers getmaneuvers(Integer maxlevel) {
		Maneuvers maneuvers = new Maneuvers(maxlevel * 2);
		for (Maneuver m : getmaneuvers()) {
			if (maxlevel == null || m.level <= maxlevel) {
				maneuvers.add(m);
			}
		}
		maneuvers.sort();
		return maneuvers;
	}

	@Override
	public String toString() {
		return name;
	}
}
