# 7. The link is held by the process and kept alive by a foreground service while it is up

Date: 2026-10-11

## Context

On stage the phone's screen goes off, and a link that drops then is a lost sound change mid-song.
An activity, and a ViewModel scoped to it, can be destroyed or frozen with the screen off, and a
process with nothing in the foreground is the first Android kills. The link's retries also need a
home that outlives any one screen, and the plan asks that nothing about retries live in Android code.

## Decision

- The link is one `LinkKeeper` (in `wmi`) per process, held by `LinkHolder` on the application and
  exposed as flows. The panel and the notification both render its `LinkState`, through `LinkWords`.
- `LinkService` is a foreground service of type `connectedDevice` that runs exactly while the link is
  not idle: the application starts it when the link leaves idle, and it stops itself when the link
  returns there. Its notification says where the link stands and carries a Disconnect action.
- The service is started, not bound. A bound service lives only while a client is bound, which is the
  lifetime of the screen this is meant to outlive.

## Consequences

- The process, and with it the link and its retries, survives the screen going off and the activity
  going away, for as long as the link is wanted.
- The user sees a notification whenever the app holds the WMI-1's one link, so it is never held
  without their knowing, and they can drop it from there.
- The service holds no logic: it renders a state and forwards one action. Its test runs on
  Robolectric; the link's behavior is tested in `wmi`.
