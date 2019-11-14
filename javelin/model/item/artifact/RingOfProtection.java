package javelin.model.item.artifact;

import javelin.model.unit.Combatant;
import javelin.model.unit.Slot;

/**
 * Adds AC bonus.
 *
 * @author alex
 */
public class RingOfProtection extends Artifact{
	private int bonus;

	/** Constructor. */
	public RingOfProtection(int bonus,int price){
		super("Ring of protection +"+bonus,price,Slot.RING);
		this.bonus=bonus;
	}

	@Override
	protected void apply(Combatant c){
		c.source.setrawac(c.source.getrawac()+bonus);
	}

	@Override
	protected void negate(Combatant c){
		c.source.setrawac(c.source.getrawac()-bonus);
	}
}
