package javelin.controller.fight.minigame;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.Weather;
import javelin.controller.exception.battle.EndBattle;
import javelin.controller.fight.Fight;
import javelin.controller.map.Map;
import javelin.model.item.Item;
import javelin.model.unit.Combatant;
import javelin.model.unit.Squad;
import javelin.model.world.Period;
import javelin.old.RPG;
import javelin.old.messagepanel.MessagePanel;
import javelin.view.screen.BattleScreen;

/**
 * A minigame is a battle totally independent from the normal game {@link Squad}
 * . It usually has differing objectives, mechanics and overall dynamics.
 *
 * @author alex
 */
public abstract class Minigame extends Fight{
	/** Constructor. */
	public Minigame(){
		bribe=false;
		hide=false;
		map=Map.random();
		period=RPG.pick(Period.ALL);
		weather=Weather.DISTRIBUTION[RPG.r(Weather.DISTRIBUTION.length)];
	}

	@Override
	public Integer getel(int teamel){
		return null;
	}

	@Override
	public boolean onend(){
		System.exit(0);
		return false;
	}

	@Override
	public void withdraw(Combatant combatant,BattleScreen screen){
		if(Javelin.prompt("Do you want to abandon this match?\n\n"
				+"Press ENTER to confirm, any other key to cancel...")=='\n')
			throw new EndBattle();
		MessagePanel.active.clear();
	}

	@Override
	public List<Item> getbag(Combatant combatant){
		return new ArrayList<>();
	}

	/**
	 * Launches this minigame, offering a chance for it to do some setup actions.
	 *
	 * @return <code>false</code> if failed to start and should quit the
	 *         application.
	 */
	public boolean start(){
		return true;
	}
}
