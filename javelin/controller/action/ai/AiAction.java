package javelin.controller.action.ai;

import java.util.List;

import javelin.controller.action.Action;
import javelin.controller.ai.ActionProvider;
import javelin.controller.ai.ChanceNode;
import javelin.model.state.BattleState;
import javelin.model.unit.attack.Combatant;

/**
 * Use this if you want {@link ActionProvider} to use your {@link Action} as a
 * possible AI move.
 */
public interface AiAction {

	/**
	 * Lists the possible results of an action.
	 * @param active
	 *            Current unit.
	 * @param s
	 *            Current battle state.
	 * 
	 * @see BattleState#next
	 */
	List<List<ChanceNode>> getoutcomes(final Combatant active,
			final BattleState s);
}