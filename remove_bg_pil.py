from PIL import Image

source_image_path = r"C:\Users\User\.gemini\antigravity\brain\91dc4be2-b113-4892-93a6-700611b7a694\icon_flat_1772283566287.png"
output_path = r"C:\Users\User\.gemini\antigravity\brain\91dc4be2-b113-4892-93a6-700611b7a694\icon_flat_transparent.png"

try:
    img = Image.open(source_image_path).convert("RGBA")
    datas = img.getdata()

    # Get the background color from top-left pixel
    bg_color = datas[0]
    # tolerance
    tolerance = 15

    newData = []
    for item in datas:
        if (abs(item[0] - bg_color[0]) < tolerance and
            abs(item[1] - bg_color[1]) < tolerance and
            abs(item[2] - bg_color[2]) < tolerance):
            newData.append((255, 255, 255, 0)) # transparent
        else:
            newData.append(item)

    img.putdata(newData)
    img.save(output_path, "PNG")
    print("Background removed using PIL color comparison!")
except Exception as e:
    print(f"Error: {e}")
