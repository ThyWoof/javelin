package javelin.controller.upgrade.classes;

import javelin.controller.upgrade.skill.Perception;
import javelin.controller.upgrade.skill.SkillUpgrade;
import javelin.model.unit.Monster;
import javelin.model.unit.attack.Combatant;

/**
 * @see ClassLevelUpgrade
 */
public class Commoner extends ClassLevelUpgrade {
	private static final Level[] TABLE = new Level[] { new Level(0, 0, 0),
			new Level(0, 0, 0), new Level(0, 0, 0), new Level(1, 1, 1),
			new Level(1, 1, 1), new Level(1, 1, 1), new Level(2, 2, 2),
			new Level(2, 2, 2), new Level(2, 2, 2), new Level(3, 3, 3),
			new Level(3, 3, 3), new Level(3, 3, 3), new Level(4, 4, 4),
			new Level(4, 4, 4), new Level(4, 4, 4), new Level(5, 5, 5),
			new Level(5, 5, 5), new Level(5, 5, 5), new Level(6, 6, 6),
			new Level(6, 6, 6), new Level(6, 6, 6), };
	private static final SkillUpgrade[] SKILLS = new SkillUpgrade[] {
			Perception.SINGLETON };
	public static final ClassLevelUpgrade SINGLETON = new Commoner();

	private Commoner() {
		super("Commoner", .48f, TABLE, 4, 2, SKILLS, .45f);
	}

	@Override
	protected void setlevel(int level, Monster m) {
		m.commoner = level;
	}

	@Override
	public int getlevel(Monster m) {
		return m.commoner;
	}

	@Override
	protected boolean prefer(Combatant c) {
		return true;
	}
}
