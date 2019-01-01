package javelin.model.world.location.fortification;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.challenge.ChallengeCalculator;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.fight.Siege;
import javelin.model.diplomacy.Diplomacy;
import javelin.model.item.Ruby;
import javelin.model.item.key.TempleKey;
import javelin.model.item.key.door.MasterKey;
import javelin.model.unit.Combatant;
import javelin.model.unit.Squad;
import javelin.model.world.World;
import javelin.model.world.location.Location;
import javelin.model.world.location.ResourceSite;
import javelin.model.world.location.ResourceSite.Resource;
import javelin.old.RPG;
import javelin.old.messagepanel.MessagePanel;

/**
 * Represents all the resource types found in the game: gold, experience, keys,
 * labor and rubies. Usually only 2 are offered per instance though, to increase
 * randomization.
 *
 * Since the actual fight gives no xp or gold these results are doubled as
 * treasure.
 *
 * TODO experience was a nice reward but too explotiable. You could dismiss
 * mercenaries and have a larger XP reward, or divide the squad and have only
 * reiceve all XP, which is super explotaible. If could trigger
 * {@link #reward(Reward)} just after the battle is over, this could be easily
 * solved.
 *
 * @author alex
 */
public class Trove extends Fortification{
	static final String DESCRIPTION="A treasure trove";
	static final boolean DEBUG=false;

	enum Reward{
		GOLD,EXPERIENCE,TEMPLEKEY,RUBY,KEY,REPUTATION,RESOURCE;

		static Reward getrandom(){
			Reward[] all=values();
			return all[RPG.r(0,all.length-1)];
		}
	}

	class TroveFight extends Siege{
		public TroveFight(Location l){
			super(l);
			rewardgold=false;
			rewardxp=false;
		}

		@Override
		public String reward(){
			String message=take();
			MessagePanel.active.clear();
			return message;
		}
	}

	TempleKey key=null;
	Reward[] rewards=new Reward[2];
	List<Combatant> originalgarrison=new ArrayList<>();
	Resource resource=RPG.pick(new ArrayList<>(ResourceSite.RESOURCES.values()));

	/** Constructor. */
	public Trove(){
		super(DESCRIPTION,DESCRIPTION,1,20);
		if(World.scenario.simpletroves){
			rewards[0]=Reward.EXPERIENCE;
			rewards[1]=Reward.GOLD;
		}else{
			rewards[0]=Reward.getrandom();
			while(rewards[1]==null||rewards[0]==rewards[1])
				rewards[1]=Reward.getrandom();
			if(rewards[0]==Reward.TEMPLEKEY||rewards[1]==Reward.TEMPLEKEY)
				key=TempleKey.generate();
		}
		descriptionknown+=" ("+describe(rewards[0])+" or "+describe(rewards[1])+")";
		discard=false;
		vision=0;
		link=false;
	}

	String describe(Reward reward){
		Object o;
		if(reward==Reward.TEMPLEKEY) o=key;
		if(reward==Reward.RESOURCE)
			o="Natural resource: "+resource;
		else
			o=reward;
		return o.toString().toLowerCase();
	}

	@Override
	protected Siege fight(){
		return new TroveFight(this);
	}

	@Override
	protected void generategarrison(int minlevel,int maxlevel){
		super.generategarrison(minlevel,maxlevel);
		for(Combatant c:garrison)
			originalgarrison.add(c.clone());
	}

	String reward(Reward reward){
		if(reward==Reward.EXPERIENCE)
			return RewardCalculator.rewardxp(Squad.active.members,originalgarrison,2);
		if(reward==Reward.GOLD){
			int gold=Javelin.round(RewardCalculator.receivegold(originalgarrison)*2);
			Squad.active.gold+=gold;
			return "Party receives $"+Javelin.format(gold)+"!";
		}
		if(reward==Reward.TEMPLEKEY){
			key.grab();
			return null;
		}
		if(reward==Reward.RUBY){
			new Ruby().grab();
			return null;
		}
		if(reward==Reward.KEY){
			new MasterKey().grab();
			return null;
		}
		if(reward==Reward.REPUTATION){
			int reputation=ChallengeCalculator.calculateel(originalgarrison);
			Diplomacy.instance.reputation+=reputation;
			return "You gain "+reputation+" reputation!";
		}
		if(reward==Reward.RESOURCE){
			new ResourceSite.ResourceLink(resource,null).grab();
			return null;
		}
		throw new RuntimeException(reward+" #unknownreward");
	}

	@Override
	public List<Combatant> getcombatants(){
		return garrison;
	}

	String take(){
		remove();
		ArrayList<String> choices=new ArrayList<>(rewards.length);
		for(Reward r:rewards)
			choices.add(describe(r));
		String prompt="What will you take as a spoil from this trove?";
		int choice=Javelin.choose(prompt,choices,false,true);
		return reward(rewards[choice]);
	}

	@SuppressWarnings("unused")
	@Override
	public boolean interact(){
		if(Javelin.DEBUG&&DEBUG){
			if(!super.interact()) return false;
			var message=take();
			if(message!=null) Javelin.message(message,false);
			return true;
		}
		return super.interact();
	}
}