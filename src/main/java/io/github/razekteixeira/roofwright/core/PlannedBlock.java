package io.github.razekteixeira.roofwright.core;

/** A block of a roof plan in world coordinates. */
public record PlannedBlock(int x, int y, int z, Piece piece, Role role) {
	public long key() {
		return key(x, y, z);
	}

	/** Packs a position the same way as Minecraft's {@code BlockPos.asLong}. */
	public static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}

	public PlannedBlock withPiece(Piece value) {
		return new PlannedBlock(x, y, z, value, role);
	}
}
