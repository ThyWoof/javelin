package javelin.old;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javelin.controller.ai.BattleAi;

public class RPG{
	public static Random rand=new Random();

	public static float random(){
		return rand.nextFloat();
	}

	/**
	 * Random number from zero to s-1
	 *
	 * @param s Upper bound (excluded)
	 * @return
	 */
	public static final int r(final int s){
		if(s<=0) return 0;
		return rand.nextInt(s);
	}

	/**
	 * @return A random number uniformly distributed in [n1, n2] range. It is
	 *         allowed to have so n1 > n2, or n1 < n2, or n1 == n2.
	 */
	public static final int r(final int n1,final int n2){
		return Math.min(n1,n2)+rand.nextInt(Math.max(n1,n2)-Math.min(n1,n2)+1);
	}

	public static int rolldice(final int number,final int sides){
		int total=0;
		for(int i=0;i<number;i++)
			total+=r(1,sides);
		return total;
	}

	public static <K> K pick(final List<K> list){
		return list.get(RPG.r(list.size()));
	}

	public static boolean chancein(int x){
		return RPG.r(1,x)==1;
	}

	/**
	 * Used to return numbers that average 0, useful for adding a small random
	 * factor to things that would otherwise be boring and 100% predictable. For
	 * example, if you input 4, it will return 1d4-1d4, which strongly tends to 0
	 * but could actually return anywhere in the range [-3,+3].
	 *
	 * @param sides Given a die X...
	 * @return the result of 1dX - 1dX.
	 */
	public static int randomize(int sides){
		return r(1,sides)-r(1,sides);
	}

	/**
	 * Useful for handling typical cases in {@link BattleAi}-related logic, since
	 * it only allows for true randomness in a few key points due to performance
	 * considerations. Uses float for parameters since they'd have to be convered
	 * anyways.
	 *
	 * @param dice Given this number of dice...
	 * @param size ...and this number of faces per dice...
	 * @return ... the most likely outcome of this roll.
	 */
	public static int average(float dice,float size){
		size+=1;
		return Math.round(dice*(size/2f));
	}

	/**
	 * @return The same input list, but shuffled with
	 *         {@link Collections#shuffle(List)}.
	 */
	public static <K extends List> K shuffle(K list){
		Collections.shuffle(list);
		return list;
	}
}