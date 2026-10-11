# Release keys

`allowed_signers` holds the public half of the key that signs a release's `SHA256SUMS`, in OpenSSH
allowed-signers format: `hughandkettle ssh-ed25519 AAAA...`. It is not here yet; TODO.md says who
makes it. The private half lives only in the CI secret store and on the user's machine.

To verify a download:

```sh
ssh-keygen -Y verify -f allowed_signers -I hughandkettle -n hughandkettle -s SHA256SUMS.sig < SHA256SUMS
sha256sum -c SHA256SUMS
```

To make the key once: `ssh-keygen -t ed25519 -C hughandkettle-release -f hughandkettle-release`, then
`echo "hughandkettle $(cat hughandkettle-release.pub)" > release/allowed_signers` and commit that file only.
