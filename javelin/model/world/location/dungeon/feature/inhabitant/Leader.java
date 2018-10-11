package javelin.model.world.location.dungeon.feature.inhabitant;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.challenge.ChallengeCalculator;
import javelin.controller.challenge.Difficulty;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.exception.battle.StartBattle;
import javelin.controller.fight.Fight;
import javelin.model.unit.Combatant;
import javelin.model.unit.Combatants;
import javelin.model.unit.Monster;
import javelin.model.unit.Squad;
import javelin.model.unit.skill.Skill;
import javelin.model.world.location.dungeon.Dungeon;
import javelin.model.world.location.unique.MercenariesGuild;
import javelin.view.screen.Option;
import javelin.view.screen.town.SelectScreen;

public class Leader extends Inhabitant{
	static final Option ATTACK=new Option("",0,'a',2);
	static final Option HIRE=new Option("",0,'h',2);

	class Treaty extends Option{
		String monster=null;
		Boolean lawful=null;
		Boolean good=null;

		Treaty(int price,String monster,Boolean lawful,Boolean good){
			super("Make treaty with all ",price);
			this.monster=monster;
			this.lawful=lawful;
			this.good=good;
			if(monster!=null)
				name+=monster;
			else if(lawful!=null)
				name+=(lawful?"lawful":"chaotic")+" inhabitants";
			else if(good!=null) name+=(good?"good":"evil")+" inhabitants";
		}

		boolean applies(Combatants encounter){
			if(encounter==null) return false;
			for(Combatant c:encounter)
				if(monster!=null){
					if(monster.equals(c.source.name)) return true;
				}else if(lawful!=null){
					if(lawful.equals(c.source.lawful)) return true;
				}else if(lawful!=null&&good.equals(c.source.good)) return true;
			return false;
		}

		boolean applies(ArrayList<Combatants> encounters){
			for(Combatants encounter:encounters)
				if(applies(encounter)) return true;
			return false;
		}

		void register(ArrayList<Combatants> encounters,ArrayList<Option> options){
			if((lawful==null||lawful.equals(base.lawful))
					&&(good==null||good.equals(base.good))&&applies(encounters))
				options.add(this);
		}

		void sign(){
			ArrayList<Combatants> encounters=Dungeon.active.encounters;
			for(Combatants encounter:new ArrayList<>(encounters)){
				if(encounters.size()==1) return;
				if(applies(encounter))
					encounters.set(encounters.indexOf(encounter),null);
			}
		}
	}

	class LeaderScreen extends SelectScreen{
		class LeaderFight extends Fight{
			LeaderFight(){
				hide=false;
				bribe=false;
			}

			@Override
			public ArrayList<Combatant> getfoes(Integer teamel){
				return guards;
			}
		}

		LeaderScreen(){
			super("You parlay with a "+base+" leader:",null);
		}

		@Override
		public String getCurrency(){
			return "";
		}

		@Override
		public String printpriceinfo(Option o){
			return o.price==0?"":" $"+Javelin.format(o.price);
		}

		@Override
		public String printinfo(){
			String debug="";
			if(Javelin.DEBUG){
				debug="Diplomacy DC "+diplomacydc+"\n";
				debug+="Your diplomacy: "
						+Squad.active.getbest(Skill.DIPLOMACY).taketen(Skill.DIPLOMACY)
						+"\n";
				debug+="Alignment: "+base.lawful+" "+base.good;
				debug+="\n\n";
			}
			return debug+"Your squad has $"+Javelin.format(Squad.active.gold)+".";
		}

		@Override
		public List<Option> getoptions(){
			ATTACK.name="Attack ("+Difficulty.describe(guards)+")";
			ArrayList<Option> options=new ArrayList<>();
			options.add(ATTACK);
			int diplomacy=Squad.active.getbest(Skill.DIPLOMACY)
					.taketen(Skill.DIPLOMACY);
			if(diplomacy<diplomacydc-5) return options;
			if(guards.size()>1){
				HIRE.name="Hire "+base;
				HIRE.price=MercenariesGuild.getfee(base);
				options.add(HIRE);
			}
			return addtreaties(options,diplomacy);
		}

		List<Option> addtreaties(ArrayList<Option> options,int diplomacy){
			if(diplomacy<diplomacydc) return options;
			ArrayList<Combatants> encounters=Dungeon.active.encounters;
			if(encounters.size()<2) return options;
			int treatyprice=10
					*Javelin.round(RewardCalculator.getgold(inhabitant.source.cr));
			new Treaty(treatyprice,base.name,null,null).register(encounters,options);
			if(diplomacy<diplomacydc+5) return options;
			treatyprice*=5+Dungeon.active.gettier().tier;
			new Treaty(treatyprice,null,base.lawful,null).register(encounters,
					options);
			new Treaty(treatyprice,null,null,base.good).register(encounters,options);
			return options;
		}

		@Override
		public boolean select(Option o){
			if(o==ATTACK){
				Leader.this.remove();
				for(Combatant c:Squad.active.getmercenaries())
					if(c.source.name.equals(base.name)){
						Squad.active.members.remove(c);
						guards.add(c);
					}
				throw new StartBattle(new LeaderFight());
			}
			if(o.price>Squad.active.gold){
				print(text+"\nNot enough gold...");
				return false;
			}
			Squad.active.gold-=o.price;
			if(o==HIRE){
				guards.remove(1);
				Combatant mercenary=new Combatant(base,true);
				mercenary.setmercenary(true);
				Squad.active.members.add(mercenary);
				return true;
			}
			if(o instanceof Treaty){
				((Treaty)o).sign();
				return true;
			}
			return false;
		}

	}

	/**
	 * First entry is the Leader himself. Others are #base clones added to ensure
	 * a high-level encounter.
	 */
	Combatants guards=new Combatants();
	Monster base;

	/** Constructor. */
	public Leader(){
		super(Dungeon.active.level+Difficulty.MODERATE,
				Dungeon.active.level+Difficulty.DEADLY);
		base=Javelin.getmonster(inhabitant.source.name);
		guards.add(inhabitant);
		while(ChallengeCalculator.calculateel(guards)<Dungeon.active.level)
			guards.add(new Combatant(base,true));
	}

	@Override
	public boolean activate(){
		new LeaderScreen().show();
		return true;
	}
}
