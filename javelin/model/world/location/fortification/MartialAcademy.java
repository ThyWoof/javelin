package javelin.model.world.location.fortification;

import java.util.ArrayList;
import java.util.HashSet;

import javelin.controller.upgrade.Upgrade;
import javelin.controller.upgrade.UpgradeHandler;
import javelin.controller.upgrade.ability.RaiseAbility;
import javelin.controller.upgrade.ability.RaiseDexterity;
import javelin.controller.upgrade.ability.RaiseIntelligence;
import javelin.controller.upgrade.ability.RaiseStrength;
import javelin.controller.upgrade.classes.Warrior;
import javelin.model.world.location.town.Academy;
import javelin.model.world.location.town.Town;
import tyrant.mikera.engine.RPG;

/**
 * Allows a player to learn one upgrade set.
 *
 * @author alex
 */
public class MartialAcademy extends Academy {
	public static final ArrayList<Guild> GUILDS = new ArrayList<MartialAcademy.Guild>();

	public static class BuildMartialAcademy extends BuildAcademy {
		public BuildMartialAcademy() {
			super(null, Town.HAMLET);
		}

		@Override
		protected void define() {
			goal = RPG.pick(GUILDS).generate();
			super.define();
		}
	}

	public static class Guild {
		String name;
		HashSet<Upgrade> upgrades;
		RaiseAbility ability;

		public Guild(HashSet<Upgrade> upgrades, String name,
				RaiseAbility ability) {
			super();
			this.name = name;
			this.upgrades = upgrades;
			this.ability = ability;
		}

		public MartialAcademy generate() {
			return new MartialAcademy(upgrades, name, ability);
		}
	}

	static {
		UpgradeHandler uh = UpgradeHandler.singleton;
		GUILDS.add(new Guild(uh.shots, "Academy (shooting range)",
				RaiseDexterity.SINGLETON));
		GUILDS.add(new Guild(uh.expertise, "Academy (combat expertise)",
				RaiseIntelligence.SINGLETON));
		GUILDS.add(new Guild(uh.power, "Academy (power attack)",
				RaiseStrength.SINGLETON));
	}

	/**
	 * See {@link Academy#Academy(String, String, int, int, HashSet)}.
	 *
	 * @param raise
	 */
	public MartialAcademy(HashSet<Upgrade> upgrades, String descriptionknownp,
			RaiseAbility raise) {
		super(descriptionknownp, "An academy", 6, 10, upgrades, raise,
				Warrior.SINGLETON);
	}
}
