import math
from PIL import Image, ImageDraw, ImageFilter

def create_king_frame(size=1024):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    cx, cy = size / 2, size / 2
    outer_r = size * 0.40
    inner_r = size * 0.31
    
    # Outer Glow
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([cx - outer_r - 20, cy - outer_r - 20, cx + outer_r + 20, cy + outer_r + 20], fill=(255, 200, 50, 80))
    glow = glow.filter(ImageFilter.GaussianBlur(30))
    img.alpha_composite(glow)
    
    # Gold Metallic Outer Ring Layers
    for r in range(int(inner_r), int(outer_r) + 1):
        frac = (r - inner_r) / (outer_r - inner_r)
        brightness = math.sin(frac * math.pi) * 0.5 + 0.5
        r_c = int(220 + frac * 35)
        g_c = int(170 + frac * 45)
        b_c = int(30 + brightness * 120)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(r_c, g_c, b_c, 255), width=2)
        
    # Beveled Metallic Accents
    draw.ellipse([cx - inner_r, cy - inner_r, cx + inner_r, cy + inner_r], outline=(255, 240, 160, 255), width=6)
    draw.ellipse([cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r], outline=(255, 225, 90, 255), width=8)
    draw.ellipse([cx - outer_r - 5, cy - outer_r - 5, cx + outer_r + 5, cy + outer_r + 5], outline=(170, 120, 20, 255), width=3)

    # Bottom Gold Diamond & Teal Gem Ornament
    bot_y = cy + outer_r - 10
    d_size = 55
    d_pts = [(cx, bot_y - d_size), (cx + d_size, bot_y), (cx, bot_y + d_size), (cx - d_size, bot_y)]
    draw.polygon(d_pts, fill=(255, 200, 50, 255), outline=(255, 240, 160, 255))
    
    g_size = 32
    g_pts = [(cx, bot_y - g_size), (cx + g_size, bot_y), (cx, bot_y + g_size), (cx - g_size, bot_y)]
    draw.polygon(g_pts, fill=(0, 210, 180, 255), outline=(200, 255, 240, 255))
    
    # Side Diamond Gem Studs along ring
    for angle_deg in [135, 150, 165, 195, 210, 225, 315, 330, 345, 15, 30, 45]:
        rad = math.radians(angle_deg)
        sx = cx + (outer_r - 10) * math.cos(rad)
        sy = cy + (outer_r - 10) * math.sin(rad)
        draw.ellipse([sx - 6, sy - 6, sx + 6, sy + 6], fill=(0, 220, 190, 255), outline=(255, 255, 255, 255))

    # KING CROWN ON TOP
    crown_y = cy - outer_r + 25
    crown_w = 260
    crown_h = 170
    
    band_rect = [cx - crown_w*0.4, crown_y - 10, cx + crown_w*0.4, crown_y + 25]
    draw.rectangle(band_rect, fill=(240, 180, 30, 255), outline=(255, 230, 120, 255))
    
    for bx_ratio in [-0.32, -0.16, 0.0, 0.16, 0.32]:
        bx = cx + crown_w * bx_ratio
        draw.ellipse([bx - 8, crown_y + 2 - 8, bx + 8, crown_y + 2 + 8], fill=(0, 200, 170, 255), outline=(255, 255, 255, 255))

    peaks = [
        (cx - crown_w*0.45, crown_y - 10),
        (cx - crown_w*0.35, crown_y - crown_h*0.65),
        (cx - crown_w*0.18, crown_y - crown_h*0.45),
        (cx, crown_y - crown_h),
        (cx + crown_w*0.18, crown_y - crown_h*0.45),
        (cx + crown_w*0.35, crown_y - crown_h*0.65),
        (cx + crown_w*0.45, crown_y - 10),
    ]
    draw.polygon(peaks, fill=(255, 195, 40, 255), outline=(255, 240, 160, 255))
    
    tip_centers = [
        (cx - crown_w*0.35, crown_y - crown_h*0.65),
        (cx - crown_w*0.18, crown_y - crown_h*0.45),
        (cx, crown_y - crown_h),
        (cx + crown_w*0.18, crown_y - crown_h*0.45),
        (cx + crown_w*0.35, crown_y - crown_h*0.65)
    ]
    for tx, ty in tip_centers:
        draw.ellipse([tx - 15, ty - 15, tx + 15, ty + 15], fill=(255, 220, 90, 255), outline=(255, 255, 200, 255))
        draw.ellipse([tx - 7, ty - 7, tx + 7, ty + 7], fill=(0, 210, 180, 255))
        
    draw.ellipse([cx - 20, crown_y - crown_h*0.5 - 20, cx + 20, crown_y - crown_h*0.5 + 20], fill=(0, 210, 180, 255), outline=(255, 255, 220, 255))

    # Apply alpha mask to cutout inner profile circle
    orig_a = img.getchannel('A')
    cutout = Image.new('L', (size, size), 255)
    cdraw = ImageDraw.Draw(cutout)
    cdraw.ellipse([cx - inner_r + 3, cy - inner_r + 3, cx + inner_r - 3, cy + inner_r - 3], fill=0)
    
    final_a = Image.eval(orig_a, lambda p: p)
    for x in range(int(cx - inner_r + 3), int(cx + inner_r - 3)):
        for y in range(int(cy - inner_r + 3), int(cy + inner_r - 3)):
            if (x - cx)**2 + (y - cy)**2 < (inner_r - 3)**2:
                final_a.putpixel((x, y), 0)
                
    img.putalpha(final_a)
    return img

