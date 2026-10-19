# Packs the Windows app folder `./gradlew :desktop:createDistributable` built into a signed MSIX.
#
#   msix.ps1 -Version X.Y.Z -Out hughandkettle.msix [-Pfx cert.pfx]
#
# The certificate's password comes from MSIX_PFX_PASSWORD, and a real one must pair with release/msix.cer.
# Without -Pfx it makes a throwaway self-signed certificate and leaves its public half beside the MSIX as a
# .cer, which Windows must trust before installing.
param(
    [Parameter(Mandatory)] [string] $Version,
    [Parameter(Mandatory)] [string] $Out,
    [string] $Pfx = ""
)
$ErrorActionPreference = "Stop"
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "version must be X.Y.Z, got '$Version'" }

$here = $PSScriptRoot
$app = Join-Path $here "..\build\compose\binaries\main\app\hughandkettle"
if (-not (Test-Path (Join-Path $app "hughandkettle.exe"))) { throw "no app folder at $app; run :desktop:createDistributable first" }

# makeappx and signtool from the newest Windows SDK on the machine.
$kit = Get-ChildItem "${env:ProgramFiles(x86)}\Windows Kits\10\bin\10.*\x64\makeappx.exe" | Sort-Object FullName | Select-Object -Last 1
if (-not $kit) { throw "no Windows SDK with makeappx.exe" }
$makeappx = $kit.FullName
$signtool = Join-Path $kit.DirectoryName "signtool.exe"

$stage = Join-Path ([IO.Path]::GetTempPath()) ("msix-" + [Guid]::NewGuid())
Copy-Item $app $stage -Recurse
Copy-Item (Join-Path $here "msix\Assets") (Join-Path $stage "Assets") -Recurse
(Get-Content (Join-Path $here "msix\AppxManifest.xml") -Raw).Replace("@VERSION@", "$Version.0") |
    Set-Content (Join-Path $stage "AppxManifest.xml") -Encoding utf8

$Out = [IO.Path]::GetFullPath($Out)
New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
& $makeappx pack /o /d $stage /p $Out
if ($LASTEXITCODE -ne 0) { throw "makeappx failed" }
Remove-Item $stage -Recurse -Force

$subject = "CN=Hugh and Kettle"
if ($Pfx) {
    $committed = Join-Path $here "..\..\release\msix.cer"
    if (-not (Test-Path $committed)) { throw "release/msix.cer is missing; see release/README.md" }
    $real = Get-PfxData -FilePath $Pfx -Password (ConvertTo-SecureString $env:MSIX_PFX_PASSWORD -AsPlainText -Force)
    $public = New-Object Security.Cryptography.X509Certificates.X509Certificate2 $committed
    if ($real.EndEntityCertificates[0].Thumbprint -ne $public.Thumbprint) { throw "the certificate does not pair with release/msix.cer" }
} else {
    $env:MSIX_PFX_PASSWORD = "throwaway"
    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $subject -CertStoreLocation Cert:\CurrentUser\My `
        -NotAfter (Get-Date).AddDays(30)
    $Pfx = [IO.Path]::ChangeExtension($Out, ".throwaway.pfx")
    $secure = ConvertTo-SecureString $env:MSIX_PFX_PASSWORD -AsPlainText -Force
    Export-PfxCertificate -Cert $cert -FilePath $Pfx -Password $secure | Out-Null
    Export-Certificate -Cert $cert -FilePath ([IO.Path]::ChangeExtension($Out, ".cer")) | Out-Null
    Remove-Item "Cert:\CurrentUser\My\$($cert.Thumbprint)"
}
& $signtool sign /fd SHA256 /f $Pfx /p $env:MSIX_PFX_PASSWORD $Out
if ($LASTEXITCODE -ne 0) { throw "signtool failed; the certificate's subject must be '$subject'" }
if ($Pfx -like "*.throwaway.pfx") { Remove-Item $Pfx }
Write-Output "msix: $Out"
