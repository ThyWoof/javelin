package javelin.controller.action.world;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javelin.controller.action.SimpleAction;
import javelin.controller.fight.Fight;
import javelin.model.unit.Squad;
import javelin.model.unit.attack.Combatant;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.WorldScreen;

/**
 * Configures {@link Combatant#automatic} and {@link Squad#strategic}.
 * 
 * TODO shouldn't need this, the UI should help set this more easily
 * 
 * @author alex
 */
public class Automate extends WorldAction implements SimpleAction {
	class AutomateWindow extends javelin.view.frame.Frame {
		ArrayList<Checkbox> boxes = new ArrayList<Checkbox>();
		Checkbox strategic = null;

		public AutomateWindow() {
			super("Automate units");
		}

		@Override
		protected Container generate() {
			boxes.clear();
			Panel container = new Panel(new GridLayout(0, 1));
			for (Combatant c : getunits()) {
				Checkbox box = new Checkbox(c.toString(), c.automatic);
				container.add(box);
				boxes.add(box);
			}
			container.add(new Label());
			if (BattleScreen.active instanceof WorldScreen) {
				strategic = new Checkbox("Strategic combat",
						Squad.active.strategic);
				container.add(strategic);
			} else {
				strategic = null;
				container.add(new Label("Changes are reset after battle."));
			}
			Button confirm = new Button("Apply");
			container.add(confirm);
			confirm.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					enter();
				}
			});
			return container;
		}

		@Override
		protected void enter() {
			for (int i = 0; i < boxes.size(); i++) {
				getunits().get(i).automatic = boxes.get(i).getState();
			}
			if (strategic != null) {
				Squad.active.strategic = strategic.getState();
			}
			frame.dispose();
		};

		ArrayList<Combatant> getunits() {
			if (BattleScreen.active instanceof WorldScreen) {
				return Squad.active.members;
			}
			ArrayList<Combatant> team = new ArrayList<Combatant>(
					Fight.state.blueTeam);
			for (Combatant c : new ArrayList<Combatant>(team)) {
				if (c.source.passive) {
					team.remove(c);
				}
			}
			return team;
		}
	}

	/** Constructor. */
	public Automate() {
		super("Set automatic units", new int[] {}, new String[] { "a" });
	}

	@Override
	public int[] getcodes() {
		return keys;
	}

	@Override
	public String getname() {
		return name;
	}

	@Override
	public String[] getkeys() {
		return morekeys;
	}

	@Override
	public void perform(WorldScreen screen) {
		perform();
	}

	@Override
	public void perform() {
		AutomateWindow w = new AutomateWindow();
		w.show();
		while (w.frame.isDisplayable() && w.frame.isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// keep waiting
			}
		}
	}
}