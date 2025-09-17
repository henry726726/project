import os
import uuid
import json
from PIL import Image
import requests
from typing import Dict, Any

def generate_image_from_prompt(prompt_json: Dict[str, Any]) -> str:
    """
    프롬프트 JSON을 받아서 이미지를 생성하는 함수
    
    Args:
        prompt_json (dict): Qwen에서 생성된 프롬프트 JSON
    
    Returns:
        str: 생성된 이미지 파일 경로
    """
    try:
        # 배경 프롬프트 추출
        background = prompt_json.get("background", {})
        background_prompt = background.get("prompt", "")
        negative_prompt = background.get("negative_prompt", "")
        
        # 제품 정보 추출
        product = prompt_json.get("product", {})
        product_type = product.get("type", "")
        product_material = product.get("material", "")
        product_design = product.get("design", "")
        
        # 최종 프롬프트 구성
        full_prompt = f"{background_prompt}"
        if product_type:
            full_prompt += f", {product_type}"
        if product_material:
            full_prompt += f", {product_material}"
        if product_design:
            full_prompt += f", {product_design}"
        
        # 출력 파일명 생성
        output_filename = f"generated_{uuid.uuid4()}.png"
        output_path = os.path.join("outputs", output_filename)
        os.makedirs("outputs", exist_ok=True)
        
        # 실제 이미지 생성 (여기서는 더미 이미지 생성)
        # 실제 구현에서는 Stable Diffusion, DALL-E, Midjourney 등의 API를 사용
        generated_image = create_dummy_image(full_prompt, negative_prompt)
        generated_image.save(output_path)
        
        print(f"이미지 생성 완료: {output_path}")
        return output_path
        
    except Exception as e:
        print(f"이미지 생성 중 오류 발생: {e}")
        # 오류 발생 시 기본 이미지 생성
        return create_fallback_image()

def create_dummy_image(prompt: str, negative_prompt: str = "") -> Image.Image:
    """
    더미 이미지를 생성하는 함수 (실제 구현에서는 AI 모델 사용)
    
    Args:
        prompt (str): 이미지 생성 프롬프트
        negative_prompt (str): 네거티브 프롬프트
    
    Returns:
        Image.Image: 생성된 이미지
    """
    # 512x512 크기의 기본 이미지 생성
    width, height = 512, 512
    
    # 프롬프트에 따라 색상 결정 (간단한 휴리스틱)
    if "jewelry" in prompt.lower() or "ring" in prompt.lower():
        # 보석류 - 금색 배경
        color = (255, 215, 0)  # 금색
    elif "watch" in prompt.lower():
        # 시계 - 은색 배경
        color = (192, 192, 192)  # 은색
    elif "white" in prompt.lower() or "clean" in prompt.lower():
        # 깔끔한 배경 - 흰색
        color = (255, 255, 255)  # 흰색
    else:
        # 기본 - 연한 회색
        color = (240, 240, 240)  # 연한 회색
    
    # 이미지 생성
    image = Image.new('RGB', (width, height), color)
    
    # 간단한 패턴 추가 (실제로는 AI 모델이 생성)
    from PIL import ImageDraw
    draw = ImageDraw.Draw(image)
    
    # 중앙에 원 그리기 (제품을 나타냄)
    center_x, center_y = width // 2, height // 2
    radius = min(width, height) // 6
    
    # 제품 영역
    draw.ellipse([center_x - radius, center_y - radius, 
                  center_x + radius, center_y + radius], 
                 fill=(200, 200, 200), outline=(100, 100, 100), width=2)
    
    # 텍스트 영역 표시 (레이아웃 정보 기반)
    # 상단 텍스트 영역
    draw.rectangle([50, 50, width - 50, 120], 
                   fill=(255, 255, 255, 100), outline=(0, 0, 0), width=1)
    
    # 하단 텍스트 영역
    draw.rectangle([50, height - 120, width - 50, height - 50], 
                   fill=(255, 255, 255, 100), outline=(0, 0, 0), width=1)
    
    return image

def create_fallback_image() -> str:
    """
    오류 발생 시 사용할 기본 이미지 생성
    
    Returns:
        str: 기본 이미지 파일 경로
    """
    output_filename = f"fallback_{uuid.uuid4()}.png"
    output_path = os.path.join("outputs", output_filename)
    os.makedirs("outputs", exist_ok=True)
    
    # 기본 이미지 생성
    image = Image.new('RGB', (512, 512), (240, 240, 240))
    image.save(output_path)
    
    return output_path

def call_nano_banana_api(prompt: str, negative_prompt: str = "", 
                        width: int = 512, height: int = 512) -> Image.Image:
    """
    실제 Nano Banana API를 호출하는 함수 (구현 예시)
    
    Args:
        prompt (str): 이미지 생성 프롬프트
        negative_prompt (str): 네거티브 프롬프트
        width (int): 이미지 너비
        height (int): 이미지 높이
    
    Returns:
        Image.Image: 생성된 이미지
    """
    # 실제 API 호출 코드 (예시)
    # api_url = "https://api.nanobanana.com/generate"
    # headers = {"Authorization": "Bearer YOUR_API_KEY"}
    # data = {
    #     "prompt": prompt,
    #     "negative_prompt": negative_prompt,
    #     "width": width,
    #     "height": height,
    #     "steps": 20,
    #     "guidance_scale": 7.5
    # }
    # 
    # response = requests.post(api_url, headers=headers, json=data)
    # if response.status_code == 200:
    #     image_data = response.content
    #     return Image.open(io.BytesIO(image_data))
    # else:
    #     raise Exception(f"API 호출 실패: {response.status_code}")
    
    # 현재는 더미 이미지 반환
    return create_dummy_image(prompt, negative_prompt)

if __name__ == "__main__":
    # 테스트 코드
    test_prompt = {
        "product": {
            "type": "jewelry",
            "material": "gold",
            "design": "elegant"
        },
        "background": {
            "prompt": "A minimal studio scene with clean white background",
            "negative_prompt": "busy patterns, harsh shadows"
        }
    }
    
    result_path = generate_image_from_prompt(test_prompt)
    print(f"테스트 이미지 생성: {result_path}")
