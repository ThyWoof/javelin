package javelin.model.diplomacy.mandate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javelin.Javelin;
import javelin.controller.ContentSummary;
import javelin.model.diplomacy.Diplomacy;
import javelin.model.diplomacy.Relationship;
import javelin.model.diplomacy.mandate.influence.ImproveRelationship;
import javelin.model.diplomacy.mandate.influence.Insult;
import javelin.model.diplomacy.mandate.meta.RaiseHandSize;
import javelin.model.diplomacy.mandate.meta.Redraw;
import javelin.model.diplomacy.mandate.unit.RequestMercenaries;
import javelin.model.unit.Squad;
import javelin.model.world.location.town.District;
import javelin.model.world.location.town.Town;
import javelin.old.RPG;

/**
 * A {@link Diplomacy} action.
 *
 * @author alex
 */
public abstract class Mandate implements Serializable,Comparable<Mandate>{
	/** All types of mandates. */
	static final List<Class<? extends Mandate>> MANDATES=new ArrayList<>(List.of(
			RaiseHandSize.class,Redraw.class,RequestGold.class,
			RequestMercenaries.class,RevealAlignment.class,ImproveRelationship.class,
			Insult.class,RequestTrade.class,RequestMap.class,RequestAlly.class));
	/**
	 * If {@link Javelin#DEBUG} and not-<code>null</code>, will prioritize this
	 * card type over others.
	 */
	static final Class<? extends Mandate> DEBUG=null;

	/**
	 * Used for equality as well.
	 *
	 * @see #getname()
	 */
	public String name;
	/** May be ignored by cards that have no target. */
	protected Relationship target;

	/** Reflection constructor. */
	public Mandate(Relationship r){
		target=r;
	}

	/**
	 * @return Text to be shown to player describing this action and (possible)
	 *         target(s).
	 */
	public abstract String getname();

	/**
	 * @param diplomacy
	 * @return If <code>false</code>, will impede this from being drawn into the
	 *         Mandate hand. If already on hand, will remove it.
	 * @see #name
	 */
	public abstract boolean validate(Diplomacy d);

	@Override
	public int hashCode(){
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o){
		return o instanceof Mandate&&name.equals(((Mandate)o).name);
	}

	@Override
	public int compareTo(Mandate o){
		return name.compareTo(o.name);
	}

	/** What to do once this card is played. */
	public abstract void act(Diplomacy d);

	/** Called once after a card is instantiated and validated. */
	public void define(){
		name=getname()+".";
	}

	/**
	 * @return A squad in the {@link #target}'s {@link District} or
	 *         <code>null</code> if none present.
	 */
	protected Squad getsquad(){
		var squads=target.town.getdistrict().getsquads();
		return squads.isEmpty()?null:RPG.pick(squads);
	}

	/**
	 * @return <code>true</code> if added a valid, non-repeated mandate card to
	 *         {@link Diplomacy#hand}.
	 */
	public static boolean generate(Diplomacy d){
		try{
			HashMap<Town,Relationship> relationships=d.getdiscovered();
			var discovered=new ArrayList<>(relationships.keySet());
			var deck=RPG.shuffle(MANDATES);
			if(Javelin.DEBUG&&DEBUG!=null){
				deck=new ArrayList<>(deck);
				deck.remove(DEBUG);
				deck.add(0,DEBUG);
			}
			for(var type:deck)
				for(var town:RPG.shuffle(discovered)){
					var card=type.getConstructor(Relationship.class)
							.newInstance(relationships.get(town));
					if(!card.validate(d)) continue;
					card.define();
					if(d.hand.add(card)) return true;
				}
			return false;
		}catch(ReflectiveOperationException e){
			throw new RuntimeException(e);
		}
	}

	/** @see ContentSummary */
	public static String printsummary(){
		return MANDATES.size()+" diplomatic actions";
	}
}