import json, os, random
from typing import Dict, Tuple, List

# ------------------------------------------
# [1] 감성 기반 폰트 스타일 사전
# ------------------------------------------

FONT_STYLE_MAP = {
    "따뜻": ["NanumBrushScript-Regular.ttf", "EastSeaDokdo-Regular.ttf"],
    "친근": ["Jua-Regular.ttf", "DoHyeon-Regular.ttf"],
    "고급": ["Diphylleia-Regular.ttf", "GrandifloraOne-Regular.ttf"],
    "프리미엄": ["Diphylleia-Regular.ttf", "GrandifloraOne-Regular.ttf"],
    "미니멀": ["NanumGothic-Regular.ttf", "NotoSansKR-Regular.ttf"],
    "모던": ["NanumGothic-Regular.ttf", "NotoSansKR-Regular.ttf"],
    "역동": ["Gugi-Regular.ttf"],
}

# ------------------------------------------
# [2] 감성 점수화 기반 스타일 매핑 (★ 추가된 부분)
# ------------------------------------------

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
    """★ 감성 단어별 점수를 부여하여 스타일 프로필을 결정"""
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
        # 기본값 (뉴트럴)
        scores = {"font_softness": 0.5, "contrast_pref": 0.5, "serif_pref": 0.5}
    return scores


def luminance(hex_color: str) -> float:
    rgb = tuple(int(hex_color[i:i+2], 16) / 255 for i in (1, 3, 5))
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]


def adjust_lightness(hex_color: str, factor: float) -> str:
    """★ 조명에 따른 색상 밝기 조절"""
    hex_color = hex_color.strip()
    r = int(hex_color[1:3], 16)
    g = int(hex_color[3:5], 16)
    b = int(hex_color[5:7], 16)
    r = int(max(0, min(255, r * factor)))
    g = int(max(0, min(255, g * factor)))
    b = int(max(0, min(255, b * factor)))
    return f"#{r:02x}{g:02x}{b:02x}"


# ------------------------------------------
# [3] 폰트 선택 로직
# ------------------------------------------

def choose_font_from_style(style_text: str) -> str:
    """감성 텍스트 기반 폰트 선택"""
    candidates = []
    for key, fonts in FONT_STYLE_MAP.items():
        if key in style_text:
            candidates.extend(fonts)
    if not candidates:
        candidates = ["NotoSansKR-Regular.otf"]
    return random.choice(candidates)


# ------------------------------------------
# [4] 팔레트 기반 색상 선택 (★ 조명 보정 추가)
# ------------------------------------------

def choose_color_from_palette(palette: List[str], style_text: str, lighting_type: str) -> Tuple[str, str]:
    """팔레트 기반 텍스트/테두리 색상 선택 + 조명 보정"""
    if not palette:
        return ("#FFFFFF", "#000000")

    # 감성 필터링
    if "따뜻" in style_text or "친근" in style_text:
        palette = [c for c in palette if "9b" in c or "a4" in c or "8c" in c] or palette
    elif "프리미엄" in style_text or "고급" in style_text:
        palette = [c for c in palette if "c2" in c or "ab" in c] or palette
    elif "미니멀" in style_text or "모던" in style_text:
        palette = [c for c in palette if "bb" in c or "b3" in c] or palette

    palette_sorted = sorted(palette, key=luminance)
    text_color = palette_sorted[-1]
    stroke_color = palette_sorted[0]

    # ★ 조명 명도 보정
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


# ------------------------------------------
# [5] 통합 진입 함수
# ------------------------------------------

def load_design_context(json_path: str) -> Dict:
    """context.json 기반 폰트 및 색상 자동 선택"""
    if not os.path.exists(json_path):
        raise FileNotFoundError(f"Context file not found: {json_path}")

    with open(json_path, "r", encoding="utf-8") as f:
        ctx = json.load(f)

    style_text = ctx["background"].get("style", "")
    palette = ctx["background"].get("palette", [])
    lighting_type = ctx["background"].get("lighting", {}).get("type", "soft")

    # ★ 감성 점수 계산
    style_scores = compute_style_scores(style_text)

    # ★ 점수를 활용해 폰트 스타일 선택 (serif 선호도 기반)
    if style_scores["serif_pref"] >= 0.7:
        fallback = "NotoSerifKR-Bold.otf"
    elif style_scores["font_softness"] >= 0.7:
        fallback = "NanumSquareRound.ttf"
    else:
        fallback = "Pretendard-Regular.otf"

    chosen_font = choose_font_from_style(style_text) or fallback

    # 색상 선택 (+조명 보정)
    text_color, stroke_color = choose_color_from_palette(palette, style_text, lighting_type)

    return {
        "font_path": chosen_font,
        "text_color": text_color,
        "stroke_color": stroke_color,
        "style_scores": style_scores  # ★ 추가: 감성 점수 확인용
    }
