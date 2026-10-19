param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
            $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH
        } else { throw 'JDK 17 is required. Install Android Studio and set JAVA_HOME to its jbr folder, or install a JDK 17.' }
    }
    if (-not $env:ANDROID_HOME -and -not (Test-Path 'local.properties')) {
        $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
        if (Test-Path $defaultSdk) { $env:ANDROID_HOME = $defaultSdk }
        else { throw 'Android SDK not found. Install SDK Platform 35 and Build Tools 35.0.0 using Android Studio, then set ANDROID_HOME.' }
    }
    if (-not (Test-Path 'gradlew.bat')) {
        $toolsDir = Join-Path $projectRoot '.tools'
        New-Item -ItemType Directory -Force $toolsDir | Out-Null
        $zip = Join-Path $toolsDir 'gradle-8.11.1-bin.zip'
        $url = 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip'
        $expectedHash = (Invoke-RestMethod -Uri ($url + '.sha256')).Trim().ToLowerInvariant()
        if ($expectedHash -notmatch '^[a-f0-9]{64}$') { throw 'Invalid Gradle checksum response.' }
        if (-not (Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }
        if ((Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) { throw 'Gradle archive checksum mismatch. Remove .tools/gradle-8.11.1-bin.zip and retry.' }
        if (-not (Test-Path (Join-Path $toolsDir 'gradle-8.11.1/bin/gradle.bat'))) { Expand-Archive -LiteralPath $zip -DestinationPath $toolsDir -Force }
        & (Join-Path $toolsDir 'gradle-8.11.1/bin/gradle.bat') wrapper --gradle-version 8.11.1 --distribution-type bin
        if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper generation failed.' }
        Add-Content -LiteralPath 'gradle/wrapper/gradle-wrapper.properties' -Value "`ndistributionSha256Sum=$expectedHash"
    }
    & './gradlew.bat' ':app:testDebugUnitTest' ':app:assembleDebug'
    if ($LASTEXITCODE -ne 0) { throw 'Android build or unit tests failed.' }
    if ($DeviceTests) {
        & './gradlew.bat' ':app:connectedDebugAndroidTest'
        if ($LASTEXITCODE -ne 0) { throw 'Device tests failed.' }
    }
    Get-FileHash -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Algorithm SHA256
} finally { Pop-Location }
