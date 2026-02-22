package github.businessdirt.axite.ecs.collection

class BitArray(nBits: Int = 0) {
    @PublishedApi
    internal var bits = LongArray((nBits + 63) ushr 6)

    val capacity: Int
        get() = bits.size shl 6

    val isNotEmpty: Boolean
        get() = bits.any { it != 0L }

    val isEmpty: Boolean
        get() = bits.all { it == 0L }

    operator fun get(idx: Int): Boolean {
        val word = idx ushr 6
        return word < bits.size && (bits[word] and (1L shl idx)) != 0L
    }

    fun set(idx: Int) {
        val word = idx ushr 6
        if (word >= bits.size) {
            // Idiomatically double the capacity, or explicitly size to fit the requested word
            bits = bits.copyOf(maxOf(bits.size * 2, word + 1))
        }
        bits[word] = bits[word] or (1L shl idx)
    }

    fun clearAll() {
        bits.fill(0L)
    }

    fun clear(idx: Int) {
        val word = idx ushr 6
        if (word < bits.size) {
            bits[word] = bits[word] and (1L shl idx).inv()
        }
    }

    fun intersects(other: BitArray): Boolean {
        val limit = minOf(bits.size, other.bits.size)
        return (0 until limit).any { (bits[it] and other.bits[it]) != 0L }
    }

    fun contains(other: BitArray): Boolean {
        // First check if 'other' has non-zero bits outside our array capacity
        if (other.bits.size > bits.size && (bits.size until other.bits.size).any { other.bits[it] != 0L }) {
            return false
        }

        // Check overlapping bits
        val limit = minOf(bits.size, other.bits.size)
        return (0 until limit).all { (bits[it] and other.bits[it]) == other.bits[it] }
    }

    fun length(): Int {
        val lastWordIndex = bits.indexOfLast { it != 0L }
        if (lastWordIndex == -1) return 0
        return (lastWordIndex shl 6) + 64 - bits[lastWordIndex].countLeadingZeroBits()
    }

    fun numBits(): Int = bits.sumOf { it.countOneBits() }

    inline fun forEachSetBit(action: (Int) -> Unit) {
        for (word in bits.indices.reversed()) {
            var currentWord = bits[word]
            val offset = word shl 6
            while (currentWord != 0L) {
                val bitIndex = 63 - currentWord.countLeadingZeroBits()
                action(offset + bitIndex)
                currentWord = currentWord xor (1L shl bitIndex)
            }
        }
    }

    inline fun clearAndForEachSetBit(action: (Int) -> Unit) {
        for (word in bits.indices.reversed()) {
            var currentWord = bits[word]
            if (currentWord != 0L) {
                bits[word] = 0L // Clear it once outside the while loop
                val offset = word shl 6
                while (currentWord != 0L) {
                    val bitIndex = 63 - currentWord.countLeadingZeroBits()
                    action(offset + bitIndex)
                    currentWord = currentWord xor (1L shl bitIndex)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BitArray) return false

        val limit = minOf(bits.size, other.bits.size)
        for (i in 0 until limit) {
            if (bits[i] != other.bits[i]) return false
        }

        // Ensure remaining words in whichever array is larger are entirely zero
        return if (bits.size > limit) {
            (limit until bits.size).all { bits[it] == 0L }
        } else {
            (limit until other.bits.size).all { other.bits[it] == 0L }
        }
    }

    override fun hashCode(): Int {
        var result = 1

        // Find the last non-zero word. Returns -1 if empty or all zeros.
        val lastNonZero = bits.indexOfLast { it != 0L }

        // If lastNonZero is -1, the range (-1 downTo 0) is empty and the loop is skipped.
        for (i in lastNonZero downTo 0) {
            val word = bits[i]
            result = 31 * result + (word xor (word ushr 32)).toInt()
        }

        return result
    }

    override fun toString(): String = when {
        isEmpty -> "0"
        else -> buildString(length()) {
            for (word in bits) {
                for (i in 0 until 64) {
                    append(if ((word and (1L shl i)) != 0L) '1' else '0')
                }
            }
        }.trimEnd('0')
    }
}

fun BitArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty