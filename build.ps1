param(
  [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"

$PluginRoot = $PSScriptRoot
$ApiJar = Get-ChildItem -Path (Join-Path $Root "skyblock\libraries\io\papermc\paper\paper-api") -Recurse -Filter "paper-api-*.jar" |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

if (-not $ApiJar) {
  throw "Could not find a Paper API jar under $Root\skyblock\libraries. Start your server once or run scripts\download-jars.ps1 first."
}

$LibraryJars = Get-ChildItem -Path (Join-Path $Root "skyblock\libraries") -Recurse -Filter "*.jar" |
  Sort-Object FullName
$Classpath = (($LibraryJars.FullName + @($ApiJar.FullName)) | Select-Object -Unique) -join [System.IO.Path]::PathSeparator

$EnvPath = Join-Path $Root ".env"
$JavaBin = $null
if (Test-Path $EnvPath) {
  foreach ($line in Get-Content $EnvPath) {
    if ($line -match "^JAVA_EXE=(.+)$") {
      $JavaBin = Split-Path $Matches[1] -Parent
      break
    }
  }
}

$Javac = if ($JavaBin -and (Test-Path (Join-Path $JavaBin "javac.exe"))) {
  Join-Path $JavaBin "javac.exe"
} else {
  "javac"
}

$Jar = if ($JavaBin -and (Test-Path (Join-Path $JavaBin "jar.exe"))) {
  Join-Path $JavaBin "jar.exe"
} else {
  "jar"
}

$BuildDir = Join-Path $PluginRoot "build"
$ClassesDir = Join-Path $BuildDir "classes"
$JarPath = Join-Path $BuildDir "isles-1.0.0.jar"
$DeployPath = Join-Path $Root "skyblock\plugins\isles-1.0.0.jar"

if (Test-Path $BuildDir) {
  Remove-Item -Recurse -Force $BuildDir
}

New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $PluginRoot "src\main\java") -Recurse -Filter "*.java"
if (-not $Sources) {
  throw "No Java sources found."
}

& $Javac -encoding UTF-8 -cp $Classpath -d $ClassesDir @($Sources.FullName)
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

$ResourcesDir = Join-Path $PluginRoot "src\main\resources"
if (Test-Path $ResourcesDir) {
  Copy-Item -Path (Join-Path $ResourcesDir "*") -Destination $ClassesDir -Recurse -Force
}

Push-Location $ClassesDir
try {
  & $Jar cf $JarPath .
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

Copy-Item -Path $JarPath -Destination $DeployPath -Force
Write-Host "Built $JarPath"
Write-Host "Deployed $DeployPath"
