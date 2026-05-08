param(
    [string]$TargetDir = "C:\GreenSoft\reader",
    [switch]$SkipBuild,
    [switch]$SkipFrontendBuild
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

function Invoke-CheckedCommand {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    & $Name @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Name $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Invoke-FrontendBuild {
    $webDir = Join-Path $root "web"
    $nodeModulesDir = Join-Path $webDir "node_modules"
    $distWebDir = Join-Path $webDir "dist"
    $resourceRoot = Join-Path $root "src\main\resources"
    $resourceWebDir = Join-Path $resourceRoot "web"

    if (!(Test-Path -LiteralPath $webDir)) {
        throw "Missing web directory: $webDir"
    }

    Push-Location $webDir
    try {
        if (!(Test-Path -LiteralPath $nodeModulesDir)) {
            Invoke-CheckedCommand "npm" @("install", "--legacy-peer-deps")
        }

        Invoke-CheckedCommand "npm" @("run", "build")
    }
    finally {
        Pop-Location
    }

    if (!(Test-Path -LiteralPath $distWebDir)) {
        throw "Missing frontend build output: $distWebDir"
    }

    $resolvedResourceRoot = [System.IO.Path]::GetFullPath($resourceRoot)
    $resolvedResourceWebDir = [System.IO.Path]::GetFullPath($resourceWebDir)
    if (!$resolvedResourceWebDir.StartsWith($resolvedResourceRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to replace unexpected resource path: $resourceWebDir"
    }

    if (Test-Path -LiteralPath $resourceWebDir) {
        Remove-Item -LiteralPath $resourceWebDir -Recurse -Force
    }
    Move-Item -LiteralPath $distWebDir -Destination $resourceWebDir
}

Set-Location $root

if (!$SkipBuild) {
    if (!$SkipFrontendBuild) {
        Invoke-FrontendBuild
    }

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
