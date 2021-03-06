package javelin.model.world.location.unique;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.challenge.ChallengeRatingCalculator;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.exception.RepeatTurn;
import javelin.controller.fight.Siege;
import javelin.controller.fight.TrainingSession;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.controller.upgrade.UpgradeHandler;
import javelin.controller.upgrade.feat.FeatUpgrade;
import javelin.model.Realm;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;
import javelin.model.unit.Squad;
import javelin.model.world.location.fortification.Fortification;
import javelin.view.screen.SquadScreen;
import tyrant.mikera.engine.RPG;

/**
 * Designed as a manner of making the lower levels faster, this receives the
 * amount of treasure in gold as a fee and offers a fight which, if won, will
 * offer a free feat upgrade. There are 4 levels and each is a difficult fight
 * for parties of 4 of the same level. The encounter levels are: 2, 7, 10, 17.
 * 
 * Inspired by the tower in WUtai on Final Fantasy VII.
 * 
 * @author alex
 */
public class TrainingHall extends Fortification {
	static final boolean DEBUG = false;
	private static final String DESCRIPTION = "The Training Hall";
	/**
	 * Actual encounter level for each {@link TrainingSession}, by using the
	 * shifted-to-zero index. Supposed to be difficult (EL-1) encounter party of
	 * levels 1 to 4.
	 */
	public static int[] EL = new int[] { 4, 8, 10, 12 };
	/**
	 * From 1 upwards, the current training session difficulty, supposed to
	 * represent different floors.
	 * 
	 * TODO this is being initialized in 2 places
	 */
	public Integer currentlevel = 1;

	/** Constructor. */
	public TrainingHall() {
		super(DESCRIPTION, DESCRIPTION, 0, 0);
		UniqueLocation.init(this);
		discard = false;
		impermeable = false;
		sacrificeable = true;
	}

	@Override
	protected void generategarrison(int minel, int maxel) {
		generategarrison();
	}

	public void generategarrison() {
		if (currentlevel == null) {
			currentlevel = 1;
		}
		// garrison.clear();
		ArrayList<Monster> senseis = SquadScreen.getcandidates();
		for (Monster sensei : new ArrayList<Monster>(senseis)) {
			if (!sensei.think(0)) {
				senseis.remove(sensei);
			}
		}
		int nstudents = Squad.active.members.size();
		int nsenseis = RPG.r(Math.min(3, nstudents), RPG.max(nstudents, 5));
		while (isweak() && garrison.size() < nsenseis) {
			garrison.add(new Combatant(null, RPG.pick(senseis).clone(), true));
		}
		while (isweak()) {
			Combatant.upgradeweakest(garrison, Realm.random());
		}
	}

	boolean isweak() {
		return ChallengeRatingCalculator
				.calculateel(garrison) < EL[currentlevel - 1];
	}

	@Override
	protected Siege fight() {
		int price = RewardCalculator.receivegold(garrison);
		Game.messagepanel.clear();
		if (price > Squad.active.gold) {
			Game.message("Not enough money to pay the training fee of $" + price
					+ "!", null, Delay.NONE);
			Game.getInput();
			throw new RepeatTurn();
		}
		Game.message(
				"Do you want to pay a free of $" + price
						+ " for a lesson?\nPress s to study or any other key to leave",
				null, Delay.NONE);
		if (Game.getInput().getKeyChar() == 's') {
			Squad.active.gold -= price;
			return new TrainingSession(this);
		}
		throw new RepeatTurn();
	}

	@Override
	public boolean drawgarisson() {
		return false;
	}

	@Override
	public List<Combatant> getcombatants() {
		return garrison;
	}

	public void level() {
		currentlevel += 1;
		boolean done = currentlevel - 1 >= EL.length;
		String prefix;
		if (done) {
			prefix = "This has been the final lesson.\n\n";
		} else {
			prefix = "Congratulations, you've graduated this level!\n\n";
		}
		Combatant student = Squad.active.members
				.get(Javelin.choose(prefix + "Which student will learn a feat?",
						Squad.active.members, true, true));
		ArrayList<FeatUpgrade> feats = UpgradeHandler.singleton.getfeats();
		ArrayList<FeatUpgrade> options = new ArrayList<FeatUpgrade>();
		while (options.size() < 3 && !feats.isEmpty()) {
			FeatUpgrade f = RPG.pick(feats);
			feats.remove(f);
			if (!options.contains(f)
					&& f.upgrade(student.clone().clonesource())) {
				options.add(f);
			}
		}
		options.get(Javelin.choose("Learn which feat?", options, true, true))
				.upgrade(student);
		if (done) {
			remove();
		} else {
			generategarrison();
		}
	}
}
