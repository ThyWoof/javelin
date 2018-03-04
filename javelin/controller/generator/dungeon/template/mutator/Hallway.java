package javelin.controller.generator.dungeon.template.mutator;

import javelin.controller.generator.dungeon.template.Template;

public class Hallway extends Mutator {

	public Hallway() {
		chance = 0.1;
	}

	@Override
	public void apply(Template t) {
		Grow.grow(t, t.width > t.height, 10);
		t.doors += 8;
	}
}