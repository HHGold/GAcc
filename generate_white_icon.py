import os
from PIL import Image, ImageDraw

source_image_path = r"c:\AI\GAcc\icon_white_bg.png"
res_dir = r"c:\AI\GAcc\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def make_circle_mask(size):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    return mask

try:
    with Image.open(source_image_path) as img:
        img = img.convert("RGBA")
        
        # Crop to square if it isn't based on the center
        width, height = img.size
        min_dim = min(width, height)
        left = (width - min_dim) / 2
        top = (height - min_dim) / 2
        right = (width + min_dim) / 2
        bottom = (height + min_dim) / 2
        square_img = img.crop((left, top, right, bottom))

        for density, size in sizes.items():
            out_dir = os.path.join(res_dir, density)
            os.makedirs(out_dir, exist_ok=True)
            
            # Standard Icon
            resized = square_img.resize((size, size), Image.Resampling.LANCZOS)
            out_path = os.path.join(out_dir, "ic_launcher.png")
            resized.save(out_path, format="PNG")
            
            # Round Icon
            round_img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
            mask = make_circle_mask(size)
            round_img.paste(resized, (0, 0), mask=mask)
            out_round_path = os.path.join(out_dir, "ic_launcher_round.png")
            round_img.save(out_round_path, format="PNG")
            
            print(f"Generated {density} icons.")
    print("White background icons successfully processed!")
except Exception as e:
    print(f"Error: {e}")
