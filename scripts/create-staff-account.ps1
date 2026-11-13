param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $FullName,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $Email,

    [Parameter(Mandatory = $true)]
    [ValidateSet('STAFF', 'MANAGER', 'BAKER_STAFF')]
    [string] $Role,

    [string] $Phone,
    [string] $Address,
    [string] $DateOfBirth,
    [string] $Gender,

    [string] $ApiBaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:ADMIN_ACCESS_TOKEN)) {
    throw 'Set ADMIN_ACCESS_TOKEN to a valid ADMIN bearer access token before running this script.'
}

$requestBody = @{
    fullName = $FullName
    email = $Email
    role = $Role
    phone = $Phone
    address = $Address
    dateOfBirth = $DateOfBirth
    gender = $Gender
} | ConvertTo-Json -Depth 4

$headers = @{
    Authorization = "Bearer $env:ADMIN_ACCESS_TOKEN"
}

$uri = "$($ApiBaseUrl.TrimEnd('/'))/api/v1/admin/accounts"
$response = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers `
    -ContentType 'application/json' -Body $requestBody

$response | ConvertTo-Json -Depth 8
