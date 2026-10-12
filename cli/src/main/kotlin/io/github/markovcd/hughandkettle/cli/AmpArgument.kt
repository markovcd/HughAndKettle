package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels

/** An `<amp-id>` argument; a failure lists the ids there are. */
public object AmpArgument {
    public fun parse(text: String): Result<AmpModel> =
        AmpModels.byId(text)?.let { Result.success(it) }
            ?: Result.failure(
                IllegalArgumentException("no amp '$text'; there are ${AmpModels.all.joinToString(", ") { it.id }}"),
            )
}
