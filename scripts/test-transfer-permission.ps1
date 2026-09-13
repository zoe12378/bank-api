# Tests that a user cannot debit an account they do not own.
# Password is requested at runtime only.

$api = 'http' + '://localhost:8080'
$securePassword = Read-Host 'huang_demo password' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $securePassword).Password
$loginJson = @{ username = 'huang_demo'; password = $password } | ConvertTo-Json

try {
    Write-Output 'Step 1: logging in...'
    $login = Invoke-RestMethod `
        -Method Post `
        -Uri "$api/api/auth/login" `
        -ContentType 'application/json' `
        -Body $loginJson

    Write-Output 'Step 1 passed: login succeeded.'
    Write-Output 'Step 2: testing an unauthorized transfer...'
    $transferJson = @{ fromAccountNumber = 'B001'; toAccountNumber = 'A001'; amount = 1.00 } | ConvertTo-Json
    Invoke-RestMethod `
        -Method Post `
        -Uri "$api/api/transfers" `
        -ContentType 'application/json' `
        -Headers @{ Authorization = "Bearer $($login.accessToken)" } `
        -Body $transferJson

    Write-Output 'UNEXPECTED: transfer was allowed.'
} catch {
    Write-Output 'Request rejected. Expected status: 403.'
    if ($PSItem.ErrorDetails.Message) {
        Write-Output $PSItem.ErrorDetails.Message
    } else {
        Write-Output "PowerShell error: $($PSItem.Exception.Message)"
    }
}
