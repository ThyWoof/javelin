package javelin.controller.action.ai.attack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javelin.Javelin;
import javelin.controller.action.Action;
import javelin.controller.action.ai.AiAction;
import javelin.controller.ai.ChanceNode;
import javelin.controller.ai.Node;
import javelin.controller.audio.Audio;
import javelin.model.state.BattleState;
import javelin.model.unit.Combatant;
import javelin.model.unit.CurrentAttack;
import javelin.model.unit.abilities.discipline.Maneuver;
import javelin.model.unit.abilities.discipline.Strike;
import javelin.model.unit.abilities.spell.Spell;
import javelin.model.unit.attack.Attack;
import javelin.model.unit.attack.AttackSequence;
import javelin.model.unit.feat.attack.Cleave;
import javelin.model.unit.skill.Bluff;
import javelin.view.mappanel.battle.overlay.AiOverlay;

/**
 * Base class for {@link MeleeAttack} and {@link RangedAttack}.
 *
 * @author alex
 */
public abstract class AbstractAttack extends Action implements AiAction{
	/**
	 * Inspired by (but deals minimum damage instead of half)
	 * https://dnd-wiki.org/wiki/Graze_Damage_(3.5e_Variant_Rule)#dynamic_user_navbox
	 *
	 * Found the link when looking for a less miss-prone variant combat rules.
	 */
	static final boolean GRAZE=true;
	/**
	 * If <code>true</code>, always applies average damage. Disabled because there
	 * doesn't seem to be any drastic performance improvement from it.
	 */
	static final boolean FLATDAMAGE=false;

	static final ConcurrentHashMap<Thread,Strike> CURRENTMANEUVER=new ConcurrentHashMap<>();

	class DamageNode extends ChanceNode{
		public DamageChance damage;

		DamageNode(Node n,DamageChance damage,String action,Javelin.Delay delay,
				Combatant active,Combatant target,String audio){
			super(n,damage.chance,action,delay);
			this.damage=damage;
			overlay=new AiOverlay(target.location[0],target.location[1]);
			this.audio=target.hp<=0?new Audio("die",target):new Audio(audio,active);
		}
	}

	/** @see Bluff#feign(Combatant) */
	protected boolean feign=false;
	/** @see Cleave */
	protected boolean cleave=false;
	String hitsound;
	String misssound;

	public AbstractAttack(final String name,String hitsound,String misssound){
		super(name);
		this.hitsound=hitsound;
		this.misssound=misssound;
	}

	public List<ChanceNode> attack(final BattleState s,final Combatant current,
			final Combatant target,CurrentAttack attacks,int bonus){
		final Attack a=attacks.getnext();
		final int damagebonus=getdamagebonus(current,target);
		final float ap=AbstractAttack
				.calculateattackap(getattacks(current).get(attacks.sequenceindex));
		return attack(current,target,a,bonus,damagebonus,ap,s);
	}

	protected int getdamagebonus(Combatant attacker,Combatant target){
		return 0;
	}

	public List<ChanceNode> attack(Combatant attacker,Combatant target,
			final Attack a,int attackbonus,int damagebonus,final float ap,
			BattleState s){
		final Strike m=getmaneuver();
		s=s.clone();
		attacker=s.clone(attacker);
		attacker.ap+=ap;
		if(m!=null) m.preattacks(attacker,target,a,s);
		final ArrayList<ChanceNode> nodes=new ArrayList<>();
		for(final DamageChance dc:dealattack(attacker,target,a,attackbonus,s)){
			if(dc.damage>0) dc.damage+=damagebonus;
			if(dc.damage<0) dc.damage=0;
			nodes.add(createnode(attacker,target,a,ap,m,dc,s));
		}
		if(m!=null) m.postattacks(attacker,target,a,s);
		return nodes;
	}

