package io.github.markovcd.hughandkettle.amp

/**
 * Reads Apple's binary property list, `bplist00`, as [H&K's iPad apps][Gm40Memory] save it. An object
 * comes back as a `Map<String, Any?>`, `List<Any?>`, `String`, `Long`, `Double`, `Boolean`, `ByteArray`,
 * [PlistUid] or null. Every offset and length is checked against the file before it is followed.
 */
internal object BinaryPlist {
    private const val MAGIC = "bplist00"
    private const val TRAILER_BYTES = 32

    // A keyed archive nests a few levels; anything deeper is not one.
    private const val MAX_DEPTH = 32

    fun isOne(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.length && String(bytes, 0, MAGIC.length, Charsets.US_ASCII) == MAGIC

    fun read(bytes: ByteArray): Result<Any?> {
        if (!isOne(bytes) || bytes.size < MAGIC.length + TRAILER_BYTES) return failure("is not a binary property list")
        val trailer = bytes.size - TRAILER_BYTES
        val offsetSize = bytes[trailer + 6].toInt() and 0xFF
        val refSize = bytes[trailer + 7].toInt() and 0xFF
        val count = number(bytes, trailer + 8, 8)
        val top = number(bytes, trailer + 16, 8)
        val table = number(bytes, trailer + 24, 8)
        if (offsetSize !in 1..8 || refSize !in 1..8) return failure("has references of $offsetSize and $refSize bytes")
        if (count !in 1..bytes.size.toLong() || top !in 0 until count) return failure("has a broken trailer")
        if (table !in MAGIC.length..trailer.toLong() - count * offsetSize) return failure("has its offsets outside it")
        return Reader(bytes, offsetSize, refSize, count.toInt(), table.toInt()).run {
            try {
                Result.success(objectAt(top.toInt(), 0))
            } catch (e: Broken) {
                failure(e.message!!)
            }
        }
    }

    private fun failure(why: String): Result<Any?> = Result.failure(IllegalArgumentException("the file $why"))

    /** The unsigned big-endian number of [size] bytes at [at]; the caller has checked they are there. */
    private fun number(
        bytes: ByteArray,
        at: Int,
        size: Int,
    ): Long = (0 until size).fold(0L) { sum, i -> (sum shl 8) or (bytes[at + i].toLong() and 0xFF) }

    private class Broken(
        why: String,
    ) : Exception(why)

    private class Reader(
        val bytes: ByteArray,
        val offsetSize: Int,
        val refSize: Int,
        val count: Int,
        val table: Int,
    ) {
        private val done = HashMap<Int, Any?>()
        private val reading = HashSet<Int>()

        fun objectAt(
            index: Int,
            depth: Int,
        ): Any? {
            if (index !in 0 until count) throw Broken("refers to object $index of $count")
            if (index in done) return done[index]
            if (depth > MAX_DEPTH) throw Broken("nests deeper than $MAX_DEPTH")
            if (!reading.add(index)) throw Broken("has object $index inside itself")
            val offset = number(bytes, table + index * offsetSize, offsetSize)
            if (offset !in MAGIC.length until table) throw Broken("puts object $index at $offset, outside it")
            val value = parse(offset.toInt(), depth)
            reading -= index
            done[index] = value
            return value
        }

        private fun parse(
            at: Int,
            depth: Int,
        ): Any? {
            val marker = bytes[at].toInt() and 0xFF
            val info = marker and 0x0F
            return when (marker shr 4) {
                0x0 -> {
                    when (info) {
                        0x0 -> null
                        0x8 -> false
                        0x9 -> true
                        else -> throw Broken("has an unknown marker ${hex(marker)} at $at")
                    }
                }

                0x1 -> {
                    if (info > 3) throw Broken("has a ${1 shl info}-byte integer at $at")
                    number(need(at + 1, 1 shl info), at + 1, 1 shl info)
                }

                0x2 -> {
                    when (info) {
                        2 -> Float.fromBits(number(need(at + 1, 4), at + 1, 4).toInt()).toDouble()
                        3 -> Double.fromBits(number(need(at + 1, 8), at + 1, 8))
                        else -> throw Broken("has a ${1 shl info}-byte real at $at")
                    }
                }

                0x3 -> {
                    Double.fromBits(number(need(at + 1, 8), at + 1, 8))
                }

                0x4 -> {
                    val (length, start) = length(at, info)
                    need(start, length).copyOfRange(start, start + length)
                }

                0x5 -> {
                    val (length, start) = length(at, info)
                    String(need(start, length), start, length, Charsets.US_ASCII)
                }

                0x6 -> {
                    val (length, start) = length(at, info)
                    String(need(start, length * 2L), start, length * 2, Charsets.UTF_16BE)
                }

                0x8 -> {
                    PlistUid(number(need(at + 1, info + 1), at + 1, info + 1).toIntExact(at))
                }

                0xA, 0xC -> {
                    val (length, start) = length(at, info)
                    refs(start, length).map { objectAt(it, depth + 1) }
                }

                0xD -> {
                    val (length, start) = length(at, info)
                    val keys = refs(start, length)
                    val values = refs(start + length * refSize, length)
                    keys.zip(values).associate { (key, value) ->
                        val name =
                            objectAt(key, depth + 1) as? String ?: throw Broken("has a key that is not text at $at")
                        name to objectAt(value, depth + 1)
                    }
                }

                else -> {
                    throw Broken("has an unknown marker ${hex(marker)} at $at")
                }
            }
        }

        /** How many items the object at [at] holds, and where they start: in [info], or in an integer after it. */
        private fun length(
            at: Int,
            info: Int,
        ): Pair<Int, Int> {
            if (info != 0x0F) return info to at + 1
            val marker = need(at + 1, 1)[at + 1].toInt() and 0xFF
            val size = 1 shl (marker and 0x0F)
            if (marker shr 4 != 0x1 || size > 4) throw Broken("has a broken length at $at")
            val length = number(need(at + 2, size), at + 2, size).toIntExact(at)
            return length to at + 2 + size
        }

        private fun refs(
            at: Int,
            length: Int,
        ): List<Int> {
            need(at, length.toLong() * refSize)
            return List(length) { number(bytes, at + it * refSize, refSize).toIntExact(at) }
        }

        /** The bytes, once [size] of them from [at] are known to lie before the offset table. */
        private fun need(
            at: Int,
            size: Int,
        ): ByteArray = need(at, size.toLong())

        private fun need(
            at: Int,
            size: Long,
        ): ByteArray {
            if (size < 0 || at + size > table) throw Broken("runs past its end at $at")
            return bytes
        }

        private fun Long.toIntExact(at: Int): Int =
            if (this in
                0..Int.MAX_VALUE
            ) {
                toInt()
            } else {
                throw Broken("has $this at $at")
            }

        private fun hex(marker: Int) = "%02X".format(marker)
    }
}
