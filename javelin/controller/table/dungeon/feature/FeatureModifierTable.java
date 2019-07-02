package javelin.controller.table.dungeon.feature;

import javelin.controller.table.Table;
import javelin.old.RPG;

/**
 * @see #rollmodifier()
 * @author alex
 */
public class FeatureModifierTable extends Table{
	enum Modifier{
		VERYEASY,EASY,NORMAL,HARD,VERYHARD,
	}

	/** Constructor. */
	public FeatureModifierTable(){
		add(Modifier.VERYEASY,2);
		add(Modifier.EASY,2);
		add(Modifier.NORMAL,4);
		add(Modifier.HARD,2);
		add(Modifier.VERYHARD,1);
	}

	/**
	 * @return A modifier for DC or d20 roll, ranging from very easy/very hard
	 *         (-1d8 or +1d8), easy/hard (-1d4 or +d1d4) or normal. Roughly equal
	 *         chances of being easier/normal/harder, skewed towards not being
	 *         Very Hard.
	 */
	public int rollmodifier(){
		Object modifier=roll();
		if(modifier==Modifier.VERYEASY) return -RPG.r(1,8);
		if(modifier==Modifier.EASY) return -RPG.r(1,4);
		if(modifier==Modifier.NORMAL) return -0;
		if(modifier==Modifier.HARD) return +RPG.r(1,4);
		return +RPG.r(1,8);
	}
}
