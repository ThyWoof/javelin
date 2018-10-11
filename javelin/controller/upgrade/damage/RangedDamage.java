package javelin.controller.upgrade.damage;

import java.util.List;

import javelin.model.unit.Monster;
import javelin.model.unit.attack.AttackSequence;

/**
 * Upgrades ranged damage.
 *
 * @author alex
 */
public class RangedDamage extends Damage{
	public RangedDamage(){
		super("More ranged damage");
	}

	@Override
	protected List<AttackSequence> getattacktype(final Monster m){
		return m.ranged;
	}

	@Override
	public void incrementupgradecount(final Monster m){
		m.rangeddamageupgrades+=1;
	}
}