package javelin.model.world.location.dungeon;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javelin.Javelin;
import javelin.JavelinApp;
import javelin.controller.Point;
import javelin.controller.challenge.ChallengeCalculator;
import javelin.controller.challenge.Difficulty;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.comparator.EncountersByEl;
import javelin.controller.exception.GaveUp;
import javelin.controller.fight.Fight;
import javelin.controller.fight.RandomDungeonEncounter;
import javelin.controller.generator.dungeon.DungeonGenerator;
import javelin.controller.generator.dungeon.template.Template;
import javelin.controller.generator.encounter.EncounterGenerator;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.controller.table.RareFeature;
import javelin.controller.table.Table;
import javelin.controller.table.Tables;
import javelin.controller.table.dungeon.DoorExists;
import javelin.controller.table.dungeon.DungeonFeatureModifier;
import javelin.controller.terrain.Terrain;
import javelin.controller.terrain.hazard.Hazard;
import javelin.model.item.key.TempleKey;
import javelin.model.item.key.door.Key;
import javelin.model.item.key.door.MasterKey;
import javelin.model.unit.Combatant;
import javelin.model.unit.Combatants;
import javelin.model.unit.Squad;
import javelin.model.unit.skill.Skill;
import javelin.model.world.Actor;
import javelin.model.world.Incursion;
import javelin.model.world.World;
import javelin.model.world.location.Location;
import javelin.model.world.location.dungeon.feature.Brazier;
import javelin.model.world.location.dungeon.feature.Chest;
import javelin.model.world.location.dungeon.feature.Feature;
import javelin.model.world.location.dungeon.feature.Fountain;
import javelin.model.world.location.dungeon.feature.FruitTree;
import javelin.model.world.location.dungeon.feature.Inhabitant;
import javelin.model.world.location.dungeon.feature.Portal;
import javelin.model.world.location.dungeon.feature.Spirit;
import javelin.model.world.location.dungeon.feature.StairsDown;
import javelin.model.world.location.dungeon.feature.StairsUp;
import javelin.model.world.location.dungeon.feature.Trap;
import javelin.model.world.location.dungeon.feature.door.Door;
import javelin.model.world.location.dungeon.temple.TempleDungeon;
import javelin.view.Images;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.DungeonScreen;
import javelin.view.screen.WorldScreen;
import tyrant.mikera.engine.RPG;

/**
 * A dungeon is an underground area of the world where the combats are harder
 * but have extra treasure laying around.
 *
 * The in-game logic for dungeons is that they are a hideout of bandits or
 * similar, which is why they are sacrificeable by {@link Incursion}s and are
 * removed from the game after a {@link Squad} leaves one (in this case it's
 * assumed the bandits packed their stuff and left).
 *
 * @author alex
 */
public class Dungeon extends Location {
	static final int MAXTRIES = 1000;
	static final int[] DELTAS = { -1, 0, 1 };

	/** Current {@link Dungeon} or <code>null</code> if not in one. */
	public static Dungeon active = null;

	/** All of this dungeon's {@link Feature}s. */
	public Features features = new Features(this);
	/**
	 * Explored squares in this dungeon.
	 *
	 * TODO why is there this and also {@link #discovered}?
	 */
	public boolean[][] visible;
	/**
	 * Current {@link Squad} location.
	 *
	 * TODO is this needed?
	 */
	public Point herolocation;
	/** File to use under 'avatar' folder. */
	public String floor;
	/** File to use under 'avatar' folder. */
	public String wall;
	/** Tiles already revealed. */
	public HashSet<Point> discovered = new HashSet<Point>();
	public char[][] map;
	public int size;
	public int stepsperencounter;
	public int level = -1;
	public boolean doorbackground = true;
	public Tables tables;
	public ArrayList<Combatants> encounters = new ArrayList<Combatants>();

	float ratiomonster = RPG.r(25, 50) / 100f;
	float ratiofeatures = RPG.r(50, 95) / 100f;
	float ratiotraps = RPG.r(10, 25) / 100f;
	float ratiotreasure = RPG.r(5, 10) / 100f;

	Dungeon parent;
	int revealed = 0;

	transient boolean generated = false;

	/** Constructor. */
	public Dungeon(Integer level, Dungeon parent) {
		super("A dungeon");
		this.parent = parent;
		link = false;
		discard = false;
		impermeable = true;
		allowedinscenario = false;
		this.level = level == null ? determineel() : level;
		DungeonTier tier = gettier();
		wall = tier.wall;
		floor = tier.floor;
	}

