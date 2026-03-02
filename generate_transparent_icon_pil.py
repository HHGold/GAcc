from PIL import Image
import os

source_image_path = r"c:\AI\GAcc\icon.png"
res_dir = r"c:\AI\GAcc\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

try:
    img = Image.open(source_image_path).convert("RGBA")
    datas = img.getdata()

    # The background is exactly the same color as the top-left pixel
    bg_color = datas[0]
    # tolerance
    tolerance = 25

    newData = []
    for item in datas:
        if (abs(item[0] - bg_color[0]) < tolerance and
            abs(item[1] - bg_color[1]) < tolerance and
            abs(item[2] - bg_color[2]) < tolerance):
            newData.append((255, 255, 255, 0)) # transparent
        else:
            newData.append(item)

    img.putdata(newData)
    
    # Now generate icons from the transparent image
    # Crop to bounding box of non-transparent elements to maximize size
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        
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
    print("Transparent icons successfully generated from user provided image!")

except Exception as e:
    print(f"Error: {e}")
