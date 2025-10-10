import os, json, argparse
from PIL import Image, ImageDraw, ImageFont
from design_context import load_design_context  # ← 감성 기반 모듈 통합

# ----------------------------------------------------------
# 유틸 함수
# ----------------------------------------------------------
def resolve_font_path(font_name: str) -> str:
    """폰트 파일명을 fonts 폴더 기준으로 절대 경로 변환"""
    if os.path.exists(font_name):
        return font_name
    font_dir = os.path.join(os.path.dirname(__file__), "fonts")
    candidate = os.path.join(font_dir, font_name)
    if not os.path.exists(candidate):
        raise FileNotFoundError(f"Font not found: {font_name}")
    return candidate


def draw_centered_text(draw, text, font, box, fill, stroke_fill=None, stroke_width=0):
    """텍스트를 중앙 정렬로 그림"""
    text_w, text_h = draw.textsize(text, font=font)
    x = box[0] + (box[2] - text_w) / 2
    y = box[1] + (box[3] - text_h) / 2
    draw.text((x, y), text, font=font, fill=fill, stroke_fill=stroke_fill, stroke_width=stroke_width)


# ----------------------------------------------------------
# 메인 실행 함수
# ----------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--context", type=str, default="context.json", help="디자인 컨텍스트 JSON 경로")
    parser.add_argument("--layout", type=str, default="layout.json")
    parser.add_argument("--copy", type=str, default="copy.json")
    parser.add_argument("--bg", type=str, default="background.png")
    parser.add_argument("--output", type=str, default="output.png")
    args = parser.parse_args()

    # ① 디자인 컨텍스트 로드
    design_ctx = load_design_context(args.context)
    font_name = design_ctx["font_path"]
    text_color = design_ctx["text_color"]
    stroke_color = design_ctx["stroke_color"]
    style_scores = design_ctx["style_scores"]

    font_path = resolve_font_path(font_name)

    print(f"[🎨 자동 스타일 매핑 결과]")
    print(f" ├ 폰트: {font_name}")
    print(f" ├ 텍스트 색상: {text_color}")
    print(f" ├ 스트로크 색상: {stroke_color}")
    print(f" └ 감성 점수: {style_scores}")

    # ② 배경 이미지 로드
    if not os.path.exists(args.bg):
        raise FileNotFoundError(f"배경 이미지 없음: {args.bg}")
    bg = Image.open(args.bg).convert("RGBA")
    W, H = bg.size
    draw = ImageDraw.Draw(bg)

    # ③ 텍스트 및 레이아웃 데이터 로드
    with open(args.copy, "r", encoding="utf-8") as f:
        copy_data = json.load(f)
    with open(args.layout, "r", encoding="utf-8") as f:
        layout_data = json.load(f)

    texts = copy_data.get("texts", [])
    layout_items = layout_data.get("layout", {}).get("nongraphic_layout", [])

    # ④ 텍스트 렌더링
    for i, layout in enumerate(layout_items):
        if i >= len(texts):
            break
        bbox = layout["bbox"]
        x, y, w_rel, h_rel = bbox
        box = (x * W, y * H, w_rel * W, h_rel * H)

        text = texts[i]
        font_size = int(box[3] * 0.8)
        font = ImageFont.truetype(font_path, font_size)

        draw_centered_text(
            draw,
            text=text,
            font=font,
            box=box,
            fill=text_color,
            stroke_fill=stroke_color,
            stroke_width=2
        )

    # ⑤ 결과 저장
    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    bg.save(args.output)
    print(f"[✅ 완료] 결과 이미지 저장: {args.output}")


if __name__ == "__main__":
    main()
