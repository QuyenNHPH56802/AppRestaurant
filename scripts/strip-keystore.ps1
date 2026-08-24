$ErrorActionPreference = 'Stop'
Set-Location "C:\Users\Administrator\App\AppRestaurant"

$env:FILTER_BRANCH_SQUELCH_WARNING = '1'

# Lấy danh sách tất cả branch (local + remote)
$branches = & "C:\Program Files\Git\bin\git.exe" for-each-ref --format='%(refname:short)' refs/heads refs/remotes
Write-Host "Branches detected:" $branches

# Các path cần gỡ khỏi mọi commit
$pathsToRemove = @(
    'android/keystore/restaurant-release.jks',
    'android/keystore/'
)

# Dùng index-filter để xóa hoàn toàn file khỏi mọi commit
foreach ($path in $pathsToRemove) {
    Write-Host "Removing path from history: $path"
    & "C:\Program Files\Git\bin\git.exe" filter-branch --force --index-filter `
        "git rm --cached --ignore-unmatch '$path'" `
        --prune-empty --tag-name-filter cat -- --all
}

Write-Host "Cleaning up refs and repacking..."
& "C:\Program Files\Git\bin\git.exe" for-each-ref --format='%(refname)' refs/original | ForEach-Object { & "C:\Program Files\Git\bin\git.exe" update-ref -d $_ }
& "C:\Program Files\Git\bin\git.exe" reflog expire --expire=now --all
& "C:\Program Files\Git\bin\git.exe" gc --prune=now --aggressive

Write-Host "Done."
& "C:\Program Files\Git\bin\git.exe" log --all --oneline
& "C:\Program Files\Git\bin\git.exe" ls-files | Select-String -Pattern "keystore" | ForEach-Object { "STILL TRACKED: $_" }