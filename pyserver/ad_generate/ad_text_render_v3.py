import os, sys, json, argparse, math, glob, re, random
from typing import Tuple, Dict, Optional, List
from PIL import Image, ImageDraw, ImageFont, ImageOps, ImageFilter

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
    if x1 < x0:
        x0, x1 = x1, x0
    if y1 < y0:
        y0, y1 = y1, y0
    return x0, y0, x1, y1

def detect_and_to_px(bbox: List[float], W: int, H: int) -> Tuple[int,int,int,int]:
    if len(bbox) != 4:
        raise ValueError("bbox must have 4 numbers")
    x, y, a, b = bbox
    is_pixels = any(v > 1.0 for v in bbox)

    def as_xyxy(xx, yy, ww, hh, pixels: bool):
        if pixels:
            return clamp_box(xx, yy, ww, hh, W, H)
        else:
            return clamp_box(xx*W, yy*H, ww*W, hh*H, W, H)

    def as_xywh(xx, yy, ww, hh, pixels: bool):
        if pixels:
            return clamp_box(xx, yy, xx+ww, yy+hh, W, H)
        else:
            return clamp_box(xx*W, yy*H, (xx+ww)*W, (yy+hh)*H, W, H)

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
    x0, y0, x1, y1 = box
    if x1 <= x0 or y1 <= y0:
        return 0.5
    crop = img.crop((x0, y0, x1, y1)).convert("RGB")
    pixels = crop.resize((32, 32), Image.LANCZOS).getdata()
    def _linear(c):
        c = c / 255.0
        return c/12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    s = 0.0
    for r, g, b in pixels:
        R, G, B = _linear(r), _linear(g), _linear(b)
        L = 0.2126*R + 0.7152*G + 0.0722*B
        s += L
    return s / (32*32)

def choose_text_and_stroke(bg_luma: float) -> Tuple[Tuple[int,int,int], Tuple[int,int,int]]:
    if bg_luma >= 0.6:
        return (0,0,0), (255,255,255)
    else:
        return (255,255,255), (0,0,0)

# -----------------------------
# Underlay / Glass
# -----------------------------

def draw_underlay(draw: ImageDraw.ImageDraw, box, radius_px: int, fill_rgba: Tuple[int,int,int,int]):
    draw.rounded_rectangle(box, radius=radius_px, fill=fill_rgba)

def glass_underlay(base: Image.Image, box, radius=16, blur=6, tint=(17,20,24,115)):
    x0, y0, x1, y1 = [int(v) for v in box]
    x0, y0, x1, y1 = clamp_box(x0, y0, x1, y1, *base.size)
    if x1 <= x0 or y1 <= y0:
        return
    region = base.crop((x0, y0, x1, y1)).filter(ImageFilter.GaussianBlur(blur))
    base.paste(region, (x0, y0))
    d = ImageDraw.Draw(base, "RGBA")
    d.rounded_rectangle((x0, y0, x1, y1), radius=radius, fill=tint)

# -----------------------------
# Text wrapping & fitting
# -----------------------------

def wrap_text_to_width(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont, max_w: int, mode: str) -> List[str]:
    mode = mode.lower()
    if mode == 'auto':
        mode = 'word' if (' ' in text) else 'char'
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
        if cur:
            lines.append(cur)
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
        if cur:
            lines.append(cur)
    return lines

