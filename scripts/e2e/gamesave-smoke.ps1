param(
    [string]$BaseUrl = "http://localhost:18080",
    [int]$StartupTimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

function Assert-That([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}

function Invoke-GameSaveJson([string]$method, [string]$path, $body, [string]$token = $null) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    $parameters = @{
        Method = $method
        Uri = "$BaseUrl$path"
        Headers = $headers
        ContentType = "application/json"
    }
    if ($null -ne $body) { $parameters.Body = ($body | ConvertTo-Json -Depth 8) }
    Invoke-RestMethod @parameters
}

function Upload-Object([string]$token, [string]$path) {
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    $size = (Get-Item -LiteralPath $path).Length
    $json = & curl.exe --silent --show-error --fail-with-body `
        -X POST "$BaseUrl/api/game-save/v1/objects" `
        -H "Authorization: Bearer $token" `
        -F "file=@$path;type=application/octet-stream" `
        -F "sha256=$hash" `
        -F "size=$size"
    if ($LASTEXITCODE -ne 0) { throw "Object upload failed: $json" }
    $response = $json | ConvertFrom-Json
    Assert-That ($response.data.sha256 -eq $hash) "Server SHA-256 does not match uploaded content"
    [PSCustomObject]@{ ObjectId = $response.data.objectId; Sha256 = $hash; Size = $size }
}

Write-Host "Waiting for CMS E2E environment..."
$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
do {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/" -TimeoutSec 3
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) { break }
    } catch { Start-Sleep -Seconds 2 }
} while ((Get-Date) -lt $deadline)
Assert-That ((Get-Date) -lt $deadline) "CMS did not start within $StartupTimeoutSeconds seconds"

$tempDirectory = Join-Path ([IO.Path]::GetTempPath()) ("gamesave-e2e-" + [Guid]::NewGuid().ToString("N"))
[IO.Directory]::CreateDirectory($tempDirectory) | Out-Null
try {
    $username = "e2e" + [Guid]::NewGuid().ToString("N").Substring(0, 16)
    $password = "GameSave-E2E-Password"
    $deviceA = "e2e-device-a-" + [Guid]::NewGuid().ToString("N")
    $deviceB = "e2e-device-b-" + [Guid]::NewGuid().ToString("N")

    $register = Invoke-GameSaveJson POST "/api/game-save/v1/auth/register" @{
        username = $username; password = $password; deviceId = $deviceA; deviceName = "E2E-A"
    }
    $tokenA = $register.data.deviceToken
    Assert-That (-not [string]::IsNullOrWhiteSpace($tokenA)) "Register response is missing device token"

    $game = Invoke-GameSaveJson POST "/api/game-save/v1/games" @{ name = "E2E Game"; provider = "CUSTOM" } $tokenA
    $gameId = $game.data.gameId

    $fileA = Join-Path $tempDirectory "slot-a.sav"
    [IO.File]::WriteAllBytes($fileA, [Text.Encoding]::UTF8.GetBytes("e2e-first-content"))
    $objectA = Upload-Object $tokenA $fileA
    $commitA = Invoke-GameSaveJson POST "/api/game-save/v1/games/$gameId/snapshots" @{
        expectedHeadSnapshotId = $null; triggerType = "MANUAL"; description = "E2E first snapshot"
        files = @(@{ path = "slot-a.sav"; sha256 = $objectA.Sha256; size = $objectA.Size })
    } $tokenA
    $snapshotA = $commitA.data.snapshotId
    Assert-That ($commitA.data.created -eq $true) "First snapshot was not created"

    $loginB = Invoke-GameSaveJson POST "/api/game-save/v1/auth/login" @{
        username = $username; password = $password; deviceId = $deviceB; deviceName = "E2E-B"
    }
    $tokenB = $loginB.data.deviceToken

    $conflictObserved = $false
    try {
        Invoke-GameSaveJson POST "/api/game-save/v1/games/$gameId/snapshots" @{
            expectedHeadSnapshotId = $null; triggerType = "MANUAL"; description = "Expected conflict"
            files = @(@{ path = "slot-a.sav"; sha256 = $objectA.Sha256; size = $objectA.Size })
        } $tokenB | Out-Null
    } catch {
        $conflictObserved = $_.Exception.Message -match "409|SYNC_CONFLICT"
    }
    Assert-That $conflictObserved "Stale HEAD did not return a sync conflict"

    $fileB = Join-Path $tempDirectory "slot-b.sav"
    [IO.File]::WriteAllBytes($fileB, [Text.Encoding]::UTF8.GetBytes("e2e-second-content"))
    $objectB = Upload-Object $tokenB $fileB
    $commitB = Invoke-GameSaveJson POST "/api/game-save/v1/games/$gameId/snapshots" @{
        expectedHeadSnapshotId = $snapshotA; triggerType = "MANUAL"; description = "E2E second snapshot"
        files = @(
            @{ path = "slot-a.sav"; sha256 = $objectA.Sha256; size = $objectA.Size },
            @{ path = "slot-b.sav"; sha256 = $objectB.Sha256; size = $objectB.Size }
        )
    } $tokenB
    Assert-That ($commitB.data.created -eq $true) "Second snapshot was not created"

    $quota = Invoke-GameSaveJson GET "/api/game-save/v1/account/quota" $null $tokenB
    $expectedUsedBytes = [int64]$objectA.Size + [int64]$objectB.Size
    Assert-That ([int64]$quota.data.usedBytes -eq $expectedUsedBytes) "Quota usage does not match unique uploaded objects"
    $timeline = Invoke-GameSaveJson GET "/api/game-save/v1/games/$gameId/snapshots?limit=10" $null $tokenB
    Assert-That ($timeline.data.Count -eq 2) "Snapshot timeline count is incorrect"
    $manifest = Invoke-GameSaveJson GET "/api/game-save/v1/games/$gameId/snapshots/$($commitB.data.snapshotId)" $null $tokenB
    Assert-That ($manifest.data.files.Count -eq 2) "Snapshot manifest is incomplete"

    $downloadUrl = Invoke-GameSaveJson GET "/api/game-save/v1/objects/$($objectB.ObjectId)/download-url" $null $tokenB
    $downloaded = Join-Path $tempDirectory "downloaded.sav"
    Invoke-WebRequest -UseBasicParsing -Uri $downloadUrl.data -OutFile $downloaded
    Assert-That ((Get-FileHash -LiteralPath $downloaded -Algorithm SHA256).Hash.ToLowerInvariant() -eq $objectB.Sha256) "Presigned object download validation failed"

    Write-Host "[PASS] GameSave E2E: registration, two-device conflict, objects, snapshots, timeline, manifest, download."
}
finally {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}