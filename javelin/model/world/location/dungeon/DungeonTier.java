package javelin.model.world.location.dungeon;

public class DungeonTier {
	public static final DungeonTier CAVE = new DungeonTier("Cave", 5, 5, 7);
	public static final DungeonTier DUNGEON = new DungeonTier("Dungeon", 10, 5,
			10);
	public static final DungeonTier RUINS = new DungeonTier("Ruins", 15, 10,
			15);
	public static final DungeonTier KEEP = new DungeonTier("Keep", 20, 10, 20);

	public static final DungeonTier HIGHEST = KEEP;
	public static final DungeonTier[] TIERS = new DungeonTier[] { CAVE, DUNGEON,
			RUINS, KEEP, };

	public String name;
	public int level;
	public int minrooms;
	public int maxrooms;

	public DungeonTier(String name, int level, int minrooms, int maxrooms) {
		this.name = name;
		this.minrooms = minrooms;
		this.maxrooms = maxrooms;
		this.level = level;
	}
}