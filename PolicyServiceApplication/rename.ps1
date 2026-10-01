$files = Get-ChildItem -Path 'src\main\java\com\insureflow','src\test\java\com\insureflow' -Recurse -Filter *.java
foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $content = $content -replace 'com\.PolicyServiceApplication\.demo', 'com.insureflow.policyservice'
    Set-Content -NoNewline -Path $f.FullName -Value $content
}
Write-Host "Done replacing package references in $($files.Count) files"
