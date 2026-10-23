package io.github.markovcd.hughandkettle.amp

/** The one place that tells the files an import takes apart: a tone file, or the GrandMeister app's preset file. */
public object ToneImport {
    /** The most a file brought in is read to: a tone file is a few kilobytes and a preset file under 32 KB. */
    public const val MOST_BYTES: Int = 64 * 1024

    /** Whether [bytes] are the GrandMeister app's preset file rather than a tone file. */
    public fun isPresetFile(bytes: ByteArray): Boolean = BinaryPlist.isOne(bytes)

    public fun read(bytes: ByteArray): Result<TonesRead> =
        if (isPresetFile(bytes)) {
            Gm40Memory.read(bytes)
        } else {
            ToneFile.read(bytes.toString(Charsets.UTF_8)).map { TonesRead(listOf(it.tone), it.skipped) }
        }
}
