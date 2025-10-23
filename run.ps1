Write-Host "Starting Warsaw Transport Map Application..."
Set-Location -Path $PSScriptRoot
$env:Path = "$env:Path;$(Get-Location)\apache-maven-3.9.4\bin"
mvn.cmd javafx:run

