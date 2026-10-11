# Continuous integration and delivery

Continuous integration is a practice, not a server: everyone lands small changes on the mainline often, every change is verified by the full build and tests, and a broken mainline is fixed before anything else. Continuous delivery follows from it: the mainline is always releasable, so a release is a decision and a button, not a project. The pipeline that checks it is in `pipeline.md`; this is the discipline it serves.

## Integrate small and often

- Land on the mainline at least once a day. Work that lives on a branch for days integrates with a mainline that has moved, and the merge is where the bugs are.
- Each commit is one small, whole step that builds and passes on its own. A large change lands as a sequence of such steps, not one drop at the end.
- Rebase onto the mainline before landing and again whenever it moves under you; resolve conflicts while they are small.
- A commit nobody else can see is not integrated. Push what landed, unless the user said to hold it.
- If the repo works through pull requests: a branch lives hours, not days, and a pull request is small enough to review in one sitting.

## Every change is verified

- Run the tests the change touches while working, and the full suite before each commit.
- Read the CI verdict on every push rather than assuming it. A change is integrated when the mainline build is green with it, not when it is pushed.
- A test that fails some runs and not others is a broken build that happens to pass sometimes. Fix it or quarantine it with an item saying so; never rerun until green.

## A red mainline stops the line

- When the mainline build goes red, fixing it is the next thing: before the task in hand, before any new commit. Say so first in the reply.
- Fix forward if the fix is quick and obvious; otherwise revert the commit that broke it and fix it off the mainline.
- Never land on a red mainline. A second commit on a red build hides which one broke it.
- "It was already red" is the first sentence of the report, not a reason to carry on.

## The mainline is always releasable

- Every commit on the mainline could be released as it stands: it builds, passes, and a user could run it.
- Unfinished work lands dark: behind a flag, unregistered, or unreachable from the UI and the CLI until it is whole. Never parked on a long-lived branch, and never visible half-done.
- What goes with a change lands in the same commit, so releasing needs no catch-up: the changelog entry, the docs and site edit, the migration of saved data, the version of any contract it moved.
- Migrations are expand then contract: the new version reads what the old one wrote, and the old one survives what the new one writes, so a release can be rolled back.

## A release is a non-event

- Release on demand, small and often. A release that bundles months of change is where the surprises are.
- Build once, promote the artifact. The thing tested is the thing shipped; nothing is rebuilt between a check and a release.
- Releasing and rolling back are each one command anyone on the project can run, automated end to end, with no step that lives only in someone's head.
- Where every green mainline commit goes to users automatically (continuous deployment), everything above is load-bearing: there is no release step left to catch what they missed.
