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

## The Android key

One key signs the APK, the bundle for Google Play and every store's build (ADR 0015). Losing it means
the app can never be updated again, so keep the `.jks` and its password backed up in two places off the
machine. To make it once:

```sh
keytool -genkeypair -keystore hughandkettle-release.jks -alias hughandkettle \
    -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Hugh and Kettle"
```

## The secrets

The `Release` workflow reads three:

```sh
gh secret set RELEASE_SSH_KEY < hughandkettle-release
base64 -i hughandkettle-release.jks | gh secret set ANDROID_KEYSTORE_BASE64
gh secret set ANDROID_KEYSTORE_PASSWORD
```

The SSH key is made without a passphrase, since CI cannot type one. The Windows certificate below is
optional; without it a release has no MSIX.

## The Windows certificate

Windows installs an MSIX only if its signature chains to a certificate it trusts, and the MSIX's
publisher (`CN=Hugh and Kettle`, in `desktop/packaging/msix/AppxManifest.xml`) must be that
certificate's subject. `msix.cer` is its public half; it is not here yet. The private half, as a
`.pfx`, lives only in the CI secret store (`MSIX_PFX_BASE64`, `MSIX_PFX_PASSWORD`) and on the user's machine.

A self-signed certificate is trusted by nobody until the user imports `msix.cer` once into
*Local Machine › Trusted People* (double-click it, Install Certificate). A certificate from a
code-signing authority, or Azure Trusted Signing, takes that step away.

To make a self-signed one, valid five years:

```sh
openssl req -x509 -newkey rsa:3072 -nodes -days 1825 -subj "/CN=Hugh and Kettle" \
    -addext "keyUsage=digitalSignature" -addext "extendedKeyUsage=codeSigning" \
    -addext "basicConstraints=CA:FALSE" -keyout msix.key -out msix.crt
openssl pkcs12 -export -inkey msix.key -in msix.crt -out msix.pfx
openssl x509 -in msix.crt -outform der -out release/msix.cer
base64 -w0 msix.pfx   # the MSIX_PFX_BASE64 secret; the export password is MSIX_PFX_PASSWORD
```

Commit `release/msix.cer` only.
