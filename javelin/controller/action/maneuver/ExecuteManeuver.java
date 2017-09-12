package javelin.controller.action.maneuver;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.action.Action;
import javelin.controller.action.ai.AiAction;
import javelin.controller.ai.ChanceNode;
import javelin.controller.exception.RepeatTurn;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.model.state.BattleState;
import javelin.model.unit.abilities.discipline.Disciplines;
import javelin.model.unit.abilities.discipline.Maneuver;
import javelin.model.unit.abilities.discipline.Maneuvers;
import javelin.model.unit.attack.Combatant;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.town.SelectScreen;

public class ExecuteManeuver extends Action implements AiAction {
	public static final ExecuteManeuver INSTANCE = new ExecuteManeuver();

	private ExecuteManeuver() {
		super("Execute maneuvers", "m");
	}

	@Override
	public List<List<ChanceNode>> getoutcomes(Combatant c, BattleState s) {
		final ArrayList<List<ChanceNode>> outcomes = new ArrayList<List<ChanceNode>>();
		final Disciplines disciplines = c.disciplines;
		disciplines.sort();
		for (String discipline : disciplines.keySet()) {
			for (Maneuver m : disciplines.get(discipline)) {
				BattleState s2 = s.clone();
				Combatant c2 = s2.clone(c);
				Maneuver m2 = c2.disciplines.find(m);
				if (m.spent) {
					ArrayList<ChanceNode> chance = new ArrayList<ChanceNode>();
					c2.ready(m2);
					final String feedback = c2 + " readies "
							+ m.name.toLowerCase() + "!";
					final ChanceNode node = new ChanceNode(s2, 1, feedback,
							Delay.BLOCK);
					chance.add(node);
					outcomes.add(chance);
				} else {
					outcomes.addAll(m.getoutcomes(c2, s2, m2));
				}
			}
		}
		return outcomes;
	}

	@Override
	public boolean perform(Combatant c) {
		final Disciplines disciplines = c.disciplines;
		if (disciplines.isEmpty()) {
			Game.message("No known manuevers...", Delay.WAIT);
			BattleScreen.active.block();
			throw new RepeatTurn();
		}
		Maneuvers maneuvers = new Maneuvers();
		String prompt = "Choose a manuever to execute (or ready), or press any other key to exit...";
		for (String discipline : disciplines.keySet()) {
			prompt += "\n\n" + discipline + ":\n";
			for (Maneuver m : disciplines.get(discipline)) {
				final String spent = m.spent ? " (not ready)" : "";
				prompt += "  " + SelectScreen.KEYS[maneuvers.size()] + " - "
						+ m.name + spent + "\n";
				maneuvers.add(m);
			}
		}
		Maneuver m = choose(maneuvers, prompt);
		if (m.spent) {
			String name = m.name.toLowerCase();
			Character confirm = Javelin.prompt("Do you want to ready " + name
					+ "?\nPress ENTER or m to confirm...");
			if (confirm != 'm' && confirm != '\n') {
				throw new RepeatTurn();
			}
			c.ready(m);
			Game.messagepanel.clear();
			Game.message(c + " readies " + name + "...", Delay.WAIT);
			return true;
		}
		try {
			m.spend();
			return m.perform(c);
		} catch (RepeatTurn e) {
			m.spent = false;
			throw e;
		}
	}

	Maneuver choose(Maneuvers maneuvers, String prompt) {
		if (maneuvers.size() == 1) {
			return maneuvers.get(0);
		}
		int choice = SelectScreen
				.convertkeytoindex(Javelin.promptscreen(prompt));
		Javelin.app.switchScreen(BattleScreen.active);
		if (choice == -1 && choice < maneuvers.size()) {
			throw new RepeatTurn();
		}
		return maneuvers.get(choice);
	}
}