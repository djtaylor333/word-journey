
$wordsPath = "c:\Users\david\OneDrive\Documents\projects\AI Projects\word-journey\app\src\main\assets\words.json"
$existing = Get-Content $wordsPath -Raw | ConvertFrom-Json

$newJsonPath = "c:\Users\david\AppData\Roaming\Code\User\workspaceStorage\423c01632c0f0a8d51a9d2e88036fe6c\GitHub.copilot-chat\chat-session-resources\59af7bdb-e951-4967-bfab-1fba8ad0faa7\toolu_bdrk_013Mi86ibd3wHgdYrfPDuZa6__vscode-1771612156676\content.txt"
$rawText = Get-Content $newJsonPath -Raw
$rawText = $rawText -replace '(?s)^```json\s*', '' -replace '\s*```\s*$', ''
$newWords = $rawText.Trim() | ConvertFrom-Json

function Merge-WordArrays($existingArr, $newArr) {
    $dict = [ordered]@{}
    foreach ($entry in $existingArr) { $dict[$entry.word] = $entry }
    foreach ($entry in $newArr) {
        if (-not $dict.Contains($entry.word)) { $dict[$entry.word] = $entry }
    }
    return @($dict.Values)
}

$merged3 = Merge-WordArrays $existing."3" $newWords."3"
$merged4 = Merge-WordArrays $existing."4" $newWords."4"
$merged5 = Merge-WordArrays $existing."5" $newWords."5"
$merged6 = $existing."6"
$merged7 = Merge-WordArrays $existing."7" $newWords."7"

Write-Host "Merged counts: 3=$($merged3.Count), 4=$($merged4.Count), 5=$($merged5.Count), 6=$($merged6.Count), 7=$($merged7.Count)"

$output = [ordered]@{
    "3" = $merged3
    "4" = $merged4
    "5" = $merged5
    "6" = $merged6
    "7" = $merged7
}

$json = $output | ConvertTo-Json -Depth 5
Set-Content -Path $wordsPath -Value $json -Encoding UTF8
Write-Host "words.json updated successfully!"
