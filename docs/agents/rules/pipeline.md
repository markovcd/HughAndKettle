# The pipeline

## One gate, the same everywhere

CI runs the gate command itself (the one `AGENTS.md` names), not a list of steps that happens to resemble it. Build it as a container image that carries everything the tests need, so a local run, CI and a release all mean the same thing by "passing", and a test cannot skip in CI for want of a tool it has locally.

The stages stack: the publish stage builds on the gate stage, so no artifact can exist unless every test passed. Restores are locked to committed lock files, so a dependency version that moved fails the gate instead of building.

**Why:** two descriptions of what a change has to pass drift apart, and the one that drifts is the one nobody runs locally.

## Workflows are code that holds the keys

- **Pin every action to a commit SHA**, with the version in a comment beside it. A tag is the action author's to move. Let Dependabot or Renovate say when a pin should move.
- **Least permission.** Each workflow declares `permissions:`, read-only by default; write only for the job that publishes.
- **Secrets only where needed.** A secret goes to the one job that uses it, never to a workflow triggered from a fork's pull request.
- **Cancel superseded runs.** `concurrency` with `cancel-in-progress` on anything triggered by a push; never on a deploy or a release, which must finish what it started.
- **Set `timeout-minutes`** on every job, at a few times its usual length. The default is six hours of a hang.
- **Filter deploys on paths**, with a comment saying the list has to grow when the deployed thing gains a dependency.
- **Header comment on every workflow**: what it does, what triggers it, and why anything surprising in it is there.

## A release is one script

Everything but the publishing is a script in the repo (`release.sh`) that runs the same on a machine as in the workflow. Run locally, it builds with a throwaway key and publishes nowhere; in the workflow, the only extra step is publishing what it built. Trying a release is then running a script, not dispatching a workflow and waiting.

One build at a time: a build that empties an output folder takes a lock first, and a second build refuses with a message saying which build holds it and since when.

## A release refuses before moving anything

Before tagging or publishing, check and stop with a reason on the first that fails:

- the tree is not clean, or holds work this session did not do;
- the branch tip is not the commit that would be released;
- the tag already exists;
- the changelog's `Unreleased` section is missing or empty;
- the gate has not passed on that exact commit. **No CI run at all is not a pass**: a commit that was never pushed has nothing red about it;
- a locked restore fails.

The checks are safe to run any time; only the final dispatch publishes, and it waits for the user's go.

## Versions come from tags

The version is the next one after the latest `vX.Y.Z` tag, or one given explicitly; it is passed to the build, never edited into a file. Every other build carries a suffix (`-dev`, and the commit) so it can never pass for a release: it does not self-update, and it is not counted as a release in telemetry. A release run is named for its version, and its notes are that version's section of the changelog.

## What ships is signed

Release artifacts ship with a checksum file, and the checksum file is signed. Anything that installs an update verifies the signature against a public key committed to the repo, before installing. The build checks that the signing key pairs with that public key before building anything, so a bad key fails in seconds rather than after a release nothing can install.

## Measure on a schedule, gate on correctness

Coverage, benchmarks and other slow measurements run on a schedule or on demand, not on every push, and fail on nothing: they are for noticing a change, not for blocking one. The gate fails only on things that are wrong.

## Read the whole log

Capture a build's output to a file and filter it afterwards, never in the pipe. A gate failure is news for the first sentence of the report, with the failing test's name.
