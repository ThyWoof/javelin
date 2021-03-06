package javelin.controller.upgrade.ability;

import javelin.controller.challenge.factor.HdFactor;
import javelin.controller.challenge.factor.SkillsFactor;
import javelin.controller.upgrade.classes.ClassAdvancement;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;

/**
 * @see RaiseAbility
 */
public class RaiseIntelligence extends RaiseAbility {
	/** Singleton instance. */
	public static final RaiseAbility INSTANCE = new RaiseIntelligence();

	RaiseIntelligence() {
		super("intelligence");
		purchaseskills = true;
	}

	@Override
	int getabilityvalue(Monster m) {
		return m.intelligence;
	}

	@Override
	boolean setattribute(Combatant m, int l) {
		m.source.raiseintelligence(+2);
		return true;
	}

	@Override
	public boolean apply(Combatant c) {
		int before = total(c.source);
		if (c.source.intelligence == 0 || !super.apply(c)) {
			return false;
		}
		int after = total(c.source);
		if (after > before) {
			c.source.skillpool = after - before;
		}
		return true;
	}

	int total(Monster source) {
		int total = Math.round(Math.round(Math.ceil(source.originalhd)))
				* SkillsFactor.levelup(
						HdFactor.gettypedata(source).skillprogression, source);
		for (ClassAdvancement c : ClassAdvancement.CLASSES) {
			total += c.getlevel(source)
					* SkillsFactor.levelup(c.skillrate, source);
		}
		return total;
	}
}
