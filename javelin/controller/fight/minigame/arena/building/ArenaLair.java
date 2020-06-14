package javelin.controller.fight.minigame.arena.building;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javelin.Javelin;
import javelin.controller.fight.Fight;
import javelin.controller.fight.minigame.arena.ArenaMinigame;
import javelin.controller.generator.encounter.EncounterGenerator;
import javelin.controller.terrain.Terrain;
import javelin.model.unit.Combatant;
import javelin.old.RPG;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.Option;
import javelin.view.screen.town.PurchaseScreen;

public class ArenaLair extends ArenaBuilding{
	public static final int OPTIONS=9;

	class HireOption extends Option{
		ArrayList<Combatant> group;

		public HireOption(ArrayList<Combatant> group){
			super(Javelin.group(group),calculateprice(group));
			this.group=group;
		}
	}

	class ArenaLairScreen extends PurchaseScreen{
		ArrayList<Combatant> hired=null;

		public ArenaLairScreen(){
			super("Which group of allies do you wish to hire?",null);
			stayopen=false;
		}

		@Override
		protected int getgold(){
			return ArenaMinigame.get().gold;
		}

		@Override
		protected void spend(Option o){
			ArenaMinigame.get().gold-=o.price;
			hired=((HireOption)o).group;
			for(Combatant c:hired)
				c.setmercenary(true);
			hires.remove(hired);
		}

		@Override
		public void onexit(){
			super.onexit();
			if(hired!=null){
				Javelin.app.switchScreen(BattleScreen.active);
				ArenaMinigame.get().enter(hired,Fight.state.blueTeam,getlocation());
			}
		}

		@Override
		public List<Option> getoptions(){
			ArrayList<Option> options=new ArrayList<>(hires.size());
			for(ArrayList<Combatant> group:hires)
				options.add(new HireOption(group));
			return options;
		}
	}

	ArrayList<ArrayList<Combatant>> hires=new ArrayList<>(OPTIONS);

	public ArenaLair(){
		super("Lair","locationmercenariesguild",
				"Click this lair to recruit allies into the arena!");
		stock();
	}

	void stock(){
		int levelmin=level*5+1;
		int levelmax=levelmin+4;
		while(hires.size()<OPTIONS)
			hires.add(EncounterGenerator.generate(RPG.r(levelmin,levelmax),
					Arrays.asList(Terrain.ALL)));
	}

	@Override
	protected boolean click(Combatant current){
		new ArenaLairScreen().show();
		return true;
	}

	static int calculateprice(ArrayList<Combatant> group){
		int fee=0;
		for(Combatant c:group)
			fee+=c.pay();
		return fee*10;
	}

	@Override
	public String getactiondescription(Combatant current){
		String info;
		if(hires.isEmpty())
			info="Currently empty";
		else{
			List<Integer> prices=hires.stream().map(hire->calculateprice(hire))
					.collect(Collectors.toList());
			info="\nHires between $"+prices.stream().reduce(Math::min)+" and $"
					+prices.stream().reduce(Math::max)+".";
		}
		return super.getactiondescription(current)+getgoldinfo()+info;
	}

	public static String getgoldinfo(){
		return "\n\nYour gladiators currently have $"
				+Javelin.format(ArenaMinigame.get().gold)+".";
	}

	@Override
	protected void upgradebuilding(){
		hires.clear();
		stock();
	}
}
