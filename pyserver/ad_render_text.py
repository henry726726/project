# ad_text_render_module.py

import os, sys, json, re, io
from typing import Tuple, Dict, Optional, List
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, glob
import base64

# -----------------------------
# Color / geometry helpers
# -----------------------------

def hex_to_rgb(hex_str: str) -> Tuple[int, int, int]:
    s = hex_str.lstrip('#')
    if len(s) == 3:
        s = ''.join([c*2 for c in s])
    if len(s) != 6:
        raise ValueError(f"Invalid hex color: {hex_str}")
    return tuple(int(s[i:i+2], 16) for i in (0,2,4))

def clamp(v, lo, hi):
    return max(lo, min(hi, v))

def clamp_box(x0, y0, x1, y1, W, H):
    x0 = clamp(int(round(x0)), 0, W)
    y0 = clamp(int(round(y0)), 0, H)
    x1 = clamp(int(round(x1)), 0, W)
    y1 = clamp(int(round(y1)), 0, H)
    if x1 < x0: x0, x1 = x1, x0
    if y1 < y0: y1, y0 = y1, y0
    return x0, y0, x1, y1

def detect_and_to_px(bbox: List[float], W: int, H: int) -> Tuple[int, int, int, int]:
    if len(bbox) != 4:
        raise ValueError("bbox must have 4 numbers")
    x, y, a, b = bbox
    is_pixels = any(v > 1.0 for v in bbox)
    def as_xyxy(xx, yy, ww, hh, pixels: bool):
        if pixels: return clamp_box(xx, yy, ww, hh, W, H)
        else: return clamp_box(xx*W, yy*H, ww*W, hh*H, W, H)
    def as_xywh(xx, yy, ww, hh, pixels: bool):
        if pixels: return clamp_box(xx, yy, xx+ww, yy+hh, W, H)
        else: return clamp_box(xx*W, yy*H, (xx+ww)*W, (yy+hh)*H, W, H)
    if (not is_pixels) and (a > x) and (b > y) and (a <= 1.0) and (b <= 1.0):
        return as_xyxy(x, y, a, b, False)
    if is_pixels and (a > x) and (b > y):
        return as_xyxy(x, y, a, b, True)
    return as_xywh(x, y, a, b, is_pixels)

def box_size_xyxy(x0, y0, x1, y1):
    return (x1 - x0, y1 - y0)

# -----------------------------
# Luma / color choice
# -----------------------------

def avg_luma(img: Image.Image, box) -> float:
    x0,y0,x1,y1 = box
    if x1<=x0 or y1<=y0: return 0.5
    crop = img.crop((x0,y0,x1,y1)).convert("RGB")
    pixels = crop.resize((32, 32), Image.LANCZOS).getdata()
    def _linear(c):
        c = c / 255.0
        return c/12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    s = 0.0
    for r,g,b in pixels:
        R, G, B = _linear(r), _linear(g), _linear(b)
        L = 0.2126*R + 0.7152*G + 0.0722*B
        s += L
    return s / (32*32)

def choose_text_and_stroke(bg_luma: float):
    if bg_luma >= 0.6: return (0,0,0), (255,255,255)
    else: return (255,255,255), (0,0,0)

# -----------------------------
# Underlay / Glass
# -----------------------------

def draw_underlay(draw: ImageDraw.ImageDraw, box, radius_px: int, fill_rgba: Tuple[int,int,int,int]):
    draw.rounded_rectangle(box, radius=radius_px, fill=fill_rgba)

def glass_underlay(base: Image.Image, box, radius=16, blur=6, tint=(17,20,24,115)):
    x0,y0,x1,y1 = [int(v) for v in box]
    x0,y0,x1,y1 = clamp_box(x0,y0,x1,y1,*base.size)
    if x1<=x0 or y1<=y0: return
    region = base.crop((x0,y0,x1,y1)).filter(ImageFilter.GaussianBlur(blur))
    base.paste(region, (x0,y0))
    d = ImageDraw.Draw(base, "RGBA")
    d.rounded_rectangle((x0,y0,x1,y1), radius=radius, fill=tint)

# -----------------------------
# Text wrapping & fitting
# -----------------------------