	public Dungeon() {
		this(null, null);
	}

	protected int determineel() {
		List<Dungeon> dungeons = getdungeons();
		HashSet<Integer> els = new HashSet<Integer>(dungeons.size());
		for (Dungeon d : dungeons) {
			els.add(d.level);
		}
		for (int i = 1; i <= 20; i++) {
			if (!els.contains(i)) {
				return i;
			}
		}
		return RPG.r(1, 20);
	}

	@Override
	public boolean interact() {
		if (Javelin.prompt("You are about to enter: " + describe() + ".\n"
				+ "Press ENTER to continue or any other key to cancel...") != '\n') {
			return true;
		}
		activate(false);
		return true;
	}

	/** Create or recreate dungeon. */
	public void activate(boolean loading) {
		active = this;
		while (features.isEmpty()) {
			Game.messagepanel.clear();
			Game.message("Generating dungeon map...", Delay.NONE);
			map();
		}
		regenerate(loading);
		JavelinApp.context = new DungeonScreen(this);
		BattleScreen.active = JavelinApp.context;
		Squad.active.updateavatar();
		BattleScreen.active.mappanel.center(herolocation.x, herolocation.y,
				true);
		knowfeatures();
	}

	void knowfeatures() {
		int knowledge = Squad.active.getbest(Skill.KNOWLEDGE)
				.taketen(Skill.KNOWLEDGE);
		int reveal = knowledge - (10 + level);
		while (revealed < reveal) {
			revealed += 1;
			Feature f = getundiscoveredfeature();
			if (f != null) {
				discover(f);
			}
		}
	}

