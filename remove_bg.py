import cv2
import numpy as np

source_image_path = r"C:\Users\User\.gemini\antigravity\brain\91dc4be2-b113-4892-93a6-700611b7a694\icon_flat_1772283566287.png"
output_path = r"C:\Users\User\.gemini\antigravity\brain\91dc4be2-b113-4892-93a6-700611b7a694\icon_flat_transparent.png"

# Read image
img = cv2.imread(source_image_path, cv2.IMREAD_UNCHANGED)

if img.shape[2] == 3:
    # Add alpha channel if it doesn't exist
    img = cv2.cvtColor(img, cv2.COLOR_BGR2BGRA)

# Convert to grayscale to find background mask
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Threshold to get mask of background
# Assuming background is white or very light color
_, mask = cv2.threshold(gray, 240, 255, cv2.THRESH_BINARY)

# Find contours
contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

# Find the largest contour which is presumably the background/shape limits
h, w = img.shape[:2]
mask_floodfill = np.zeros((h+2, w+2), np.uint8)
cv2.floodFill(img, mask_floodfill, (0,0), (0,0,0,0), (5,5,5,5), (5,5,5,5))
cv2.floodFill(img, mask_floodfill, (w-1,0), (0,0,0,0), (5,5,5,5), (5,5,5,5))
cv2.floodFill(img, mask_floodfill, (0,h-1), (0,0,0,0), (5,5,5,5), (5,5,5,5))
cv2.floodFill(img, mask_floodfill, (w-1,h-1), (0,0,0,0), (5,5,5,5), (5,5,5,5))

# Also rembg is an option if installed
try:
    from rembg import remove
    with open(source_image_path, 'rb') as i:
        with open(output_path, 'wb') as o:
            input = i.read()
            output = remove(input)
            o.write(output)
    print("Done using rembg!")
except ImportError:
    # Save the floodfilled one if rembg fails
    cv2.imwrite(output_path, img)
    print("Done using floodfill!")
