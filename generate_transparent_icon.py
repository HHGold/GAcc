from rembg import remove
from PIL import Image
import os

input_path = r'c:\AI\GAcc\icon.png'
output_path = r'c:\AI\GAcc\icon_transparent.png'

with open(input_path, 'rb') as i:
    with open(output_path, 'wb') as o:
        input_data = i.read()
        output_data = remove(input_data)
        o.write(output_data)

print("Background removed successfully!")

# Now generate icons from the transparent image
source_image_path = output_path
res_dir = r"c:\AI\GAcc\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

try:
    with Image.open(source_image_path) as img:
        img = img.convert("RGBA")
        
        # Crop to square if it isn't based on the center
        width, height = img.size
        # For transparent icon, we might want to trim empty space first to maximize icon size
        # Get bounding box of non-transparent pixels
        bbox = img.getbbox()
        if bbox:
            img = img.crop(bbox)
            
        # Now make it square by padding
        width, height = img.size
        max_dim = max(width, height)
        
        # Create a new square transparent image
        square_img = Image.new("RGBA", (max_dim, max_dim), (0,0,0,0))
        
        # Paste the cropped image in the center
        left = (max_dim - width) // 2
        top = (max_dim - height) // 2
        square_img.paste(img, (left, top))
        
        # Add a tiny bit of padding (e.g., 5% on each side) so it doesn't touch the very edges
        padding = int(max_dim * 0.05)
        final_dim = max_dim + padding * 2
        final_square_img = Image.new("RGBA", (final_dim, final_dim), (0,0,0,0))
        final_square_img.paste(square_img, (padding, padding))

        for density, size in sizes.items():
            out_dir = os.path.join(res_dir, density)
            os.makedirs(out_dir, exist_ok=True)
            
            # Standard Icon
            resized = final_square_img.resize((size, size), Image.Resampling.LANCZOS)
            out_path = os.path.join(out_dir, "ic_launcher.png")
            resized.save(out_path, format="PNG")
            
            # Round Icon (just copy the same since it's transparent anyway)
            out_round_path = os.path.join(out_dir, "ic_launcher_round.png")
            resized.save(out_round_path, format="PNG")
            
            print(f"Generated {density} transparent icons.")
    print("Transparent icons successfully generated!")
except Exception as e:
    print(f"Error: {e}")
