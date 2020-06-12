package javelin.controller.event.urban;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javelin.controller.db.StateManager;
import javelin.controller.event.EventCard;
import javelin.controller.event.EventDealer;
import javelin.controller.event.urban.diplomatic.Badmouth;
import javelin.controller.event.urban.diplomatic.DegradeRelationship;
import javelin.controller.event.urban.diplomatic.ImproveRelationship;
import javelin.controller.event.urban.diplomatic.Praise;
import javelin.controller.event.urban.encounter.Guards;
import javelin.controller.event.urban.encounter.Robbers;
import javelin.controller.event.urban.negative.Fire;
import javelin.controller.event.urban.negative.Flood;
import javelin.controller.event.urban.negative.FoodShortage;
import javelin.controller.event.urban.negative.LoseResource;
import javelin.controller.event.urban.negative.Revolt;
import javelin.controller.event.urban.negative.Riot;
import javelin.controller.event.urban.negative.Sabotage;
import javelin.controller.event.urban.neutral.HostTournament;
import javelin.controller.event.urban.neutral.Migration;
import javelin.controller.event.urban.positive.CollectiveEffort;
import javelin.controller.event.urban.positive.FoodSurplus;
import javelin.controller.event.urban.positive.GainResource;
import javelin.model.unit.Squad;
import javelin.model.world.World;
import javelin.model.world.location.town.Town;
import javelin.model.world.location.town.labor.Trait;
import javelin.old.RPG;

/**
 * Events that happens autonously per {@link Town} regardless of it being
 * hostile or not. Players will be notified of events in non-hostile
 * {@link Town}s regardless of having a {@link Squad} present or not. For
 * hostile towns, they are only notified if present.
 *
 * @author alex
 * @see Town#ishostile()
 */
public class UrbanEvents extends EventDealer{
	/**
	 * Singleton instance.
	 *
	 * @see StateManager
	 */
	public static UrbanEvents instance=new UrbanEvents();
	/**
	 * Used by {@link #newinstance(Class)} to be passed as a constructor argument.
	 */
	public static Town generating;

	private UrbanEvents(){
		positive.addcontent(
				List.of(NothingHappens.class,ImproveRelationship.class,Praise.class,
						CollectiveEffort.class,FoodSurplus.class,GainResource.class));
		neutral.addcontent(
				List.of(NothingHappens.class,HostTournament.class,Migration.class));
		negative.addcontent(
				List.of(NothingHappens.class,DegradeRelationship.class,Guards.class,
						Badmouth.class,Riot.class,Fire.class,Sabotage.class,Revolt.class,
						Flood.class,FoodShortage.class,Robbers.class,LoseResource.class));
	}

	@Override
	protected EventCard newinstance(Class<? extends EventCard> type)
			throws ReflectiveOperationException{
		return type.getConstructor(Town.class).newInstance(generating);
	}

	@Override
	protected EventDeck choosedeck(){
		List<EventDeck> choices;
		var happiness=generating.diplomacy.getstatus();
		if(happiness>+1)
			choices=List.of(positive,positive,neutral,negative);
		else if(happiness<-1)
			choices=List.of(positive,neutral,negative,negative);
		else
			choices=List.of(positive,neutral,negative);
		return RPG.pick(choices);
	}

	@Override
	public String printsummary(String title){
		if(!World.scenario.urbanevents) return "(Urban events disabled)";
		var info=new ArrayList<String>();
		info.add(positive.getcontentsize()+" positive");
		info.add(neutral.getcontentsize()+" neutral");
		info.add(negative.getcontentsize()+" negative");
		var types=new HashSet<Class<? extends EventCard>>();
		types.addAll(positive.getcontent());
		types.addAll(neutral.getcontent());
		types.addAll(negative.getcontent());
		var cards=new ArrayList<UrbanEvent>(types.size());
		generating=Town.gettowns().get(0);
		for(var t:types)
			try{
				cards.add((UrbanEvent)newinstance(t));
			}catch(ReflectiveOperationException e){
				throw new RuntimeException(e);
			}
		info.add(cards.stream().filter(c->c.traits==null).count()+" basic");
		for(var t:Trait.ALL){
			var valid=cards.stream().filter(c->c.traits!=null&&c.traits.contains(t));
			info.add(valid.count()+" "+t);
		}
		return types.size()+" "+title.toLowerCase()+" ("+String.join(", ",info)+")";
	}
}