def fit_text_in_box(draw: ImageDraw.ImageDraw, text: str, font_path: str, box,
                    target_ratio=0.82, max_try=96, min_size=14, line_spacing=1.02, align="center", wrap_mode='auto'):
    x0, y0, x1, y1 = box
    W = x1 - x0
    H = y1 - y0
    if (not text) or W <= 1 or H <= 1:
        return None, None, None

    lo, hi = min_size, max_try
    best = (min_size, [text])
    while lo <= hi:
        mid = (lo + hi) // 2
        font = ImageFont.truetype(font_path, mid)
        usable_w = int(W * target_ratio)
        lines = wrap_text_to_width(draw, text, font, usable_w, wrap_mode)

        total_h = 0
        line_metrics = []
        for ln in lines:
            _, _, tw, th = draw.textbbox((0,0), ln, font=font)
            line_metrics.append((tw, th))
            total_h += th
        if line_metrics:
            total_h = int(total_h + (len(lines)-1) * (line_metrics[0][1] * (line_spacing - 1)))

        if total_h <= H * target_ratio:
            best = (mid, lines)
            lo = mid + 1
        else:
            hi = mid - 1

    size, lines = best
    font = ImageFont.truetype(font_path, size)

    line_heights = [draw.textbbox((0,0), ln, font=font)[3] for ln in lines]
    text_block_h = int(sum(line_heights) + (len(lines)-1) * (line_heights[0] * (line_spacing - 1))) if lines else 0
    cur_y = y0 + max(0, (H - text_block_h)//2)

    line_boxes: List[Tuple[str, Tuple[int,int], Tuple[int,int]]] = []
    for ln in lines:
        _, _, tw, th = draw.textbbox((0,0), ln, font=font)
        if align == 'center':
            tx = x0 + (W - tw)//2
        elif align == 'left':
            tx = x0
        else:
            tx = x1 - tw
        line_boxes.append((ln, (tx, cur_y), (tw, th)))
        cur_y += int(th * line_spacing)
    return font, line_boxes, size

# -----------------------------
# Logo placement
# -----------------------------

def place_logo(base: Image.Image, logo_path: str, box, keep_aspect=True):
    if not logo_path or not os.path.exists(logo_path):
        return
    x0, y0, x1, y1 = box
    W, H = x1 - x0, y1 - y0
    if W <= 0 or H <= 0:
        return
    logo = Image.open(logo_path).convert("RGBA")
    lw, lh = logo.size
    if keep_aspect and lw > 0 and lh > 0:
        scale = min(W / lw, H / lh)
        nw, nh = max(1, int(lw * scale)), max(1, int(lh * scale))
    else:
        nw, nh = max(1, W), max(1, H)
    logo = logo.resize((nw, nh), Image.LANCZOS)
    px = x0 + (W - nw)//2
    py = y0 + (H - nh)//2
    base.alpha_composite(logo, (px, py))

# -----------------------------
# Robust loaders
# -----------------------------

def load_copy_map(path: Optional[str]) -> Dict[str, str]:
    if not path or not os.path.exists(path):
        print("[i] copy.json 생략됨 → 빈 매핑으로 진행")
        return {}
    try:
        with open(path, 'r', encoding='utf-8-sig') as f:
            raw = f.read()
        if not raw.strip():
            print(f"[i] {path} 가 비어있음 → 빈 매핑으로 진행")
            return {}
        txt = re.sub(r"/\*.*?\*/", "", raw, flags=re.S)
        txt = re.sub(r"//.*", "", txt)
        txt = re.sub(r",\s*(\]|})", r"\\1", txt)
        return json.loads(txt)
    except json.JSONDecodeError as e:
        print(f"[경고] copy.json 파싱 실패: {e} → 빈 매핑으로 계속")
        return {}
    except Exception as e:
        print(f"[경고] copy.json 읽기 오류: {e} → 빈 매핑으로 계속")
        return {}

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
        if hits:
            return hits[0]
    for p in CANDIDATE_FONTS:
        if p and os.path.exists(p):
            return p
    raise FileNotFoundError(
        "한글 폰트를 찾지 못했습니다. --font_kor 로 실제 파일(.ttf/.otf)을 지정하거나 'C:\\Windows\\Fonts\\malgunbd.ttf' 등을 사용하세요.")

# -----------------------------
# Design Context (감성 기반)
# -----------------------------

FONT_STYLE_MAP = {
    "따뜻": ["NanumBrushScript-Regular.ttf", "EastSeaDokdo-Regular.ttf"],
    "친근": ["Jua-Regular.ttf", "DoHyeon-Regular.ttf"],
    "고급": ["Diphylleia-Regular.ttf", "GrandifloraOne-Regular.ttf"],
    "프리미엄": ["Diphylleia-Regular.ttf", "GrandifloraOne-Regular.ttf"],
    "미니멀": ["NanumGothic-Regular.ttf", "NotoSansKR-Regular.ttf"],
    "모던": ["NanumGothic-Regular.ttf", "NotoSansKR-Regular.ttf"],
    "역동": ["Gugi-Regular.ttf"],
}

STYLE_PROFILE = {
    "따뜻": {"font_softness": 0.9, "contrast_pref": 0.4, "serif_pref": 0.2},
    "친근": {"font_softness": 0.8, "contrast_pref": 0.5, "serif_pref": 0.1},
    "고급": {"font_softness": 0.3, "contrast_pref": 0.8, "serif_pref": 0.9},
    "프리미엄": {"font_softness": 0.4, "contrast_pref": 0.9, "serif_pref": 0.8},
    "미니멀": {"font_softness": 0.6, "contrast_pref": 0.6, "serif_pref": 0.2},
    "모던": {"font_softness": 0.5, "contrast_pref": 0.7, "serif_pref": 0.4},
    "역동": {"font_softness": 0.2, "contrast_pref": 1.0, "serif_pref": 0.1},
}

def compute_style_scores(style_text: str) -> Dict[str, float]:
    scores = {"font_softness": 0, "contrast_pref": 0, "serif_pref": 0}
    hits = 0
    for key, vals in STYLE_PROFILE.items():
        if key in style_text:
            for k in scores:
                scores[k] += vals[k]
            hits += 1
    if hits > 0:
        for k in scores:
            scores[k] /= hits
    else:
        scores = {"font_softness": 0.5, "contrast_pref": 0.5, "serif_pref": 0.5}
    return scores

def luminance(hex_color: str) -> float:
    rgb = tuple(int(hex_color[i:i+2], 16) / 255 for i in (1, 3, 5))
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]

