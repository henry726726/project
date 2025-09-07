# qwen_image_edit_apply.py
import argparse, os, json
from PIL import Image, ImageDraw
import torch
from diffusers import QwenImageEditPipeline

def rect_underlay(img, bbox_norm, opacity=160):
    w, h = img.size
    x, y, bw, bh = bbox_norm
    x1, y1 = int(x*w), int(y*h)
    x2, y2 = int((x+bw)*w), int((y+bh)*h)
    overlay = Image.new("RGBA", img.size, (0,0,0,0))
    draw = ImageDraw.Draw(overlay)
    draw.rectangle([x1,y1,x2,y2], fill=(0,0,0,opacity))
    return Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image", required=True)
    ap.add_argument("--layout_json", required=True)  # 1)단계에서 저장한 JSON 경로
    ap.add_argument("--prompt", default="전체적으로 고급스러운 분위기, 제품이 돋보이도록 노이즈 제거와 색감 보정")
    ap.add_argument("--steps", type=int, default=30)
    args = ap.parse_args()

    base = Image.open(args.image).convert("RGB")

    # 필요 시, nongraphic/underlay 영역에 반투명 박스 먼저 깔기(가독성용)
    layout = json.load(open(args.layout_json, "r", encoding="utf-8-sig"))
    underlays = []
    if "nongraphic_layout" in layout:
        underlays += [x["bbox"] for x in layout["nongraphic_layout"][:1]]  # 예: 첫 박스만 언더레이
    img_for_edit = base
    for b in underlays:
        img_for_edit = rect_underlay(img_for_edit, b, opacity=140)

    pipe = QwenImageEditPipeline.from_pretrained("Qwen/Qwen-Image-Edit")
    pipe.to(torch.bfloat16)
    pipe.to("cuda")

    edited = pipe(
        image=img_for_edit,
        prompt=args.prompt,
        true_cfg_scale=3.5,
        num_inference_steps=args.steps,
        generator=torch.manual_seed(0),
        negative_prompt="low quality, artifacts"
    ).images[0]

    out = "edited.png"
    edited.save(out)
    print("saved:", os.path.abspath(out))

if __name__ == "__main__":
    main()