	/**
	 * This function generates the dungeon map using {@link DungeonGenerator}
	 * and then {@link #placefeatures()}. One notable thing that happens here is
	 * the determination of how many {@link RandomDungeonEncounter}s should take
	 * for the player to explore the whole level.
	 *
	 * Currently, the calculation is done by setting a goal of one fight per
	 * room on average (so naturally, larger {@link DungeonTier}s will have more
	 * fights than smaller ones). The formula takes into account
	 * {@link DungeonScreen#VIEWRADIUS} instead of counting each step as a
	 * single tile.
	 *
	 * Since a Squad of the dungeon's intended level cannot hope to clear a
	 * dungeon if it's large (in average they can only take 4-5 encounters of
	 * the same EL), this is then offset by placing enough fountains that would
	 * theoretically allow them to do the one dungeon in one go.
	 *
	 * This is currently not counting backtracking out of the dungeon or finding
	 * your way back to town safely, so this naturally makes the dungeon more
	 * challenging (hopefully being offset by the rewards inside).
	 */
	void map() {
		DungeonTier tier = gettier();
		DungeonGenerator generator = DungeonGenerator.generate(tier.minrooms,
				tier.maxrooms, parent == null ? null : parent.tables);
		tables = generator.tables;
		map = generator.grid;
		size = map.length;
		createdoors();
		int nrooms = generator.map.rooms.size();
		calculateencounterfrequency(nrooms);
		generateencounters();
		populatedungeon(nrooms);
		visible = new boolean[size][size];
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				visible[x][y] = false;
			}
		}
	}

	void generateencounters() {
		int target = 3 + RPG.r(1, 4) + gettier().tier;
		if (parent != null) {
			encounters = new ArrayList<Combatants>(parent.encounters);
			encounters.sort(EncountersByEl.INSTANCE);
			int crop = RPG.r(1, 4);
			for (int i = 0; i < crop; i++) {
				encounters.remove(0);
			}
		}
		int attempts = 0;
		int level = this.level;
		List<Terrain> terrains = Arrays
				.asList(new Terrain[] { Terrain.UNDERGROUND });
		while (encounters.size() < target) {
			attempts += 1;
			if (attempts % 100 == 0) {
				level += 1;
			}
			try {
				int el = level + Difficulty.get();
				Combatants encounter = generateencounter(el, terrains);
				if (encounter != null && !encounters.contains(encounter)) {
					encounters.add(encounter);
				}
			} catch (GaveUp e) {
				continue;
			}
		}
	}

	protected Combatants generateencounter(int level, List<Terrain> terrains)
			throws GaveUp {
		return EncounterGenerator.generate(level, terrains);
	}

	void createdoors() {
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				if (map[x][y] == Template.DOOR
						&& gettable(DoorExists.class).rollboolean()) {
					Door d = Door.generate(this, new Point(x, y));
					if (d != null) {
						features.add(d);
					}
				}
			}
		}
	}

	void calculateencounterfrequency(int nrooms) {
		int tilesperroom = countfloor() / nrooms;
		int vision = DungeonScreen.VIEWRADIUS * 2 + 1;
		stepsperencounter = Math.round(ratiomonster * tilesperroom / vision);
	}

	int countfloor() {
		int floortiles = 0;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				char tile = map[x][y];
				if (tile == Template.WALL || tile == Template.DECORATION) {
					floortiles += 1;
				}
			}
		}
		return floortiles;
	}

	protected void populatedungeon(int nrooms) {
		herolocation = findspot();
		createstairs(herolocation);
		int goldpool = createtraps(getfeaturequantity(nrooms, ratiotraps));
		createchests(getfeaturequantity(nrooms, ratiotreasure), goldpool);
		createfeatures(getfeaturequantity(nrooms, ratiofeatures));
	}

	protected void createstairs(Point p) {
		features.add(new StairsUp("stairs up", p));
		for (int x = p.x - 1; x <= p.x + 1; x++) {
			for (int y = p.y - 1; y <= p.y + 1; y++) {
				map[x][y] = Template.FLOOR;
			}
		}
	}

	void createfeatures(int nfeatures) {
		for (int i = 0; i < nfeatures; i++) {
			boolean rare = gettable(RareFeature.class).rollboolean();
			Feature f = createfeature(rare, findspot());
			if (f != null) {
				features.add(f);
			}
		}
	}

	static int getfeaturequantity(int rooms, float ratio) {
		int quantity = Math.round(rooms * ratio);
		return quantity + RPG.randomize(quantity);
	}

	int createtraps(int ntraps) {
		int gold = 0;
		for (int i = 0; i < ntraps; i++) {
			int cr = level + Difficulty.get()
					+ gettable(DungeonFeatureModifier.class).getmodifier();
			if (cr >= Trap.MINIMUMCR) {
				Trap t = new Trap(cr, findspot());
				features.add(t);
				gold += RewardCalculator.getgold(t.cr);
			}
		}
		return gold;
	}

	/**
	 * @return <code>true</code> if given point is between 0 and {@link #SIZE}.
	 */
	public boolean valid(int coordinate) {
		return 0 <= coordinate && coordinate <= size;
	}

	/*
	 * TODO would be cool to generate a flood-map to see which chest is more
	 * distant before deciding which one is the special one. Would probably need
	 * to generate points frist, chests later.
	 */
	void createchests(int nchests, int pool) {
		for (int i = 0; i < nchests; i++) {
			if (RPG.chancein(10)) {
				nchests += 1;
			}
		}
		features.add(createspecialchest(findspot()));
		int hidden = RewardCalculator.getgold(level) * nchests;
		if (pool > 0) {
			nchests += 1;
		} else if (nchests == 0) {
			return;
		}
		pool += hidden;
		for (int i = nchests; i > 0; i--) {
			int gold = i == 1 ? pool : pool / RPG.r(2, i);
			int percentmodifier = gettable(DungeonFeatureModifier.class)
					.getmodifier() * 2;
			gold = gold * (100 + percentmodifier) / 100;
			features.add(new Chest(gold, findspot()));
			pool -= gold;
		}
	}

	/**
	 * TODO at some point rubies shouldn't depend on Haxor anymore
	 *
	 * @param p
	 *            Chest's location.
	 * @return Most special chest here.
	 */
	protected Feature createspecialchest(Point p) {
		Chest c = new Chest(p.x, p.y, World.scenario.templekeys
				? TempleKey.generate() : new MasterKey());
		c.setspecial();
		return c;
	}

	/**
	 * @param freep
	 *            Pick a location from here...
	 * @param used
	 *            as long as it's not being used...
	 * @return and returns it.
	 */
	public Point findspot() {
		Point p = null;
		while (p == null || isoccupied(p)) {
			p = new Point(RPG.r(0, size - 1), RPG.r(0, size - 1));
		}
		return p;
	}

	public boolean isoccupied(Point p) {
		if (map[p.x][p.y] == Template.WALL) {
			return true;
		}
		for (Feature f : features) {
			if (f.x == p.x && f.y == p.y) {
				return true;
			}
		}
		return false;
	}

	/** Exit and destroy this dungeon. */
	public void leave() {
		DungeonScreen.dontenter = true;
		JavelinApp.context = new WorldScreen(true);
		BattleScreen.active = JavelinApp.context;
		Squad.active.place();
		Dungeon.active = null;
		if (World.scenario.expiredungeons && expire()) {
			remove();
		}
	}

	protected boolean expire() {
		for (Feature f : features) {
			Chest c = f instanceof Chest ? (Chest) f : null;
			if (c != null && c.special) {
				return false;
			}
		}
		return true;
	}

	void regenerate(boolean loading) {
		setlocation(loading);
		if (!generated) {
			for (Feature f : features) {
				f.generate();
			}
			generated = true;
		}
	}

	/**
	 * Responsible for deciding where the player should be.
	 *
	 * @param loading
	 *            <code>true</code> if activating this {@link Dungeon} when
	 *            starting the application (loading up a save game with an
	 *            active dungeon).
	 */
	protected void setlocation(boolean loading) {
	}

	@Override
	public Integer getel(int attackel) {
		return attackel - 3;
	}

	/**
	 * Called when reaching {@link StairsUp}
	 */
	public void goup() {
		Dungeon.active.leave();
	}

	/**
	 * Called when reaching {@link StairsDown}.
	 */
	public void godown() {
		// typical dungeons have 1 level only
	}

	/** See {@link WorldScreen#encounter()}. */
	public Fight encounter() {
		return new RandomDungeonEncounter(this);
	}

	/**
	 * Akin to terrain {@link Hazard}s.
	 *
	 * @return <code>true</code> if a hazard happens.
	 */
	public boolean hazard() {
		// no hazards in normal dungeons
		return false;
	}

	/**
	 * Similar to {@link #placefeatures(Set, Set)} but usually reserved to
	 * placing {@link TempleDungeon} {@link Feature}s.
	 *
	 * Called after placing all basic features.
	 *
	 * @return Extra features to be placed using {@link #build(Feature, Set)}.
	 */
	protected Feature createfeature(boolean rare, Point p) {
		LinkedList<Feature> features = new LinkedList<Feature>();
		if (rare) {
			features.add(new Fountain(p.x, p.y));
			features.add(new Inhabitant(p.x, p.y));
		} else {
			features.add(new Chest(p.x, p.y, Key.generate()));
			features.add(new Brazier(p.x, p.y));
			features.add(new FruitTree(p.x, p.y));
			features.add(new Portal(p.x, p.y));
			features.add(new Spirit(p.x, p.y));
		}
		return RPG.pick(features);
	}

	@Override
	public List<Combatant> getcombatants() {
		return null;
	}

	public Feature getfeature(int x, int y) {
		for (Feature f : features) {
			if (f.x == x && f.y == y) {
				return f;
			}
		}
		return null;
	}

	public void setvisible(int x, int y) {
		visible[x][y] = true;
		BattleScreen.active.mappanel.tiles[x][y].discovered = true;
	}

	static public List<Dungeon> getdungeons() {
		ArrayList<Actor> actors = World.getall(Dungeon.class);
		ArrayList<Dungeon> dungeons = new ArrayList<Dungeon>(actors.size());
		for (Actor a : actors) {
			dungeons.add((Dungeon) a);
		}
		return dungeons;
	}

	public DungeonTier gettier() {
		for (DungeonTier t : DungeonTier.TIERS) {
			if (level <= t.level) {
				return t;
			}
		}
		return DungeonTier.TIERS[3];
	}

	@Override
	public String describe() {
		int squadel = ChallengeCalculator.calculateel(Squad.active.members);
		String difficulty = Difficulty.describe(level - squadel);
		return gettier().name + " (" + difficulty + ")";
	}

	@Override
	public Image getimage() {
		return Images.getImage("location" + gettier().name.toLowerCase());
	}

	public void discover(Feature f) {
		setvisible(f.x, f.y);
		f.discover(null, 9000);
	}

	public Feature getundiscoveredfeature() {
		ArrayList<Feature> features = this.features.copy();
		Collections.shuffle(features);
		for (Feature f : features) {
			if (!visible[f.x][f.y] || !f.draw) {
				return f;
			}
		}
		return null;
	}

	public static <K extends Table> K gettable(Class<K> table) {
		return Dungeon.active.tables.get(table);
	}

}
