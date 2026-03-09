import os
import re

directory = r"e:\Android Project Jetpack Compose\UniMarket\app\src\main\java\com\example\unimarket\presentation"

color_map = {
    "Color(0xFF03A9F4)": "LightBlueAction",
    "Color(0xFF0D99FF)": "SecondaryTextBlue",
    "Color(0xFF1E293B)": "TextDarkBlack",
    "Color(0xFF202124)": "TextDark",
    "Color(0xFF2196F3)": "BlueReview",
    "Color(0xFF29B6F6)": "AppBlue",
    "Color(0xFF2AB0FF)": "PrimaryBlue",
    "Color(0xFF34A853)": "AccentGreen",
    "Color(0xFF4CAF50)": "GreenBadge",
    "Color(0xFF5C7CFA)": "SecondaryBlue",
    "Color(0xFF5CA0FA)": "TagBlue",
    "Color(0xFF64748B)": "TextGray",
    "Color(0xFF78909C)": "SlateGrey",
    "Color(0xFFB0C0D0)": "DashColor",
    "Color(0xFFC8E6C9)": "GreenBadgeBorder",
    "Color(0xFFE0E0E0)": "DividerColor",
    "Color(0xFFE0E5EC)": "BorderLightBlue",
    "Color(0xFFE1F5FE)": "LightBlueSelection",
    "Color(0xFFE2E8F0)": "BorderLightGray",
    "Color(0xFFE3F2FD)": "LightBlueReviewBg",
    "Color(0xFFE53935)": "RedDanger",
    "Color(0xFFE8F4FD)": "LightBlueBg",
    "Color(0xFFE8F5E9)": "GreenBadgeBg",
    "Color(0xFFEDF2F7)": "TagBlueBg",
    "Color(0xFFEFF3F8)": "SurfaceLightBlue",
    "Color(0xFFF0F0F0)": "ProfileAvatarBorder",
    "Color(0xFFF1F5F9)": "ActionChipBg",
    "Color(0xFFF4F6F9)": "MessageBg",
    "Color(0xFFF7F8FA)": "BackgroundLight",
    "Color(0xFFF8FAFC)": "BackgroundLightAlternate",
    "Color(0xFFF9FAFC)": "SellTopBarBg",
    "Color(0xFFFBC02D)": "PrimaryYellowDark",
    "Color(0xFFFF9800)": "OrangeBadge",
    "Color(0xFFFFD54F)": "PrimaryYellow",
    "Color(0xFFFFEBEE)": "RedDangerBg",
    "Color(0xFFFFF0F0)": "LightRedReviewBg",
    "Color(0xFFFFF3E0)": "OrangeBadgeBg",
    "Color(0xFFFFFFFF)": "SurfaceWhite"
}

def process_file(filepath):
    if "Color.kt" in filepath or "Theme.kt" in filepath:
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    original = content
        
    # Remove local color val declarations like "val AppBlue = Color(0xFF29B6F6)"
    # We match "val AnyName = Color(0x...)" or similar
    pattern = re.compile(r"^[ \t]*val\s+\w+\s*=\s*Color\s*\(\s*0x[0-9A-Fa-f]+\s*\).*\n?", re.MULTILINE)
    content = pattern.sub('', content)
    
    # Replace inline color instances with the mapped value
    for k, v in color_map.items():
        # Match Color(0xFF...) exactly (ignoring spaces is safer but string match is fine based on PS output)
        content = content.replace(k, v)
        
    if original != content:
        # Check if we need to add the import
        if "import com.example.unimarket.presentation.theme.*" not in content:
            # Add after the package declaration
            pkg_pattern = re.compile(r"^(package .*?\n)", re.MULTILINE)
            content = pkg_pattern.sub(r"\1\nimport com.example.unimarket.presentation.theme.*\n", content)
            
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Modified: {filepath}")

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

print("Done Refactoring colors!")
