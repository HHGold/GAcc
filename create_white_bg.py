from rembg import remove
from PIL import Image
import os

input_path = r'c:\AI\GAcc\icon.png'
transparent_path = r'c:\AI\GAcc\icon_transparent_rembg.png'
white_bg_path = r'c:\AI\GAcc\icon_white_bg.png'

# 1. Remove background
with open(input_path, 'rb') as i:
    with open(transparent_path, 'wb') as o:
        input_data = i.read()
        output_data = remove(input_data)
        o.write(output_data)

# 2. Put it on a white background
transparent_img = Image.open(transparent_path).convert("RGBA")

# Crop to bounding box to maximize the subject
bbox = transparent_img.getbbox()
if bbox:
    transparent_img = transparent_img.crop(bbox)

# Create a square white background
width, height = transparent_img.size
max_dim = max(width, height)
# Add some padding (e.g., 10%)
padding = int(max_dim * 0.1)
final_dim = max_dim + padding * 2

white_bg = Image.new("RGBA", (final_dim, final_dim), (255, 255, 255, 255))

# Paste the subject in the center
left = padding + (max_dim - width) // 2
top = padding + (max_dim - height) // 2

# The mask ensures we only paste the non-transparent parts over the white background
white_bg.paste(transparent_img, (left, top), transparent_img)

white_bg.save(white_bg_path, format="PNG")
print("White background icon generated successfully!")
