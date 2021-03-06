package javelin.controller.terrain.map;

import javelin.view.Images;

/**
 * Empty battle grounds, represents a big empty sports arena.
 * 
 * @author alex
 */
public class Arena extends DndMap {
	/** Constructor. */
	public Arena() {
		super(0, 0, 0);
		floor = Images.getImage("terrainarena");
	}
}