def create_queen_frame(size=1024):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    cx, cy = size / 2, size / 2
    outer_r = size * 0.40
    inner_r = size * 0.31
    
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([cx - outer_r - 20, cy - outer_r - 20, cx + outer_r + 20, cy + outer_r + 20], fill=(255, 100, 180, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(30))
    img.alpha_composite(glow)
    
    for r in range(int(inner_r), int(outer_r) + 1):
        frac = (r - inner_r) / (outer_r - inner_r)
        brightness = math.sin(frac * math.pi) * 0.5 + 0.5
        r_c = 255
        g_c = int(130 + frac * 60)
        b_c = int(180 + brightness * 75)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(r_c, g_c, b_c, 255), width=2)
        
    draw.ellipse([cx - inner_r, cy - inner_r, cx + inner_r, cy + inner_r], outline=(255, 210, 240, 255), width=6)
    draw.ellipse([cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r], outline=(255, 170, 210, 255), width=8)
    draw.ellipse([cx - outer_r - 5, cy - outer_r - 5, cx + outer_r + 5, cy + outer_r + 5], outline=(200, 90, 150, 255), width=3)

    bot_y = cy + outer_r - 10
    d_size = 55
    d_pts = [(cx, bot_y - d_size), (cx + d_size, bot_y), (cx, bot_y + d_size), (cx - d_size, bot_y)]
    draw.polygon(d_pts, fill=(255, 120, 190, 255), outline=(255, 230, 245, 255))
    
    g_size = 32
    g_pts = [(cx, bot_y - g_size), (cx + g_size, bot_y), (cx, bot_y + g_size), (cx - g_size, bot_y)]
    draw.polygon(g_pts, fill=(180, 50, 230, 255), outline=(255, 220, 255, 255))
    
    for angle_deg in [135, 150, 165, 195, 210, 225, 315, 330, 345, 15, 30, 45]:
        rad = math.radians(angle_deg)
        sx = cx + (outer_r - 10) * math.cos(rad)
        sy = cy + (outer_r - 10) * math.sin(rad)
        draw.ellipse([sx - 6, sy - 6, sx + 6, sy + 6], fill=(220, 100, 255, 255), outline=(255, 255, 255, 255))

    crown_y = cy - outer_r + 25
    crown_w = 260
    crown_h = 170
    
    band_rect = [cx - crown_w*0.4, crown_y - 10, cx + crown_w*0.4, crown_y + 25]
    draw.rectangle(band_rect, fill=(255, 140, 190, 255), outline=(255, 220, 240, 255))
    for bx_ratio in [-0.32, -0.16, 0.0, 0.16, 0.32]:
        bx = cx + crown_w * bx_ratio
        draw.ellipse([bx - 8, crown_y + 2 - 8, bx + 8, crown_y + 2 + 8], fill=(180, 60, 220, 255), outline=(255, 255, 255, 255))

    peaks = [
        (cx - crown_w*0.45, crown_y - 10),
        (cx - crown_w*0.35, crown_y - crown_h*0.65),
        (cx - crown_w*0.18, crown_y - crown_h*0.45),
        (cx, crown_y - crown_h),
        (cx + crown_w*0.18, crown_y - crown_h*0.45),
        (cx + crown_w*0.35, crown_y - crown_h*0.65),
        (cx + crown_w*0.45, crown_y - 10),
    ]
    draw.polygon(peaks, fill=(255, 150, 200, 255), outline=(255, 230, 245, 255))
    
    tip_centers = [
        (cx - crown_w*0.35, crown_y - crown_h*0.65),
        (cx - crown_w*0.18, crown_y - crown_h*0.45),
        (cx, crown_y - crown_h),
        (cx + crown_w*0.18, crown_y - crown_h*0.45),
        (cx + crown_w*0.35, crown_y - crown_h*0.65)
    ]
    for tx, ty in tip_centers:
        draw.ellipse([tx - 15, ty - 15, tx + 15, ty + 15], fill=(255, 190, 225, 255), outline=(255, 255, 255, 255))
        draw.ellipse([tx - 7, ty - 7, tx + 7, ty + 7], fill=(180, 50, 230, 255))
        
    draw.ellipse([cx - 20, crown_y - crown_h*0.5 - 20, cx + 20, crown_y - crown_h*0.5 + 20], fill=(180, 50, 230, 255), outline=(255, 240, 255, 255))

    orig_a = img.getchannel('A')
    final_a = Image.eval(orig_a, lambda p: p)
    for x in range(int(cx - inner_r + 3), int(cx + inner_r - 3)):
        for y in range(int(cy - inner_r + 3), int(cy + inner_r - 3)):
            if (x - cx)**2 + (y - cy)**2 < (inner_r - 3)**2:
                final_a.putpixel((x, y), 0)
                
    img.putalpha(final_a)
    return img

print("Generating King Frame...")
king_img = create_king_frame()
king_img.save("app/src/main/res/drawable/ic_king_gold_frame.png")

print("Generating Queen Frame...")
queen_img = create_queen_frame()
queen_img.save("app/src/main/res/drawable/ic_queen_pink_frame.png")

print("Frames generated successfully!")
