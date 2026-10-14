package io.github.markovcd.hughandkettle.amp

/** The one place that tells the files an import takes apart: a tone file, or the GrandMeister app's preset file. */
public object ToneImport {
    /** Whether [bytes] are the GrandMeister app's preset file rather than a tone file. */
    public fun isPresetFile(bytes: ByteArray): Boolean = BinaryPlist.isOne(bytes)

    public fun read(bytes: ByteArray): Result<TonesRead> =
        if (isPresetFile(bytes)) {
            Gm40Memory.read(bytes)
        } else {
            ToneFile.read(bytes.toString(Charsets.UTF_8)).map { TonesRead(listOf(it.tone), it.skipped) }
        }
}