def adjust_lightness(hex_color: str, factor: float) -> str:
    hex_color = hex_color.strip()
    r = int(hex_color[1:3], 16)
    g = int(hex_color[3:5], 16)
    b = int(hex_color[5:7], 16)
    r = int(max(0, min(255, r * factor)))
    g = int(max(0, min(255, g * factor)))
    b = int(max(0, min(255, b * factor)))
    return f"#{r:02x}{g:02x}{b:02x}"

def choose_font_from_style(style_text: str) -> str:
    candidates: List[str] = []
    for key, fonts in FONT_STYLE_MAP.items():
        if key in style_text:
            candidates.extend(fonts)
    if not candidates:
        candidates = ["NotoSansKR-Regular.otf"]
    return random.choice(candidates)

def choose_color_from_palette(palette: List[str], style_text: str, lighting_type: str) -> Tuple[str, str]:
    if not palette:
        return ("#FFFFFF", "#000000")

    if "따뜻" in style_text or "친근" in style_text:
        palette = [c for c in palette if "9b" in c or "a4" in c or "8c" in c] or palette
    elif "프리미엄" in style_text or "고급" in style_text:
        palette = [c for c in palette if "c2" in c or "ab" in c] or palette
    elif "미니멀" in style_text or "모던" in style_text:
        palette = [c for c in palette if "bb" in c or "b3" in c] or palette

    palette_sorted = sorted(palette, key=luminance)
    text_color = palette_sorted[-1]
    stroke_color = palette_sorted[0]

    light_factor = 1.0
    if lighting_type == "soft":
        light_factor = 1.05
    elif lighting_type == "bright":
        light_factor = 1.15
    elif lighting_type == "dark":
        light_factor = 0.85

    text_color = adjust_lightness(text_color, light_factor)
    stroke_color = adjust_lightness(stroke_color, 1.0 / light_factor)

    return (text_color, stroke_color)

def load_design_context(json_path: str) -> Dict:
    if not os.path.exists(json_path):
        raise FileNotFoundError(f"Context file not found: {json_path}")

    with open(json_path, "r", encoding="utf-8") as f:
        ctx = json.load(f)

    style_text = ctx.get("background", {}).get("style", "")
    palette = ctx.get("background", {}).get("palette", [])
    lighting_type = ctx.get("background", {}).get("lighting", {}).get("type", "soft")

    style_scores = compute_style_scores(style_text)

    if style_scores["serif_pref"] >= 0.7:
        fallback = "NotoSerifKR-Bold.otf"
    elif style_scores["font_softness"] >= 0.7:
        fallback = "NanumSquareRound.ttf"
    else:
        fallback = "Pretendard-Regular.otf"

    chosen_font = choose_font_from_style(style_text) or fallback
    text_color, stroke_color = choose_color_from_palette(palette, style_text, lighting_type)

    return {
        "font_path": chosen_font,
        "text_color": text_color,
        "stroke_color": stroke_color,
        "style_scores": style_scores
    }

