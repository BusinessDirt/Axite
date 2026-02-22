package github.businessdirt.axite.ecs.collection

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("BitArray Test Suite")
internal class BitArrayTest {

    @TestFactory
    @DisplayName("Creation, Capacity, and Boundary Checks")
    fun capacityAndBoundsTests() = listOf(
        dynamicTest("Empty BitArray initializes with 0 length and capacity") {
            val bits = BitArray(0)
            assertEquals(0, bits.length())
            assertEquals(0, bits.capacity)
        },
        dynamicTest("Set bit at index 2 with sufficient capacity updates correctly") {
            val bits = BitArray(3)
            bits.set(2)
            assertEquals(3, bits.length())
            assertEquals(64, bits.capacity)
            assertTrue(bits[2])
        },
        dynamicTest("Set bit at index 2 with insufficient capacity resizes correctly") {
            val bits = BitArray(0)
            bits.set(2)
            assertEquals(3, bits.length())
            assertEquals(64, bits.capacity)
            assertTrue(bits[2])
        },
        dynamicTest("Accessing an out-of-bounds index returns false") {
            val bits = BitArray(0)
            assertFalse(bits[64])
        }
    )

    @TestFactory
    @DisplayName("Clearing Bits")
    fun clearingBitsTests() = listOf(
        dynamicTest("clearAll() resets the length to 0") {
            val bits = BitArray().apply {
                set(2)
                set(4)
                clearAll()
            }
            assertEquals(0, bits.length())
        },
        dynamicTest("clear(index) correctly unsets a specific bit") {
            val bits = BitArray().apply { set(2) }
            bits.clear(2)
            assertEquals(0, bits.length())
        }
    )

    @TestFactory
    @DisplayName("Relational Operations: Intersects and Contains")
    fun relationalTests() = listOf(
        dynamicTest("intersects() is true when sharing at least one set bit") {
            val bitsA = BitArray(256).apply { set(2); set(4); set(6) }
            val bitsB = BitArray(1).apply { set(4) }

            assertTrue(bitsA.intersects(bitsB))
            assertTrue(bitsB.intersects(bitsA))
        },
        dynamicTest("intersects() is false when no set bits are shared") {
            val bitsA = BitArray(256).apply { set(2); set(4); set(6) }
            val bitsB = BitArray(1).apply { set(3) }

            assertFalse(bitsA.intersects(bitsB))
            assertFalse(bitsB.intersects(bitsA))
        },
        dynamicTest("contains() is true if the target bit array shares the exact same bits") {
            val bitsA = BitArray(256).apply { set(2); set(4) }
            val bitsB = BitArray(1).apply { set(2); set(4) }

            assertTrue(bitsA.contains(bitsB))
            assertTrue(bitsB.contains(bitsA))
        },
        dynamicTest("contains() is false if the target bit array has different bits set") {
            val bitsA = BitArray(256).apply { set(2); set(4) }
            val bitsB = BitArray(1).apply { set(2); set(3) }

            assertFalse(bitsA.contains(bitsB))
            assertFalse(bitsB.contains(bitsA))
        }
    )

    @TestFactory
    @DisplayName("State and Iteration Utilities")
    fun utilityTests() = listOf(
        dynamicTest("forEachSetBit() iterates over set bits in descending order") {
            val bits = BitArray(128).apply {
                set(3)
                set(5)
                set(117)
            }
            var numCalls = 0
            val bitsCalled = mutableListOf<Int>()

            bits.forEachSetBit {
                ++numCalls
                bitsCalled.add(it)
            }

            assertEquals(3, numCalls)
            assertEquals(listOf(117, 5, 3), bitsCalled)
        },
        dynamicTest("numBits() and length() report correct counts") {
            val bits = BitArray().apply { set(4) }
            assertEquals(1, bits.numBits())
            assertEquals(5, bits.length())
        },
        dynamicTest("isEmpty and isNotEmpty toggle correctly when bits are set") {
            val bits = BitArray()
            assertTrue(bits.isEmpty)
            assertFalse(bits.isNotEmpty)

            bits.set(0)
            assertFalse(bits.isEmpty)
            assertTrue(bits.isNotEmpty)
        }
    )

    @TestFactory
    @DisplayName("String Representation")
    fun stringRepresentationTests() = listOf(
        emptyList<Int>() to "0",
        listOf(0) to "1",
        listOf(3) to "0001",
        listOf(100) to "0".repeat(100) + "1",
        listOf(3, 100) to "0".repeat(3) + "1" + "0".repeat(96) + "1"
    ).map { (bitsToSet, expectedOutput) ->
        dynamicTest("BitArray with bits $bitsToSet formats as '$expectedOutput'") {
            val bits = BitArray()
            bitsToSet.forEach { bits.set(it) }
            assertEquals(expectedOutput, bits.toString())
        }
    }

    @TestFactory
    @DisplayName("HashCode resizing boundaries smoke tests")
    fun hashCodeTests() = listOf(
        null, 0, 1, 63, 64, 65, 127, 128, 129
    ).map { bitToSet ->
        val name = if (bitToSet == null) "empty BitArray" else "BitArray with bit $bitToSet set"
        dynamicTest("hashCode() executes without throwing for $name") {
            val bits = BitArray()
            if (bitToSet != null) bits.set(bitToSet)

            // Smoke test: simply invoking to ensure no OutOfBounds/NullPointers occur
            assertDoesNotThrow {
                bits.hashCode()
            }
        }
    }

    @TestFactory
    @DisplayName("isNullOrEmpty Extension Function")
    fun isNullOrEmptyTests() = listOf(
        Pair("Null BitArray", null) to true,
        Pair("Empty BitArray (0 capacity)", BitArray(0)) to true,
        Pair("BitArray with capacity but no bits set", BitArray(128)) to true,
        Pair("BitArray with bit set then cleared", BitArray().apply { set(5); clear(5) }) to true,
        Pair("BitArray with a single bit set", BitArray().apply { set(0) }) to false,
        Pair("BitArray with a high bit set", BitArray().apply { set(100) }) to false
    ).map { (testCase, expectedResult) ->
        val (description, bitArray) = testCase
        dynamicTest("$description returns $expectedResult") {
            assertEquals(expectedResult, bitArray.isNullOrEmpty())
        }
    }

    @TestFactory
    @DisplayName("clearAndForEachSetBit State and Iteration")
    fun clearAndForEachSetBitTests() = listOf(
        emptyList<Int>() to emptyList(),
        listOf(0) to listOf(0),
        listOf(3, 5, 117) to listOf(117, 5, 3),            // Standard descending order
        listOf(63, 64) to listOf(64, 63),                  // Crosses the 64-bit word boundary
        listOf(0, 127, 256) to listOf(256, 127, 0)         // Widely spaced bits across multiple words
    ).map { (bitsToSet, expectedIterationOrder) ->
        dynamicTest("Bits $bitsToSet iterate as $expectedIterationOrder and leave array empty") {
            val bits = BitArray()
            bitsToSet.forEach { bits.set(it) }

            val iteratedBits = mutableListOf<Int>()
            bits.clearAndForEachSetBit {
                iteratedBits.add(it)
            }

            // Verify iteration order
            assertEquals(expectedIterationOrder, iteratedBits)

            // Verify the array is completely cleared
            assertTrue(bits.isEmpty, "BitArray should be empty after clearAndForEachSetBit")
            assertEquals(0, bits.length(), "Length should be 0 after clearing")
            assertEquals(0, bits.numBits(), "numBits should be 0 after clearing")
        }
    }
}