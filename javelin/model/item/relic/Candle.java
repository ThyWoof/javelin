package javelin.model.item.relic;

import javelin.controller.action.Target;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.model.BattleMap;
import javelin.model.Realm;
import javelin.model.state.BattleState;
import javelin.model.unit.Combatant;
import javelin.view.screen.BattleScreen;

/** Brings a single creature to 1hp. */
public class Candle extends Relic {
	/** Constructor. */
	public Candle() {
		super("Candle of Searing", Realm.FIRE);
		usedinbattle = true;
		usedoutofbattle = false;
	}

	@Override
	protected boolean activate(Combatant user) {
		new Target("") {
			@Override
			protected int calculatehitdc(Combatant target, Combatant active,
					BattleState state) {
				return 1;
			}

			@Override
			protected void attack(Combatant active, Combatant target,
					BattleState s, BattleMap map) {
				target.hp = 1;
				Game.message(
						"A roaring column of flame engulfs " + target + "!",
						null, Delay.BLOCK);
			}
		}.perform(user, BattleScreen.active.map, Game.hero());
		return true;
	}

}
