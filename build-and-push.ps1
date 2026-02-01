# Build and Push Docker Image Script
# Usage: .\build-and-push.ps1 -DockerUsername "your_username" -Version "1.0.0"

param(
    [Parameter(Position = 0, Mandatory = $true)]
    [string]$DockerUsername,
    
    [Parameter(Position = 1, Mandatory = $false)]
    [string]$Version = "latest"
)

# Colors for output
$ErrorColor = "Red"
$SuccessColor = "Green"
$InfoColor = "Cyan"
$WarningColor = "Yellow"

function Write-Header {
    param([string]$Message)
    Write-Host "`n" -ForegroundColor $InfoColor
    Write-Host ("=" * 70) -ForegroundColor $InfoColor
    Write-Host $Message -ForegroundColor $InfoColor
    Write-Host ("=" * 70) -ForegroundColor $InfoColor
}

function Write-Success {
    param([string]$Message)
    Write-Host "Success: $Message" -ForegroundColor $SuccessColor
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "Error: $Message" -ForegroundColor $ErrorColor
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "Warning: $Message" -ForegroundColor $WarningColor
}

function Write-Info {
    param([string]$Message)
    Write-Host "Info: $Message" -ForegroundColor $InfoColor
}

# Main script
Write-Header "Docker Build & Push Script"

# Ensure we're in the project root directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = $ScriptDir

# Check if we're in a subdirectory (like docker/)
if (Test-Path (Join-Path $ScriptDir "pom.xml")) {
    $ProjectRoot = $ScriptDir
    Write-Info "Running from project root: $ProjectRoot"
} elseif (Test-Path (Join-Path $ScriptDir ".." "pom.xml")) {
    $ProjectRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
    Write-Info "Changing to project root: $ProjectRoot"
    Set-Location $ProjectRoot
} else {
    Write-Error-Custom "Cannot find pom.xml. Please run this script from the project root directory."
    exit 1
}

# Validate Docker is installed
Write-Info "Checking Docker installation..."
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error-Custom "Docker is not installed or not in PATH"
    exit 1
}
Write-Success "Docker found"

# Validate Maven is installed
Write-Info "Checking Maven installation..."
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error-Custom "Maven is not installed or not in PATH"
    exit 1
}
Write-Success "Maven found"

# Set image details
$ImageName = "$DockerUsername/notification-service"
$FullImage = "${ImageName}:${Version}"

Write-Info "Image: $FullImage"
Write-Info "Version: $Version"

# Step 1: Clean and compile
Write-Header "Step 1/4: Cleaning and Compiling Project"
Write-Info "Running Maven clean install..."

try {
    $output = & mvn clean install -DskipTests 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Maven compilation failed"
        Write-Host $output
        exit 1
    }
    Write-Success "Project compiled successfully"
}
catch {
    Write-Error-Custom "Compilation failed: $_"
    exit 1
}

# Step 2: Build Docker image
Write-Header "Step 2/4: Building Docker Image"
Write-Info "Building image: $FullImage"

try {
    $buildOutput = & docker build -t $FullImage -t "${ImageName}:latest" . 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Docker build failed"
        Write-Host $buildOutput
        exit 1
    }
    Write-Success "Docker image built successfully"
}
catch {
    Write-Error-Custom "Docker build failed: $_"
    exit 1
}

# Step 3: Verify image
Write-Header "Step 3/4: Verifying Docker Image"
Write-Info "Checking image size and details..."

try {
    $ImageInfo = docker images --filter "reference=$FullImage" --format "{{.Size}}"
    if ($ImageInfo) {
        Write-Success "Image verified: Size = $ImageInfo"
    }
    else {
        Write-Error-Custom "Image not found after build"
        exit 1
    }
}
catch {
    Write-Error-Custom "Image verification failed: $_"
    exit 1
}

# Step 4: Push to Docker Hub
Write-Header "Step 4/4: Pushing to Docker Hub"
Write-Info "Pushing: $FullImage"
Write-Warning-Custom "Make sure you're logged in: docker login"

try {
    $pushOutput = & docker push $FullImage 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Docker push failed"
        Write-Host $pushOutput
        Write-Warning-Custom "Make sure you're logged in to Docker Hub: docker login"
        exit 1
    }
    Write-Success "Image pushed successfully"
    
    $pushLatest = & docker push "${ImageName}:latest" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Latest tag pushed successfully"
    }
}
catch {
    Write-Error-Custom "Docker push failed: $_"
    Write-Warning-Custom "Make sure you're logged in to Docker Hub: docker login"
    exit 1
}

# Final summary
Write-Header "Build & Push Complete!"

Write-Host ""
Write-Host "Your Docker image is now available on Docker Hub:" -ForegroundColor $SuccessColor
Write-Host ""
Write-Host "  Repository: https://hub.docker.com/r/$DockerUsername/notification-service" -ForegroundColor $InfoColor
Write-Host "  Image:      $FullImage" -ForegroundColor $InfoColor
Write-Host "  Size:       $ImageInfo" -ForegroundColor $InfoColor
Write-Host ""
Write-Host "Next steps:" -ForegroundColor $WarningColor
Write-Host ""
Write-Host "1. On your server, copy the deployment files:"
Write-Host "   - docker/docker-compose.prod.yml"
Write-Host "   - .env.example to .env (fill in your secrets)"
Write-Host ""
Write-Host "2. SSH to server and run:"
Write-Host "   docker compose -f docker-compose.prod.yml --env-file .env up -d"
Write-Host ""
Write-Host "3. Verify deployment:"
Write-Host "   curl http://localhost:8080/actuator/health"
Write-Host ""
Write-Host "For detailed instructions, see: docs/deployment/WORKFLOW.md" -ForegroundColor $WarningColor
Write-Host ""

exit 0
