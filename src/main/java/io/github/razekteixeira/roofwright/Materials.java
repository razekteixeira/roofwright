package io.github.razekteixeira.roofwright;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.GameMasterBlock;
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
 * other mods), whose slab and full block are found by name. Every block is checked by {@link #problem}
 * first, so a roof can never be made of portals, containers, fluids, unbreakable or operator blocks.
 */
public record Materials(Block stairs, Block slab, Block full, @Nullable Block wall) {
	public static final Materials DEFAULT = new Materials(
			net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS,
			net.minecraft.world.level.block.Blocks.SPRUCE_SLAB,
			net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS,
			null);

	/**
	 * Blocks a roof may never be made of, on top of the built-in rules. Server owners extend it with a data
	 * pack ({@code data/roofwright/tags/block/forbidden.json}).
	 */
	public static final TagKey<Block> FORBIDDEN = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Roofwright.MOD_ID, "forbidden"));

	/**
	 * Why a block state may not be part of a roof, or {@code null} when it may. Stairs and slabs are checked
	 * with {@code fullBlock} false; full blocks and gable walls must also be a full cube.
	 */
	public static @Nullable String problem(BlockState state, boolean fullBlock) {
		String id = id(state.getBlock()).toString();
		if (state.isAir()) {
			return id + " is air";
		}
		if (state.hasBlockEntity()) {
			return id + " holds data (a container, sign or spawner) and cannot be part of a roof";
		}
		if (!state.getFluidState().isEmpty()) {
			return id + " is or holds a fluid";
		}
		if (state.getBlock().defaultDestroyTime() < 0 || state.getBlock() instanceof GameMasterBlock) {
			return id + " is an unbreakable or operator block";
		}
		if (state.getBlock() instanceof FallingBlock) {
			return id + " would fall";
		}
		if (state.is(FORBIDDEN)) {
			return id + " is not allowed in roofs (#" + FORBIDDEN.location() + ")";
		}
		if (fullBlock && !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
			return id + " is not a full block";
		}
		return null;
	}

	/** The first problem among these materials, or {@code null}. */
	public @Nullable String problem() {
		for (Block block : wall == null ? List.of(stairs, slab) : List.of(stairs, slab, wall)) {
			String problem = problem(block.defaultBlockState(), false);
			if (problem != null) {
				return problem;
			}
		}
		return problem(full.defaultBlockState(), true);
	}

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
		// A block can be in several families (stone bricks are the base of their own and the "polished" variant
		// of stone), and the family order is not stable. Prefer the family the block is the base of, then one
		// where it is the stairs, slab or wall, then any other; ties go to the base block's id.
		Optional<BlockFamily> family = BlockFamilies.getAllFamilies()
				.filter(f -> f.getBaseBlock() == block || f.getVariants().containsValue(block))
				.filter(f -> f.get(BlockFamily.Variant.STAIRS) != null && f.get(BlockFamily.Variant.SLAB) != null)
				.min(java.util.Comparator.comparingInt((BlockFamily f) -> familyRank(f, block))
						.thenComparing(f -> id(f.getBaseBlock()).toString()));
		if (family.isPresent()) {
			BlockFamily f = family.get();
			return Result.checked(new Materials(f.get(BlockFamily.Variant.STAIRS), f.get(BlockFamily.Variant.SLAB), f.getBaseBlock(),
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
		return Result.checked(new Materials(block, slab, full, null));
	}

	private static int familyRank(BlockFamily family, Block block) {
		if (family.getBaseBlock() == block) {
			return 0;
		}
		return block == family.get(BlockFamily.Variant.STAIRS) || block == family.get(BlockFamily.Variant.SLAB)
				|| block == family.get(BlockFamily.Variant.WALL) ? 1 : 2;
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
		return Result.checked(new Materials(stairs, slab, full, null));
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
		/** The materials, or the first {@link Materials#problem} with them. */
		static Result checked(Materials materials) {
			String problem = materials.problem();
			return problem == null ? new Result(materials, null) : error(problem);
		}

		static Result error(String message) {
			return new Result(null, message);
		}
	}
}
