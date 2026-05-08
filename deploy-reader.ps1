param(
    [string]$TargetDir = "C:\GreenSoft\reader",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$distDir = Join-Path $root "build\dist"

function Copy-RequiredFile {
    param(
        [string]$Name
    )

    $source = Join-Path $distDir $Name
    $target = Join-Path $TargetDir $Name

    if (!(Test-Path -LiteralPath $source)) {
        throw "Missing package file: $source"
    }

    Copy-Item -LiteralPath $source -Destination $target -Force
}

Set-Location $root

if (!$SkipBuild) {
    & .\gradlew.bat packageDist
    if ($LASTEXITCODE -ne 0) {
        throw "packageDist failed with exit code $LASTEXITCODE"
    }
}

if (!(Test-Path -LiteralPath $TargetDir)) {
    New-Item -ItemType Directory -Path $TargetDir | Out-Null
}

Copy-RequiredFile "reader.jar"
Copy-RequiredFile "reader-tray.jar"
Copy-RequiredFile "run-tray.bat"

foreach ($dir in @("storage", "logs")) {
    $path = Join-Path $TargetDir $dir
    if (!(Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
    }
}

Write-Host "Reader deployed to $TargetDir"
Write-Host "Run with: $TargetDir\run-tray.bat"
