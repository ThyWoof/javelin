package javelin.controller.fight.minigame.battlefield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javelin.Javelin;
import javelin.controller.Point;
import javelin.controller.challenge.ChallengeCalculator;
import javelin.controller.challenge.Difficulty;
import javelin.controller.exception.GaveUp;
import javelin.controller.exception.battle.EndBattle;
import javelin.controller.fight.minigame.Minigame;
import javelin.controller.fight.setup.BattleSetup;
import javelin.controller.map.Map;
import javelin.controller.terrain.Terrain;
import javelin.model.state.BattleState;
import javelin.model.unit.Combatant;
import javelin.old.RPG;
import javelin.old.messagepanel.MessagePanel;
import javelin.view.screen.BattleScreen;

public class Battlefield extends Minigame{
	public static final boolean DEBUG=false;
	public static final int HIGHESTEL=ChallengeCalculator
			.crtoel(RPG.pick(Javelin.MONSTERSBYCR
					.get(Javelin.MONSTERSBYCR.descendingKeySet().first())).cr);

	/** This is used as a come-back mechanic (negative feedback loop). */
	static final float MAXARMY=30;
	static final int CHOICES=3;
	static final List<Terrain> TERRAINS=Arrays.asList(Terrain.NONWATER);
	static final float POINTSPERTURN=1f;
	static final int[] FLAGS=new int[]{1,2,4};

	class BattlefieldSetup extends BattleSetup{
		@Override
		public void place(){
			try{
				int width=map.map.length;
				int height=map.map[0].length;
				int midx=width/2;
				int midy=height/2;
				boolean[] regions=new boolean[]{false,false,false,false};
				HashSet<Integer> blueterrains=new HashSet<>();
				while(blueterrains.size()<2){
					int i=RPG.r(0,3);
					blueterrains.add(i);
					regions[i]=true;
				}
				placeflags(1,1,midx-1,midy-1,regions[0]);
				placeflags(midx+1,1,width-1,midy-1,regions[1]);
				placeflags(1,midy+1,midx-1,height-1,regions[2]);
				placeflags(midx+1,midy+1,width-1,height-1,regions[3]);
				updateflagpoles();
				placesquads(bluequads,blueflagpoles);
				placesquads(redsquads,redflagpoles);
			}catch(GaveUp e){
				map=Map.random();
				generatemap(Battlefield.this);
				place();
			}
		}

		public void placesquads(ArrayList<ArrayList<Combatant>> squads,
				ArrayList<Flagpole> flags){
			for(ArrayList<Combatant> squad:squads){
				Flagpole flag=RPG.pick(flags);
				for(Combatant c:squad)
					placeunit(state.clone(c),flag);
			}
		}

		void placeflags(int fromx,int fromy,int tox,int toy,boolean blueteam)
				throws GaveUp{
			int flagpoles=FLAGS[RPG.r(FLAGS.length)];
			int placed=0;
			int tries=0;
			placing:while(placed<flagpoles){
				tries+=1;
				if(tries>1000) throw new GaveUp();
				Point spot=new Point(RPG.r(fromx,tox),RPG.r(fromy,toy));
				if(checkblocked(spot)) continue placing;
				for(Point p:Point.getadjacent()){
					p.x+=spot.x;
					p.y+=spot.y;
					if(checkblocked(p)) continue placing;
				}
				Flagpole flag=new Flagpole(Battlefield.this,4/flagpoles,blueteam);
				flag.setlocation(spot);
				if(blueteam)
					state.blueTeam.add(flag);
				else
					state.redTeam.add(flag);
				placed+=1;
			}
		}
	}

	public ArrayList<ArrayList<Combatant>> bluequads=new ArrayList<>();
	public ArrayList<ArrayList<Combatant>> redsquads=new ArrayList<>();

	public ArrayList<Combatant> bluearmy=new ArrayList<>();
	public ArrayList<Combatant> redarmy=new ArrayList<>();

	public ArrayList<Flagpole> blueflagpoles=new ArrayList<>();
	public ArrayList<Flagpole> redflagpoles=new ArrayList<>();

	public ArrayList<Combatant> redcommanders=new ArrayList<>();
	public ArrayList<Combatant> redelites=new ArrayList<>();
	public ArrayList<Combatant> redfootsoliders=new ArrayList<>();

