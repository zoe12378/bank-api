# Lists transactions for A001 after logging in as huang_demo.
# Password is requested at runtime only.

$api = 'http' + '://localhost:8080'
$securePassword = Read-Host 'huang_demo password' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $securePassword).Password
$loginJson = @{ username = 'huang_demo'; password = $password } | ConvertTo-Json

try {
    $login = Invoke-RestMethod `
        -Method Post `
        -Uri "$api/api/auth/login" `
        -ContentType 'application/json' `
        -Body $loginJson

    $transactions = Invoke-RestMethod `
        -Method Get `
        -Uri "$api/api/accounts/A001/transactions" `
        -Headers @{ Authorization = "Bearer $($login.accessToken)" }

    $transactions | Format-Table id, transactionType, status, amount, balanceAfter, counterpartyAccountNumber, createdAt
} catch {
    Write-Output "Request failed: $($PSItem.Exception.Message)"
    if ($PSItem.ErrorDetails.Message) {
        Write-Output $PSItem.ErrorDetails.Message
    }
}
