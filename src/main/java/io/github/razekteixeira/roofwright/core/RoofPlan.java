package io.github.razekteixeira.roofwright.core;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The blocks of one roof, before materials are chosen. Blocks are unique per position and sorted bottom
 * up, so placing them in order builds the roof from the eaves to the ridge.
 */
public record RoofPlan(Footprint footprint, RoofSpec spec, List<PlannedBlock> blocks, List<String> notes) {
	public RoofPlan {
		blocks = List.copyOf(blocks);
		notes = List.copyOf(notes);
	}

	public int size() {
		return blocks.size();
	}

	public int minY() {
		return blocks.stream().mapToInt(PlannedBlock::y).min().orElse(footprint.wallTopY());
	}

	public int maxY() {
		return blocks.stream().mapToInt(PlannedBlock::y).max().orElse(footprint.wallTopY());
	}

	public Map<Piece.Kind, Integer> countByKind() {
		Map<Piece.Kind, Integer> counts = new EnumMap<>(Piece.Kind.class);
		for (PlannedBlock block : blocks) {
			counts.merge(block.piece().kind(), 1, Integer::sum);
		}
		return counts;
	}
}