	DamageNode createnode(Combatant attacker,Combatant target,final Attack a,
			float ap,final Strike m,final DamageChance dc,BattleState s){
		final String tohit=" ("+getchance(attacker,target,a,s)+")...";
		final StringBuilder sb=new StringBuilder(attacker.toString());
		if(dc.damage==0) return miss(attacker,target,m,dc,s,sb,tohit);
		s=s.clone();
		attacker=s.clone(attacker);
		target=s.clone(target);
		if(m!=null) m.prehit(attacker,target,a,dc,s);
		final String name=m==null?a.name:m.name.toLowerCase();
		sb.append(" "+dc.message+" ").append(target).append(" with ").append(name)
				.append(tohit);
		if(dc.critical) sb.append("\nCritical hit!");
		if(dc.damage==0)
			sb.append("\nDamage absorbed!");
		else{
			int resistance=a.energy?target.source.energyresistance:target.source.dr;
			target.damage(dc.damage,s,resistance);
			if(target.source.customName==null)
				sb.append("\n").append(target.toString());
			else
				sb.append("\n").append(target.toString());
			sb.append(" is ").append(target.getstatus()).append(".");
			posthit(attacker,target,a,ap,dc,s,sb);
		}
		if(m!=null) m.posthit(attacker,target,a,dc,s);
		boolean wait=target.source.passive
				&&target.getnumericstatus()>Combatant.STATUSUNCONSCIOUS;
		final Javelin.Delay delay=wait?Javelin.Delay.WAIT:Javelin.Delay.BLOCK;
		return new DamageNode(s,dc,sb.toString(),delay,attacker,target,hitsound);
	}

	/**
	 * Always a full attack (1AP) but divided among the {@link AttackSequence}.
	 * This would penalize creatures with only one attack so max AP cost is .5 per
	 * attack.
	 *
	 * If a {@link #CURRENTMANEUVER} is being used, returns {@link Maneuver}
	 * instead.
	 */
	static float calculateattackap(final AttackSequence attacks){
		final Maneuver m=getmaneuver();
		if(m!=null) return m.ap;
		final int nattacks=attacks.size();
		if(nattacks==1) return .5f;
		if(nattacks==2) /**
										 * if we let ap=.5 in this case it means that a combatant
										 * with a 2-attack sequence is identical to one with 1
										 * attack
										 */
			return .4f;
		return 1f/nattacks;
	}

	List<DamageChance> dealattack(final Combatant active,final Combatant target,
			final Attack a,int bonus,final BattleState s){
		bonus+=a.bonus;
		if(a.touch) bonus+=target.source.armor;
		final List<DamageChance> chances=new ArrayList<>();
		final float threatchance=(21-a.threat)/20f;
		final float misschance=misschance(s,active,target,bonus);
		final float grazechance=GRAZE?target.getac()-target.gettouchac()/20f:0;
		final float hitchance=1-misschance-grazechance;
		final float confirmchance=target.source.immunitytocritical?0
				:threatchance*hitchance;
		final Spell effect=target.source.passive?null:a.geteffect();
		final float savechance=effect==null?1:effect.getsavechance(active,target);
		final float nosavechance=1-savechance;
		chances.add(new DamageChance(misschance,0,false,null));
		if(grazechance>0){
			var graze=new DamageChance(grazechance,a.getminimumdamage(),false,null);
			graze.message="grazes";
			chances.add(graze);
		}
		hit(a,(hitchance-confirmchance)*savechance,1,target,true,chances);
		hit(a,(hitchance-confirmchance)*nosavechance,1,target,false,chances);
		hit(a,confirmchance*savechance,a.multiplier,target,true,chances);
		hit(a,confirmchance*nosavechance,a.multiplier,target,false,chances);
		if(Javelin.DEBUG) AbstractAttack.validate(chances);
		return chances;
	}

	/**
	 * @param attackbonus Bonus of the given any extraordinary bonuses (such as +2
	 *          from charge). Most common chances are calculated here or by the
	 *          concrete class.
	 * @return A bound % chance of an attack completely missing it's target.
	 * @see #bind(float)
	 */
	public float misschance(final BattleState gameState,final Combatant current,
			final Combatant target,final int attackbonus){
		final int penalty=getpenalty(current,target,gameState);
		final float misschance=(target.getac()+penalty-attackbonus)/20f;
		return Action.bind(addchances(misschance,target.source.misschance));
	}

	/**
	 * @return the chance of at least 1 out of 2 independent events happening,
	 *         given two percentage odds (1 = 100%).
	 */
	static public float addchances(float a,float b){
		return a+b-a*b;
	}

	static void hit(final Attack a,final float hitchance,final int multiplier,
			Combatant target,Boolean save,final List<DamageChance> chances){
		if(hitchance==0) return;
		if(a.geteffect()==null) save=null;
		if(FLATDAMAGE){
			var damage=a.getaveragedamage();
			chances.add(new DamageChance(hitchance,damage,multiplier!=1,save));
			return;
		}
		var damagerolls=Action.distributeroll(a.damage[0],a.damage[1]).entrySet();
		for(var roll:damagerolls){
			int damage=Math.max(1,(roll.getKey()+a.damage[2])*multiplier);
			final float chance=hitchance*roll.getValue();
			chances.add(new DamageChance(chance,damage,multiplier!=1,save));
		}
	}