def wrap_text_to_width(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont, max_w: int, mode: str) -> List[str]:
    mode = mode.lower()
    if mode == 'auto': mode = 'word' if (' ' in text) else 'char'
    lines = []
    if mode == 'word':
        words = text.split()
        cur = ''
        for w in words:
            test = (cur + ' ' + w).strip() if cur else w
            tw = draw.textbbox((0,0), test, font=font)[2]
            if tw <= max_w:
                cur = test
            else:
                if cur:
                    lines.append(cur)
                    cur = w
                else:
                    cur = w
        if cur: lines.append(cur)
    else:
        cur = ''
        for ch in list(text):
            test = cur + ch
            tw = draw.textbbox((0,0), test, font=font)[2]
            if tw <= max_w or cur == '':
                cur = test
            else:
                lines.append(cur)
                cur = ch
        if cur: lines.append(cur)
    return lines

def fit_text_in_box(draw: ImageDraw.ImageDraw, text: str, font_path: str, box,
                    target_ratio=0.82, max_try=96, min_size=14, line_spacing=1.02, align="center", wrap_mode='auto'):
    x0,y0,x1,y1 = box
    W, H = x1 - x0, y1 - y0
    if not text or W<=1 or H<=1: return None, None, None
    lo, hi = min_size, max_try
    best = (min_size, [text])
    while lo <= hi:
        mid = (lo + hi) // 2
        font = ImageFont.truetype(font_path, mid)
        usable_w = int(W * target_ratio)
        lines = wrap_text_to_width(draw, text, font, usable_w, wrap_mode)
        total_h = sum(draw.textbbox((0,0), ln, font=font)[3] for ln in lines)
        if len(lines) > 1:
            total_h += (len(lines)-1) * (draw.textbbox((0,0), lines[0], font=font)[3] * (line_spacing-1))
        if total_h <= H * target_ratio:
            best = (mid, lines)
            lo = mid + 1
        else:
            hi = mid - 1
    size, lines = best
    font = ImageFont.truetype(font_path, size)
    line_heights = [draw.textbbox((0,0), ln, font=font)[3] for ln in lines]
    text_block_h = int(sum(line_heights) + (len(lines)-1) * (line_heights[0]*(line_spacing-1))) if lines else 0
    cur_y = y0 + max(0, (H - text_block_h)//2)
    line_boxes = []
    for ln in lines:
        _,_,tw,th = draw.textbbox((0,0), ln, font=font)
        if align == 'center': tx = x0 + (W - tw)//2
        elif align == 'left': tx = x0
        else: tx = x1 - tw
        line_boxes.append((ln, (tx, cur_y), (tw, th)))
        cur_y += int(th * line_spacing)
    return font, line_boxes, size

# -----------------------------
# Logo placement
# -----------------------------

def place_logo(base: Image.Image, logo_data: bytes, box, keep_aspect=True):
    if not logo_data: return
    x0,y0,x1,y1 = box
    W,H = x1-x0, y1-y0
    if W<=0 or H<=0: return
    logo = Image.open(io.BytesIO(logo_data)).convert("RGBA")
    lw, lh = logo.size
    if keep_aspect and lw>0 and lh>0:
        scale = min(W/lw, H/lh)
        nw, nh = max(1, int(lw*scale)), max(1, int(lh*scale))
    else:
        nw, nh = max(1, W), max(1, H)
    logo = logo.resize((nw, nh), Image.LANCZOS)
    px = x0 + (W - nw)//2
    py = y0 + (H - nh)//2
    base.alpha_composite(logo, (px, py))

# -----------------------------
# Robust loaders
# -----------------------------

def load_copy_map(copy_data: dict) -> Dict[str,str]:
    if not copy_data:
        return {}
    return copy_data

CANDIDATE_FONTS = [
    r"C:\\Windows\\Fonts\\malgunbd.ttf",
    r"C:\\Windows\\Fonts\\malgun.ttf",
    r"C:\\Windows\\Fonts\\NanumGothic.ttf",
    os.path.expandvars(r"%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts\\NotoSansKR-Bold.otf"),
    os.path.expandvars(r"%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts\\NotoSansKR-Regular.otf"),
]

def resolve_font_path(requested_path: Optional[str]) -> str:
    if requested_path and os.path.exists(requested_path):
        return requested_path
    for pattern in [
        r"C:\\Windows\\Fonts\\*Noto*Sans*KR*Bold*.otf",
        r"C:\\Windows\\Fonts\\*Noto*Sans*KR*.ttf",
        os.path.expandvars(r"%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts\\*Noto*Sans*KR*Bold*.otf"),
        os.path.expandvars(r"%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts\\*Noto*Sans*KR*.ttf"),
    ]:
        hits = glob.glob(pattern)
        if hits: return hits[0]
    for p in CANDIDATE_FONTS:
        if p and os.path.exists(p): return p
    raise FileNotFoundError("한국어 폰트 파일을 찾을 수 없습니다. 적절한 경로를 지정하거나 폰트 파일을 시스템에 설치해주세요.")

# -----------------------------
# Main Class for FastAPI Integration
# -----------------------------

class AdTextRenderer:
    def __init__(self, font_path: Optional[str] = None):
        try:
            self.font_path = resolve_font_path(font_path)
            _ = ImageFont.truetype(self.font_path, 18)
            print(f"✅ 한글 폰트 로드 성공: {self.font_path}")
        except FileNotFoundError as e:
            print(f"⛔ 한글 폰트 로드 실패: {e}")
            self.font_path = None
        except Exception as e:
            print(f"⛔ 폰트 로드 중 오류 발생: {e}")
            self.font_path = None

    def render_ad(self,
                  image_data: bytes,
                  layout_json: Dict,
                  copy_map: Dict,
                  logo_data: Optional[bytes] = None,
                  params: Optional[Dict] = None) -> bytes:
        
        if not self.font_path:
            raise RuntimeError("폰트가 제대로 로드되지 않았습니다. 텍스트 렌더링을 할 수 없습니다.")
        
        params = params or {}
        
        base = Image.open(io.BytesIO(image_data)).convert("RGBA")
        W, H = base.size
        draw = ImageDraw.Draw(base, "RGBA")

        layout = layout_json.get("layout", {}) or {}
        nongraphics = layout.get("nongraphic_layout", []) or []
        graphics = layout.get("graphic_layout", []) or []

        # 1) Layout-provided UNDERLAYS first
        for g in graphics:
            gtype = (g.get("type") or '').lower()
            bbox = g.get("bbox")
            if gtype != 'underlay' or not (isinstance(bbox, list) and len(bbox) == 4):
                continue
            x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
            w, h = box_size_xyxy(x0, y0, x1, y1)
            style = g.get("style", {}) or {}
            radius = style.get("radius", 0.08)
            opacity = style.get("opacity", 0.6)
            radius_px = max(2, int(min(w, h) * radius))
            luma = avg_luma(base, (x0, y0, x1, y1))
            ur, ug, ub = ((255, 255, 255) if luma < 0.5 else (0, 0, 0))
            ua = int(clamp(opacity, 0, 1) * 255)
            draw_underlay(draw, (x0, y0, x1, y1), radius_px, (ur, ug, ub, ua))

        # 2) TEXTS (headline/subhead/etc.)
        type_counts: Dict[str, int] = {}
        for t in nongraphics:
            ttype = (t.get("type") or 'text').lower()
            idx = type_counts.get(ttype, 0)
            type_counts[ttype] = idx + 1
            key = f"{ttype}#{idx}"
            text = copy_map.get(key, '')
            if not text: continue
            bbox = t.get("bbox")
            if not (isinstance(bbox, list) and len(bbox) == 4): continue

            x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
            luma = avg_luma(base, (x0, y0, x1, y1))
            txt_col, stroke_col = choose_text_and_stroke(luma)

            font, line_boxes, size = fit_text_in_box(
                draw, text, self.font_path, (x0, y0, x1, y1),
                target_ratio=params.get("target_ratio", 0.82),
                line_spacing=params.get("line_spacing", 1.02)
            )
            if not font: continue

            # Render text lines
            for ln, (tx, ty), (tw, th) in line_boxes:
                draw.text((tx, ty), ln, font=font, fill=txt_col + (255,),
                          stroke_width=params.get("stroke", 1), stroke_fill=stroke_col + (255,))

        # 3) LOGO from graphic_layout (type=logo)
        if logo_data:
            for g in graphics:
                if (g.get("type") or '').lower() != 'logo': continue
                bbox = g.get("bbox")
                if not (isinstance(bbox, list) and len(bbox) == 4): continue
                x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
                place_logo(base, logo_data, (x0, y0, x1, y1))

        output_buffer = io.BytesIO()
        base.convert("RGB").save(output_buffer, format="PNG", quality=95)
        return output_buffer.getvalue()