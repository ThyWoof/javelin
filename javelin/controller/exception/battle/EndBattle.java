package javelin.controller.exception.battle;

import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.ai.ThreadManager;
import javelin.controller.ai.cache.AiCache;
import javelin.controller.fight.Fight;
import javelin.controller.terrain.Terrain;
import javelin.controller.wish.Ressurect;
import javelin.model.item.Item;
import javelin.model.item.Scroll;
import javelin.model.state.BattleState;
import javelin.model.unit.Combatant;
import javelin.model.unit.Squad;
import javelin.model.unit.abilities.spell.Spell;
import javelin.model.unit.abilities.spell.conjuration.healing.RaiseDead;
import javelin.model.world.Incursion;
import javelin.model.world.World;
import javelin.model.world.location.dungeon.Dungeon;
import javelin.model.world.location.dungeon.temple.Temple;
import javelin.model.world.location.unique.MercenariesGuild;
import javelin.old.messagepanel.MessagePanel;
import javelin.view.screen.BattleScreen;

/**
 * A victory or defeat condition has been achieved.
 *
 * @author alex
 */
public class EndBattle extends BattleEvent{
	/** Start after-{@link Fight} cleanup. */
	public static void end(){
		Fight.victory=Javelin.app.fight.win();
		terminateconditions(Fight.state,BattleScreen.active);
		if(Javelin.app.fight.onend()){
			int nsquads=World.getall(Squad.class).size();
			if(Squad.active!=null&&nsquads==World.getall(Squad.class).size()){
				while(World.get(Squad.active.x,Squad.active.y,Incursion.class)!=null){
					Squad.active.displace();
					Squad.active.place();
				}
				end(Fight.originalblueteam);
				if(Dungeon.active!=null){
					Temple.climbing=false;
					Dungeon.active.activate(false);
				}
			}
		}
		AiCache.reset();
		if(World.scenario!=null)
			World.scenario.end(Javelin.app.fight,Fight.victory);
		Javelin.app.fight=null;
		Fight.state=null;
	}

	static void terminateconditions(BattleState s,BattleScreen screen){
		screen.block();
		for(Combatant c:Fight.state.getcombatants())
			c.finishconditions(s,screen);
	}

	/**
	 * Prints combat info (rewards, etc).
	 *
	 * @param prefix
	 */
	public static void showcombatresult(){
		MessagePanel.active.clear();
		String combatresult;
		if(Fight.victory)
			combatresult=Javelin.app.fight.reward();
		else if(Fight.state.getfleeing(Fight.originalblueteam).isEmpty()){
			Squad.active.disband();
			combatresult="You lost!";
		}else if(Javelin.app.fight.friendly)
			combatresult="You lost!";
		else{
			combatresult="Fled from combat. No awards received.";
			if(!Fight.victory
					&&Fight.state.fleeing.size()!=Fight.originalblueteam.size()){
				combatresult+="\nFallen allies left behind are lost!";
				for(Combatant abandoned:Fight.state.dead)
					abandoned.hp=Combatant.DEADATHP;
			}
			if(Squad.active.transport!=null&&Dungeon.active==null
					&&!Terrain.current().equals(Terrain.WATER)){
				combatresult+=" Vehicle lost!";
				Squad.active.transport=null;
				Squad.active.updateavatar();
			}
		}
		Javelin.message(combatresult+"\nPress any key to continue...",
				Javelin.Delay.BLOCK);
		BattleScreen.active.getUserInput();
	}

	static void updateoriginal(List<Combatant> originalteam){
		ArrayList<Combatant> update=new ArrayList<>(Fight.state.blueTeam);
		update.addAll(Fight.state.dead);
		for(final Combatant inbattle:update){
			int originali=originalteam.indexOf(inbattle);
			if(originali>=0) update(inbattle,originalteam.get(originali));
		}
	}

	static void update(final Combatant from,final Combatant to){
		from.transferconditions(to);
		to.hp=from.hp;
		if(to.hp>to.maxhp)
			to.hp=to.maxhp;
		else if(to.hp<1) to.hp=1;
		copyspells(from,to);
	}

	static void copyspells(final Combatant from,final Combatant to){
		for(var spell:from.spells){
			var original=to.spells.has(spell.getClass());
			if(original!=null) original.used=spell.used;
		}
	}

	/**
	 * Tries to {@link #revive(Combatant)} the combatant. If can't, remove him
	 * from the game.
	 *
	 * TODO isn't updating {@link Ressurect#dead} when the entire Squad dies! this
	 * probably isn't being called
	 */
	static void bury(List<Combatant> originalteam){
		for(Combatant c:Fight.state.dead){
			if(!originalteam.contains(c)) continue;
			if(c.hp>Combatant.DEADATHP&&c.source.constitution>0)
				c.hp=1;
			else if(!Fight.victory||!revive(c,originalteam)){
				ArrayList<Item> bag=Squad.active.equipment.get(c);
				originalteam.remove(c);
				Squad.active.remove(c);
				//TODO expire all effects
				//TODO unequip artifacts as well
				if(Fight.victory) for(Item i:bag)
					i.grab();
				MercenariesGuild.die(c);
				if(!c.summoned&&!c.mercenary) Ressurect.dead=c;
			}
		}
		Fight.state.dead.clear();
	}

	/**
	 * TODO this doesnt let you select between spell or scroll, between which
	 * instance of nay of those nor between which characters to use it with. In
	 * short, we need a screen for all that.
	 */
	static boolean revive(Combatant dead,List<Combatant> originalteam){
		List<Combatant> alive=new ArrayList<>(originalteam);
		alive.removeAll(Fight.state.dead);
		Spell spell=castrevive(alive);
		Scroll scroll=null;
		if(scroll==null){
			scroll=findressurectscroll(alive);
			if(scroll!=null) spell=scroll.spell;
		}
		if(spell==null||!spell.validate(null,dead)) return false;
		spell.castpeacefully(null,dead,originalteam);
		if(scroll==null)
			spell.used+=1;
		else
			Squad.active.equipment.remove(scroll);
		return true;
	}

	static Scroll findressurectscroll(List<Combatant> alive){
		List<Scroll> ressurectscrolls=new ArrayList<>();
		for(Scroll s:Squad.active.equipment.getall(Scroll.class))
			if(s.spell instanceof RaiseDead) ressurectscrolls.add(s);
		if(ressurectscrolls.isEmpty()) return null;
		for(Combatant c:alive)
			for(Scroll s:ressurectscrolls)
				if(s.canuse(c)==null) return s;
		return null;
	}

	static Spell castrevive(List<Combatant> alive){
		for(Combatant c:alive)
			for(Spell s:c.spells)
				if(s instanceof RaiseDead&&!s.exhausted()) return s;
		return null;
	}

	static void end(ArrayList<Combatant> originalteam){
		for(Combatant c:Fight.state.getcombatants())
			if(c.summoned){
				Fight.state.blueTeam.remove(c);
				Fight.state.redTeam.remove(c);
			}
		updateoriginal(originalteam);
		bury(originalteam);
		Squad.active.members=originalteam;
		for(Combatant member:Squad.active.members){
			member.currentmelee.sequenceindex=-1;
			member.currentranged.sequenceindex=-1;
		}
		ThreadManager.printbattlerecord();
	}
}
