package io.github.razekteixeira.roofwright;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What one roof changed: every position with the state before and after. Undo and redo read it.
 *
 * @param force whether the roof was placed with {@code force}, so redo asks for the same permission
 */
public record Journal(ResourceKey<Level> level, String label, boolean force, List<Change> changes) {
	public record Change(BlockPos pos, BlockState before, BlockState after) {
	}

	public Journal {
		changes = List.copyOf(changes);
	}
}
