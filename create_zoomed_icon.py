from PIL import Image

transparent_path = r'c:\AI\GAcc\icon_transparent_rembg.png'
original_path = r'c:\AI\GAcc\icon.png'
output_path = r'c:\AI\GAcc\icon_zoomed_exact.png'

transparent_img = Image.open(transparent_path).convert("RGBA")
bbox = transparent_img.getbbox()

if bbox:
    original_img = Image.open(original_path).convert("RGBA")
    
    left, top, right, bottom = bbox
    w = right - left
    h = bottom - top
    
    # To make it a perfect square that tightly fits the largest dimension of the subject
    max_dim = max(w, h)
    
    center_x = (left + right) // 2
    center_y = (top + bottom) // 2
    
    crop_left = center_x - max_dim // 2
    crop_top = center_y - max_dim // 2
    crop_right = crop_left + max_dim
    crop_bottom = crop_top + max_dim
    
    # Crop the original image (with the blue background) using these coordinates
    cropped = original_img.crop((crop_left, crop_top, crop_right, crop_bottom))
    cropped.save(output_path, "PNG")
    print("Exact zoomed icon generated!")
else:
    print("No bbox found")
