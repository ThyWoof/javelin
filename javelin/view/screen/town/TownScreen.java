package javelin.view.screen.town;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javelin.Javelin;
import javelin.model.unit.Combatant;
import javelin.model.unit.Squad;
import javelin.model.world.Actor;
import javelin.model.world.World;
import javelin.model.world.location.town.Town;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.Option;
import javelin.view.screen.town.option.ScreenOption;
import javelin.view.screen.town.option.TournamentScreenOption;

/**
 * Shown when a {@link Squad} enters a {@link Town}.
 *
 * @author alex
 */
public class TownScreen extends PurchaseScreen{
	static final Option SETTLE=new Option("Settle worker",0,'s');
	static final boolean DEBUGMANAGEMENT=false;
	static final Option RENAME=new Option("Rename town",0,'r');

	class Manage extends ScreenOption{
		public Manage(Town town){
			super("Manage town",town,'m');
		}

		@Override
		public SelectScreen getscreen(){
			return new GovernorScreen(t);
		}
	}

	/** Constructor. */
	public TownScreen(final Town t){
		super(title(t),t);
	}

	static String title(final Actor t){
		return "Welcome to "+t+"!";
	}

	@Override
	public boolean select(final Option o){
		if(o instanceof ScreenOption){
			SelectScreen screen=((ScreenOption)o).getscreen();
			screen.show();
			if(screen.forceclose) stayopen=false;
			return true;
		}
		if(!super.select(o)) return false;
		if(o==RENAME){
			town.rename();
			title=title(town)+"\n\n";
			return true;
		}
		if(o==SETTLE) return retire(town);
		stayopen=false;
		return true;
	}

	boolean retire(Town town){
		List<Combatant> retirees=new ArrayList<>();
		for(Combatant c:Squad.active.members)
			if(!c.mercenary) retirees.add(c);
		if(retirees.isEmpty()) return false;
		int choice=Javelin.choose(
				"Which member should retire and become local labor?",retirees,true,
				false);
		if(choice<0) return false;
		Squad.active.remove(retirees.get(choice));
		town.population+=1;
		return true;
	}

	@Override
	public List<Option> getoptions(){
		final ArrayList<Option> list=new ArrayList<>();
		if(World.scenario.labormodifier>0) list.add(new Manage(town));
		list.add(RENAME);
		list.add(SETTLE);
		if(town.ishosting())
			list.add(new TournamentScreenOption("Enter tournament",town,'t'));
		return list;
	}

	@Override
	public String printpriceinfo(Option o){
		return o.price>0?super.printpriceinfo(o):"";
	}

	@Override
	public void onexit(){
		Javelin.app.switchScreen(BattleScreen.active);
	}

	@Override
	public String printinfo(){
		var info="Your squad has $"+Javelin.format(Squad.active.gold)+".";
		if(!town.quests.isEmpty()){
			info+="\n\nActive quests:\n";
			var quests=town.quests.stream().sorted((a,b)->a.daysleft-b.daysleft)
					.map(q->"- "+q+", "+q.getdeadline()+", reward: "
							+q.describereward().toLowerCase());
			info+=String.join("\n",quests.collect(Collectors.toList()));
		}
		return info;
	}

	@Override
	protected Comparator<Option> sort(){
		return (o1,o2)->o1.key.compareTo(o2.key);
	}

	@Override
	protected boolean select(char feedback,List<Option> options){
		return super.select(feedback,options);
	}
}
