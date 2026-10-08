package io.github.razekteixeira.roofwright;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import io.github.razekteixeira.roofwright.core.Piece;
import io.github.razekteixeira.roofwright.core.StairShape;

/**
 * The blocks a roof is made of: stairs, a slab, a full block and (for parapets) a wall. Resolved from
 * any one member of a vanilla block family, or from any block that behaves like stairs (Macaw's Roofs and
 * other mods), whose slab and full block are found by name.
 */
public record Materials(Block stairs, Block slab, Block full, @Nullable Block wall) {
	public static final Materials DEFAULT = new Materials(
			net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS,
			net.minecraft.world.level.block.Blocks.SPRUCE_SLAB,
			net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS,
			null);

	/** A short name for chat: the stairs' id, without the namespace for vanilla blocks. */
	public String name() {
		Identifier id = id(stairs);
		return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
	}

	public static Identifier id(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}

	/** True when the block has the state properties of stairs, whatever its class. */
	public static boolean isStairLike(Block block) {
		BlockState state = block.defaultBlockState();
		return state.hasProperty(StairBlock.FACING) && state.hasProperty(StairBlock.HALF) && state.hasProperty(StairBlock.SHAPE);
	}

	public static boolean isSlabLike(Block block) {
		return block.defaultBlockState().hasProperty(SlabBlock.TYPE);
	}

	/** Resolves materials from one block, or explains why it cannot. */
	public static Result resolve(Block block) {
		Optional<BlockFamily> family = BlockFamilies.getAllFamilies()
				.filter(f -> f.getBaseBlock() == block || f.getVariants().containsValue(block))
				.filter(f -> f.get(BlockFamily.Variant.STAIRS) != null && f.get(BlockFamily.Variant.SLAB) != null)
				.findFirst();
		if (family.isPresent()) {
			BlockFamily f = family.get();
			return Result.ok(new Materials(f.get(BlockFamily.Variant.STAIRS), f.get(BlockFamily.Variant.SLAB), f.getBaseBlock(),
					f.get(BlockFamily.Variant.WALL)));
		}
		if (!isStairLike(block)) {
			return Result.error(id(block) + " has no stairs and slab to build a roof from. Use stairs, a slab or a block like oak_planks or stone_bricks.");
		}
		// A modded stair: find its slab and full block by name in its own namespace, then in vanilla's.
		Identifier id = id(block);
		String stem = id.getPath().replaceAll("_(stairs|stair|roof|roofs)$", "");
		Block slab = firstExisting(id.getNamespace(), List.of(stem + "_slab", stem + "_slabs"), Materials::isSlabLike);
		Block full = firstExisting(id.getNamespace(), List.of(stem, stem + "s", stem + "_planks", stem + "_block"), b -> !isStairLike(b) && !isSlabLike(b));
		if (slab == null || full == null) {
			return Result.error("Found the stairs " + id + " but not a matching " + (slab == null ? "slab" : "full block")
					+ ". Give all three: /roof material " + id + " <slab> <full block>");
		}
		return Result.ok(new Materials(block, slab, full, null));
	}

	/** Explicit stairs, slab and full block, each checked for its kind. */
	public static Result explicit(Block stairs, Block slab, Block full) {
		if (!isStairLike(stairs)) {
			return Result.error(id(stairs) + " is not stairs");
		}
		if (!isSlabLike(slab)) {
			return Result.error(id(slab) + " is not a slab");
		}
		if (isStairLike(full) || isSlabLike(full) || full.defaultBlockState().isAir()) {
			return Result.error(id(full) + " is not a full block");
		}
		return Result.ok(new Materials(stairs, slab, full, null));
	}

	private static @Nullable Block firstExisting(String namespace, List<String> paths, java.util.function.Predicate<Block> kind) {
		for (String ns : List.of(namespace, "minecraft")) {
			for (String path : paths) {
				Identifier candidate = Identifier.tryParse(ns + ":" + path);
				if (candidate != null) {
					Optional<Block> found = BuiltInRegistries.BLOCK.getOptional(candidate);
					if (found.isPresent() && kind.test(found.get()) && !found.get().defaultBlockState().isAir()) {
						return found.get();
					}
				}
			}
		}
		return null;
	}

	/** The block state for a planned piece in this material. */
	public BlockState state(Piece piece) {
		return switch (piece.kind()) {
			case STAIR -> {
				BlockState state = stairs.defaultBlockState()
						.setValue(StairBlock.FACING, Directions.of(piece.facing()))
						.setValue(StairBlock.HALF, Half.BOTTOM)
						.setValue(StairBlock.SHAPE, shape(piece.shape()));
				yield state.hasProperty(StairBlock.WATERLOGGED) ? state.setValue(StairBlock.WATERLOGGED, false) : state;
			}
			case BOTTOM_SLAB -> slab(SlabType.BOTTOM);
			case TOP_SLAB -> slab(SlabType.TOP);
			case FULL -> full.defaultBlockState();
			case WALL -> wall != null ? wall.defaultBlockState() : full.defaultBlockState();
		};
	}

	private BlockState slab(SlabType type) {
		BlockState state = slab.defaultBlockState().setValue(SlabBlock.TYPE, type);
		return state.hasProperty(SlabBlock.WATERLOGGED) ? state.setValue(SlabBlock.WATERLOGGED, false) : state;
	}

	private static StairsShape shape(StairShape shape) {
		return switch (shape) {
			case STRAIGHT -> StairsShape.STRAIGHT;
			case INNER_LEFT -> StairsShape.INNER_LEFT;
			case INNER_RIGHT -> StairsShape.INNER_RIGHT;
			case OUTER_LEFT -> StairsShape.OUTER_LEFT;
			case OUTER_RIGHT -> StairsShape.OUTER_RIGHT;
		};
	}

	/** Either materials or a message for chat. */
	public record Result(@Nullable Materials materials, @Nullable String error) {
		static Result ok(Materials materials) {
			return new Result(materials, null);
		}

		static Result error(String message) {
			return new Result(null, message);
		}
	}
}
