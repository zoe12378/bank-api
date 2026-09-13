# Tests a permitted transfer from the signed-in user's own account.
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

    Write-Output 'Step 2: transferring 10.00 from A001 to B001...'
    $transferJson = @{ fromAccountNumber = 'A001'; toAccountNumber = 'B001'; amount = 10.00 } | ConvertTo-Json
    $result = Invoke-RestMethod `
        -Method Post `
        -Uri "$api/api/transfers" `
        -ContentType 'application/json' `
        -Headers @{ Authorization = "Bearer $($login.accessToken)" } `
        -Body $transferJson

    Write-Output 'Transfer succeeded:'
    $result | ConvertTo-Json
} catch {
    Write-Output "Transfer failed: $($PSItem.Exception.Message)"
    if ($PSItem.ErrorDetails.Message) {
        Write-Output $PSItem.ErrorDetails.Message
    }
}
