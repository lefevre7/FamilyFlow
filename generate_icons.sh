#!/usr/bin/env bash

# Icon Generation Script for Family Flow
# Generates Android and iOS app icons from source image
# Usage: ./generate_icons.sh <source_image_path>

set -e

SOURCE_IMAGE="$1"
if [ -z "$SOURCE_IMAGE" ] || [ ! -f "$SOURCE_IMAGE" ]; then
    echo "Usage: $0 <source_image_path>"
    echo "Example: $0 ~/Downloads/FamilyFlow.jpeg"
    exit 1
fi

# Project paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_RES="$SCRIPT_DIR/composeApp/src/androidMain/res"
IOS_ASSETS="$SCRIPT_DIR/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"

echo "🎨 Family Flow Icon Generation"
echo "Source: $SOURCE_IMAGE"
echo ""

# Check if sips is available (macOS)
if ! command -v sips &> /dev/null; then
    echo "❌ Error: sips command not found. This script requires macOS."
    exit 1
fi

# Create a temporary square version of the source image (fit entire image with padding)
echo "📐 Creating square version (fit entire image with padding to 1024x1024)..."
TEMP_SQUARE="/tmp/familyflow_square_1024.png"
TEMP_BG="/tmp/familyflow_bg_1024.png"

# Convert source to PNG and get dimensions
sips -s format png "$SOURCE_IMAGE" --out "$TEMP_SQUARE" &>/dev/null
WIDTH=$(sips -g pixelWidth "$TEMP_SQUARE" | grep pixelWidth | awk '{print $2}')
HEIGHT=$(sips -g pixelHeight "$TEMP_SQUARE" | grep pixelHeight | awk '{print $2}')

# Calculate scaling to fit within 1024x1024 while maintaining aspect ratio
if [ "$WIDTH" -gt "$HEIGHT" ]; then
    # Wider than tall - scale based on width
    NEW_WIDTH=1024
    NEW_HEIGHT=$(( HEIGHT * 1024 / WIDTH ))
else
    # Taller than wide - scale based on height
    NEW_HEIGHT=1024
    NEW_WIDTH=$(( WIDTH * 1024 / HEIGHT ))
fi

# Resize to fit within 1024x1024
sips --resampleHeightWidth "$NEW_HEIGHT" "$NEW_WIDTH" "$TEMP_SQUARE" &>/dev/null

# Create a 1024x1024 background with calm beige color (#F9F7F2)
# Note: sips doesn't support creating colored backgrounds directly, so we'll pad with white
# and the adaptive icon background layer will provide the beige color
sips --padToHeightWidth 1024 1024 --padColor FFFFFF "$TEMP_SQUARE" &>/dev/null

echo "✅ Square base image created"
echo ""

# Generate Android launcher icons
echo "🤖 Generating Android launcher icons..."

generate_android_icon() {
    local density="$1"
    local size="$2"
    local dest_dir="$ANDROID_RES/mipmap-$density"
    
    echo "  • $density: ${size}x${size}"
    
    # Regular launcher icon
    sips --resampleHeightWidth "$size" "$size" "$TEMP_SQUARE" \
        --out "$dest_dir/ic_launcher.png" &>/dev/null
    
    # Round launcher icon (same as regular for this app)
    cp "$dest_dir/ic_launcher.png" "$dest_dir/ic_launcher_round.png"
}

generate_android_icon "mdpi" 48
generate_android_icon "hdpi" 72
generate_android_icon "xhdpi" 96
generate_android_icon "xxhdpi" 144
generate_android_icon "xxxhdpi" 192

echo "✅ Android launcher icons generated"
echo ""

# Generate Android foreground icons (for adaptive icons)
echo "🤖 Generating Android adaptive icon foregrounds..."

generate_android_foreground() {
    local density="$1"
    local size="$2"
    local dest_dir="$ANDROID_RES/mipmap-$density"
    
    echo "  • $density: ${size}x${size}"
    
    # Create foreground with transparent background
    # For foreground, we use the full square image but sized appropriately
    sips --resampleHeightWidth "$size" "$size" "$TEMP_SQUARE" \
        --out "$dest_dir/ic_launcher_foreground.png" &>/dev/null
}

generate_android_foreground "mdpi" 108
generate_android_foreground "hdpi" 162
generate_android_foreground "xhdpi" 216
generate_android_foreground "xxhdpi" 324
generate_android_foreground "xxxhdpi" 432

echo "✅ Android adaptive icon foregrounds generated"
echo ""

# Generate iOS icon
echo "🍎 Generating iOS app icon..."
echo "  • 1024x1024"
sips --resampleHeightWidth 1024 1024 "$TEMP_SQUARE" \
    --out "$IOS_ASSETS/app-icon-1024.png" &>/dev/null

echo "✅ iOS app icon generated"
echo ""

# Cleanup
rm -f "$TEMP_SQUARE" "$TEMP_BG"

echo "🎉 All icons generated successfully!"
echo ""
echo "📝 Next steps:"
echo "  1. Review the generated icons in Android Studio / Xcode"
echo "  2. Run: ./gradlew :composeApp:assembleDebug"
echo "  3. Test on device/emulator to verify icons appear correctly"
echo ""