# -----------------------------
# Main
# -----------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image", required=True, help="Stage3/4 결과 이미지 경로")
    ap.add_argument("--layout_json", required=True, help="레이아웃 JSON 경로")
    ap.add_argument("--copy_json", required=False, help="문구 매핑 JSON (type#index -> text)")
    ap.add_argument("--font_kor", required=False, help="한국어 폰트 파일 경로(.ttf/.otf)")
    ap.add_argument("--logo_path", required=False, help="로고 PNG 경로(선택)")
    ap.add_argument("--out", default="final_ad.png")
    ap.add_argument("--stroke", type=int, default=1, help="텍스트 외곽선 두께")
    ap.add_argument("--underlay_color", default=None, help="언더레이 색상(hex, 예:#111418). 없으면 자동")
    ap.add_argument("--underlay_opacity", type=float, default=None, help="언더레이 불투명도(0~1) override")
    ap.add_argument("--target_ratio", type=float, default=0.82, help="텍스트 폭/높이 여유 비율")
    ap.add_argument("--line_spacing", type=float, default=1.02, help="줄간 간격 배수")
    ap.add_argument("--wrap_mode", choices=["auto","word","char"], default="auto")
    ap.add_argument("--glass_underlay", action='store_true', help="텍스트 영역에 유리(블러) 패널 적용")
    ap.add_argument("--glass_blur", type=int, default=6, help="유리 패널 블러 강도")
    ap.add_argument("--glass_alpha", type=float, default=0.45, help="유리 패널 틴트 알파(0~1)")
    ap.add_argument("--shrink_underlay_to_text", action='store_true', help="언더레이를 텍스트 폭+패딩으로 축소")
    ap.add_argument("--skip_layout_underlays", action='store_true', help="layout의 underlay 박스 그리지 않음")
    ap.add_argument("--debug_boxes", action='store_true', help="각 bbox 테두리 표시")
    ap.add_argument("--design_context", required=False, help="감성 기반 디자인 컨텍스트 JSON 경로")
    args = ap.parse_args()

    base = Image.open(args.image).convert("RGBA")
    W, H = base.size
    draw = ImageDraw.Draw(base, "RGBA")

    with open(args.layout_json, 'r', encoding='utf-8-sig') as f:
        meta = json.load(f)

    copy_map: Dict[str, str] = load_copy_map(args.copy_json)

    layout = meta.get("layout", {}) or {}
    nongraphics = layout.get("nongraphic_layout", []) or []
    graphics = layout.get("graphic_layout", []) or []

    # 디자인 컨텍스트 적용 (자동 폰트/색상 선택)
    override_text_color = None
    override_stroke_color = None
    if args.design_context:
        try:
            design_ctx = load_design_context(args.design_context)
            # 폰트 경로를 fonts 폴더와 합침
            FONT_DIR = r"C:\Users\qkddl\Documents\GitHub\project\pyserver\ad_generate\fonts"
            font_path = os.path.join(FONT_DIR, design_ctx["font_path"])
            # override 색상 (hex → rgb tuple)
            override_text_color = hex_to_rgb(design_ctx["text_color"])
            override_stroke_color = hex_to_rgb(design_ctx["stroke_color"])
            print(f"[디자인 컨텍스트] font: {font_path}, text_color: {design_ctx['text_color']}, stroke_color: {design_ctx['stroke_color']}")
        except Exception as e:
            print(f"[경고] design_context 로드 실패: {e}")
            font_path = resolve_font_path(args.font_kor)
    else:
        font_path = resolve_font_path(args.font_kor)


    try:
        _ = ImageFont.truetype(font_path, 18)
    except OSError as e:
        raise SystemExit(f"[폰트 오류] '{font_path}' 로드 실패: {e}")

    # 1) Layout-provided UNDERLAYS first (optional)
    if not args.skip_layout_underlays:
        for g in graphics:
            gtype = (g.get("type") or '').lower()
            bbox = g.get("bbox")
            if gtype != 'underlay' or not (isinstance(bbox, list) and len(bbox)==4):
                continue
            x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
            w, h = box_size_xyxy(x0, y0, x1, y1)
            style = g.get("style", {}) or {}
            radius = style.get("radius", 0.08)
            opacity = style.get("opacity", 0.6)
            if args.underlay_opacity is not None:
                opacity = args.underlay_opacity
            radius_px = max(2, int(min(w, h) * radius))
            if args.underlay_color:
                ur, ug, ub = hex_to_rgb(args.underlay_color)
            else:
                luma = avg_luma(base, (x0, y0, x1, y1))
                ur, ug, ub = ((255,255,255) if luma < 0.5 else (0,0,0))
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
        if not text:
            continue
        bbox = t.get("bbox")
        if not (isinstance(bbox, list) and len(bbox) == 4):
            continue

        x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
        if args.debug_boxes:
            draw.rectangle((x0, y0, x1, y1), outline=(255,0,0,128), width=1)

        # 텍스트 색상 / 외곽선 색상 결정 (override 가능)
        if override_text_color is not None and override_stroke_color is not None:
            txt_col = override_text_color
            stroke_col = override_stroke_color
        else:
            luma = avg_luma(base, (x0, y0, x1, y1))
            txt_col, stroke_col = choose_text_and_stroke(luma)

        font, line_boxes, size = fit_text_in_box(
            draw, text, font_path, (x0, y0, x1, y1),
            target_ratio=args.target_ratio,
            max_try=112, min_size=14,
            line_spacing=args.line_spacing,
            align='center', wrap_mode=args.wrap_mode
        )
        if not font:
            continue

        pad = 12
        if line_boxes:
            tx0 = min(tx for _, (tx, ty), (tw, th) in line_boxes)
            ty0 = min(ty for _, (tx, ty), (tw, th) in line_boxes)
            tx1 = max(tx + tw for _, (tx, ty), (tw, th) in line_boxes)
            ty1 = max(ty + th for _, (tx, ty), (tw, th) in line_boxes)
        else:
            tx0, ty0, tx1, ty1 = x0, y0, x1, y1
        ux0, uy0, ux1, uy1 = clamp_box(tx0 - pad, ty0 - pad, tx1 + pad, ty1 + pad, W, H)

        if args.glass_underlay:
            glass_alpha = clamp(args.glass_alpha, 0, 1)
            tint = (17,20,24, int(glass_alpha * 255))
            glass_underlay(base, (ux0, uy0, ux1, uy1), radius=16, blur=args.glass_blur, tint=tint)
        elif args.shrink_underlay_to_text:
            if args.underlay_color:
                ur, ug, ub = hex_to_rgb(args.underlay_color)
            else:
                luma_u = avg_luma(base, (ux0, uy0, ux1, uy1))
                ur, ug, ub = ((255,255,255) if luma_u < 0.5 else (0,0,0))
            opacity = 0.42 if args.underlay_opacity is None else args.underlay_opacity
            draw_underlay(draw, (ux0, uy0, ux1, uy1), radius_px=16, fill_rgba=(ur, ug, ub, int(clamp(opacity, 0, 1) * 255)))

        for ln, (tx, ty), (tw, th) in line_boxes:
            draw.text((tx, ty), ln, font=font, fill=txt_col + (255,),
                      stroke_width=max(0, args.stroke), stroke_fill=stroke_col + (255,))

    # 3) LOGO from graphic_layout
    for g in graphics:
        if (g.get("type") or '').lower() != 'logo':
            continue
        if not args.logo_path:
            continue
        bbox = g.get("bbox")
        if not (isinstance(bbox, list) and len(bbox) == 4):
            continue
        x0, y0, x1, y1 = detect_and_to_px(bbox, W, H)
        place_logo(base, args.logo_path, (x0, y0, x1, y1))

    base.convert("RGB").save(args.out, quality=95)
    print(f"✅ 저장 완료: {args.out}")

if __name__ == "__main__":
    main()