	float lastupdate=Float.MIN_VALUE;
	float redpoints=0;
	float bluepoints=0;

	public Battlefield(){
		setup=new BattlefieldSetup();
	}

	@Override
	public ArrayList<Combatant> getfoes(Integer teamel){
		return redarmy;
	}

	@Override
	public ArrayList<Combatant> getblueteam(){
		return bluearmy;
	}

	@Override
	public void startturn(Combatant acting){
		super.startturn(acting);
		if(lastupdate==Float.MIN_VALUE)
			lastupdate=acting.ap;
		else if(acting.ap<lastupdate+.5) return;
		updateflagpoles();
		bluepoints+=updatepoints(acting.ap-lastupdate,blueflagpoles,state.blueTeam);
		redpoints+=updatepoints(acting.ap-lastupdate,redflagpoles,state.redTeam);
		lastupdate=acting.ap;
		int elred=calculateteammel(state.redTeam,redflagpoles);
		int elblue=calculateteammel(state.blueTeam,blueflagpoles);
		float advantage=elblue+bluepoints/2-elred-redpoints/2;
		System.out.println();
		if(advantage>=Math.abs(Difficulty.VERYEASY))
			surrender(state.redTeam);
		else if(advantage<=Difficulty.VERYEASY) surrender(state.blueTeam);
		if(!redflagpoles.isEmpty()&&elred<elblue&&elred+redpoints>=elblue){
			ArrayList<Combatant> units=reinforceenemy();
			Javelin.redraw();
			Javelin.message(
					"The enemy calls for reinforcements:\n"+Combatant.group(units)+"!\n",
					true);
			MessagePanel.active.clear();
			MessagePanel.active.repaint();
		}
	}

	void surrender(ArrayList<Combatant> team){
		team.clear();
		MessagePanel.active.clear();
		String army=team==state.blueTeam?"Your":"The enemy";
		Javelin.message(army+" army disbands and flees in defeat!",true);
		throw new EndBattle();
	}

	@Override
	public void withdraw(Combatant combatant,BattleScreen screen){
		try{
			super.withdraw(combatant,screen);
		}catch(EndBattle e){
			state.blueTeam.clear();
		}
	}

	int calculateteammel(ArrayList<Combatant> team,ArrayList<Flagpole> flagpoles){
		ArrayList<Combatant> clean=new ArrayList<>(team.size()-flagpoles.size());
		for(Combatant c:team)
			if(!(c instanceof Flagpole)) clean.add(c);
		return ChallengeCalculator.calculateel(clean);
	}

	ArrayList<Combatant> reinforceenemy(){
		updateredarmy(redcommanders);
		updateredarmy(redelites);
		updateredarmy(redfootsoliders);
		float el=redpoints;
		ArrayList<Combatant> selection=recruitredsquad(el);
		state.redTeam.addAll(selection);
		redpoints-=ChallengeCalculator.calculateel(selection);
		for(Combatant c:selection)
			placeunit(c,RPG.pick(redflagpoles));
		return selection;
	}

	public ArrayList<Combatant> recruitredsquad(float el){
		int elcommander=ChallengeCalculator.calculateel(redcommanders);
		int elelites=ChallengeCalculator.calculateel(redelites);
		int elfootsolider=ChallengeCalculator.calculateel(redfootsoliders);
		int lowel=Math.min(elcommander,Math.min(elelites,elfootsolider));
		Reinforcement r=new Reinforcement(el,TERRAINS);
		ArrayList<ArrayList<Combatant>> choices=new ArrayList<>();
		if(elcommander==lowel) choices.add(r.commander);
		if(elelites==lowel) choices.add(r.elites);
		if(elfootsolider==lowel) choices.add(r.footsoldiers);
		ArrayList<Combatant> choice=RPG.pick(choices);
		if(choice==r.commander)
			redcommanders.addAll(choice);
		else if(choice==r.elites)
			redelites.addAll(choice);
		else
			redfootsoliders.addAll(choice);
		return choice;
	}

	float updatepoints(float ap,ArrayList<Flagpole> flagpoles,
			ArrayList<Combatant> team){
		float points=0;
		for(Flagpole f:flagpoles)
			points+=f.rank*POINTSPERTURN*ap*(f.hp/new Float(f.maxhp));
		float upkeep=getupkeep(team,flagpoles);
		return points*upkeep;
	}