	static public void validate(final List<DamageChance> thisattack){
		/* validate */
		float sum=0;
		for(final DamageChance verify:thisattack)
			sum+=verify.chance;
		if(sum>1.001||sum<0.999)
			throw new RuntimeException("Attack sum not whole: "+sum);
	}

	/**
	 * @param current Checks if swimmer.
	 * @return The penalty for attacking while standing on water (same as the
	 *         bonus for being attacked while staning on water).
	 */
	static int waterpenalty(final BattleState gameState,final Combatant current){
		return current.source.swim()>0
				&&gameState.map[current.location[0]][current.location[1]].flooded?2:0;
	}

	/**
	 * @param target Target of the attack
	 * @return Positive integer describing a penalty.
	 */
	public int getpenalty(final Combatant attacker,final Combatant target,
			final BattleState s){
		return AbstractAttack.waterpenalty(s,attacker)
				-AbstractAttack.waterpenalty(s,target)+target.surprise()
				+(target.burrowed?4:0);
	}

	DamageNode miss(Combatant attacker,Combatant target,final Strike m,
			final DamageChance dc,BattleState s,final StringBuilder sb,
			final String tohit){
		if(feign&&target.source.dexterity>=12){
			s=s.clone();
			target=s.clone(target);
			Bluff.feign(attacker,target);
		}
		final String name;
		final Javelin.Delay wait;
		if(m==null){
			name=target.toString();
			wait=Javelin.Delay.WAIT;
		}else{
			name=m.name.toLowerCase();
			wait=Javelin.Delay.BLOCK;
		}
		sb.append(" misses ").append(name).append(tohit);
		return new DamageNode(s,dc,sb.toString(),wait,attacker,target,misssound);
	}

	void posthit(Combatant active,Combatant target,final Attack a,float ap,
			final DamageChance dc,final BattleState s,StringBuilder sb){
		if(target.hp<=0){
			if(cleave) active.cleave(ap);
		}else if(dc.save!=null){
			target.source=target.source.clone();
			active.source=active.source.clone();
			final String effect=a.geteffect().cast(active,target,dc.save,s,null);
			sb.append("\n").append(effect);
		}
	}

	abstract List<AttackSequence> getattacks(Combatant active);

	/**
	 * @return An ongoing attack or all the possible {@link AttackSequence}s that
	 *         can be initiated.
	 */
	List<Integer> getcurrentattack(final Combatant active){
		final List<AttackSequence> attacktype=getattacks(active);
		if(attacktype.isEmpty()) return new ArrayList<>(0);
		final CurrentAttack current=active.getcurrentattack(attacktype);
		if(current.continueattack()){
			final ArrayList<Integer> attacks=new ArrayList<>(1);
			attacks.add(current.sequenceindex);
			return attacks;
		}
		final int nattacks=attacktype.size();
		final ArrayList<Integer> attacks=new ArrayList<>(nattacks);
		for(int i=0;i<nattacks;i++)
			attacks.add(i);
		return attacks;
	}

	@Override
	public boolean perform(Combatant active){
		return false;
	}

	/** @return An estimate of the chance of hitting an attack ("easy to hit"). */
	public String getchance(Combatant c,Combatant target,Attack a,BattleState s){
		float misschance=misschance(s,c,target,a.bonus);
		return Javelin.translatetochance(Math.round(20*misschance))+" to hit";
	}

	/**
	 * @return Same as
	 *         {@link #getchance(Combatant, Combatant, Attack, BattleState)} but
	 *         predicts {@link CurrentAttack}.
	 */
	public String getchance(Combatant c,Combatant target,BattleState s){
		CurrentAttack current=c.getcurrentattack(getattacks(c));
		final List<Attack> attack=current.next==null||current.next.isEmpty()
				?c.source.melee.get(0)
				:current.next;
		return MeleeAttack.SINGLETON.getchance(c,target,attack.get(0),s);
	}

	static Strike getmaneuver(){
		return CURRENTMANEUVER.get(Thread.currentThread());
	}

	/**
	 * Sets the current {@link Maneuver} which should be taken as context during
	 * the execution of this class. Since this class needs to be thread-safe this
	 * is backed by a {@link ConcurrentHashMap} in order to properly synchronize
	 * setting and clearing this for any given thread at the right time.
	 */
	public static void setmaneuver(Strike m){
		final Thread t=Thread.currentThread();
		if(m==null)
			CURRENTMANEUVER.remove(t);
		else
			CURRENTMANEUVER.put(t,m);
	}
}