package io.github.paulikauro.redstonetools

import io.github.paulikauro.redstonetools.BlockSet.Companion.X_CHUNK_BITS
import io.github.paulikauro.redstonetools.BlockSet.Companion.X_OFFSET_BITS
import io.github.paulikauro.redstonetools.BlockSet.Companion.Y_CHUNK_BITS
import io.github.paulikauro.redstonetools.BlockSet.Companion.Y_OFFSET_BITS
import io.github.paulikauro.redstonetools.BlockSet.Companion.Z_CHUNK_BITS
import io.github.paulikauro.redstonetools.BlockSet.Companion.Z_OFFSET_BITS
import org.junit.jupiter.api.Test

// calculated based on the constants in BlockSet
private const val CHUNK_BITS = X_CHUNK_BITS + Z_CHUNK_BITS + Y_CHUNK_BITS
private const val X_BITS = X_OFFSET_BITS + X_CHUNK_BITS
private const val Z_BITS = Z_OFFSET_BITS + Z_CHUNK_BITS
private const val Y_BITS = Y_OFFSET_BITS + Y_CHUNK_BITS

// A range of integers (inclusive) [a, b] requires ceil(log2(b - a + 1)) bits to store.
// The x and z coordinates, assuming range -30_000_000 to 30_000_000, require at least 26 bits.
private const val XZ_MIN_BITS = 26

// The y coordinate, assuming range -64 to 319, requires 9 bits only.
// (There are less than 2^9 = 512 values. It doesn't matter that the higher bits get truncated.)
private const val Y_MIN_BITS = 9

class BlockSetTests {
    @Test
    fun `bit sizes ok`() {
        assert(CHUNK_BITS <= Long.SIZE_BITS)
        assert(X_BITS >= XZ_MIN_BITS)
        assert(Z_BITS >= XZ_MIN_BITS)
        assert(Y_BITS >= Y_MIN_BITS)
    }
}