	float getupkeep(ArrayList<Combatant> team,ArrayList<Flagpole> flagpoles){
		team=(ArrayList<Combatant>)team.clone();
		team.removeAll(flagpoles);
		return 1-ChallengeCalculator.calculateel(team)/MAXARMY;
	}

	void updateredarmy(ArrayList<Combatant> tier){
		for(Combatant c:new ArrayList<>(tier))
			if(!state.redTeam.contains(c)) tier.remove(c);
	}

	@Override
	public void die(Combatant c,BattleState s){
		if(!(c instanceof Flagpole)) super.die(c,s);
	}

	void recruitbluearmy(){
		MessagePanel.active.clear();
		if(bluepoints<1){
			Javelin.message("You don't have army points yet...",Javelin.Delay.WAIT);
			return;
		}
		String prompt="You have "+Math.round(bluepoints)
				+" army points. Which unit(s) do you want to reinforce with?\n"
				+"(Keep in mind that recruiting once with more army points is better than recruiting many times with fewer points.)";
		Reinforcement r=new Reinforcement(bluepoints,TERRAINS);
		ArrayList<Combatant> units=recruitbluesquad(prompt,r,true);
		bluepoints-=ChallengeCalculator.calculateel(units);
		state.blueTeam.addAll(units);
		for(Combatant c:units){
			placeunit(c,RPG.pick(blueflagpoles));
			c.setmercenary(false);
		}
		Javelin.app.switchScreen(BattleScreen.active);
	}

	static public ArrayList<Combatant> recruitbluesquad(String prompt,
			Reinforcement r,boolean forceselection){
		ArrayList<String> choices=new ArrayList<>(3);
		choices.add(group(r.commander));
		choices.add(group(r.elites));
		choices.add(group(r.footsoldiers));
		int choice=Javelin.choose(prompt,choices,true,forceselection);
		if(choice<0) return null;
		if(choice==0) return r.commander;
		if(choice==1) return r.elites;
		return r.footsoldiers;
	}

	static String group(ArrayList<Combatant> group){
		String s=Combatant.group(group);
		if(Javelin.DEBUG) s+=" (EL"+ChallengeCalculator.calculateel(group)+")";
		return s;
	}

	void updateflagpoles(){
		blueflagpoles.clear();
		redflagpoles.clear();
		for(Combatant c:state.getcombatants()){
			Flagpole f=c instanceof Flagpole?(Flagpole)c:null;
			if(f!=null) if(f.blueteam)
				blueflagpoles.add(f);
			else
				redflagpoles.add(f);
		}
	}

	void placeunit(Combatant c,Flagpole f){
		Point p=new Point(f);
		while(checkblocked(p)){
			p.x+=RPG.randomize(3);
			p.y+=RPG.randomize(3);
			if(!p.validate(0,0,map.map.length,map.map[0].length)) p=new Point(f);
		}
		c.setlocation(p);
		c.initialap=lastupdate==Float.MIN_VALUE?0:lastupdate;
		c.ap=c.initialap;
		c.rollinitiative();
	}

	public boolean checkblocked(Point p){
		return !p.validate(0,0,map.map.length,map.map[0].length)
				||map.map[p.x][p.y].blocked||state.getcombatant(p.x,p.y)!=null;
	}

	@Override
	public void checkend(){
		updateflagpoles();
		ArrayList<Combatant> blueteam=(ArrayList<Combatant>)state.blueTeam.clone();
		ArrayList<Combatant> redteam=(ArrayList<Combatant>)state.redTeam.clone();
		blueteam.removeAll(blueflagpoles);
		redteam.removeAll(redflagpoles);
		if(blueteam.isEmpty()||redteam.isEmpty()) throw new EndBattle();
	}

	@Override
	public boolean onend(){
		state.blueTeam.removeAll(blueflagpoles);
		state.redTeam.removeAll(redflagpoles);
		String message;
		if(state.blueTeam.isEmpty())
			message="You've lost this match... Better luck next time!\n"
					+"Press any key to continue...";
		else
			message="Congratulations, you've won!\n"
					+"Your surviving units will be available for hire at the Battlefiled location.\n"
					+"Press any key to continue...";
		Javelin.prompt(message);
		return super.onend();
	}

	@Override
	public boolean start(){
		return super.start()&&new ArmySelectionScreen().selectarmy(this);
	}
}