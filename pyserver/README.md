# FastAPI + Stable Diffusion ControlNet 서버

## 실행 방법 (처음 설정 시)

```bash
cd pyserver
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn generate_image:app --port 8000
uvicorn generate_image:app --host 0.0.0.0 --port 8000 --reload
uvicorn compose_service:app --host 0.0.0.0 --port 8010 --reload



# 처음 시작시 
conda create -n qwen python=3.10 -y
conda activate qwen

# 2) vertex AI 클라이언트 설치 (vertexai 모듈 포함)
python -m pip install --upgrade google-cloud-aiplatform

# 3) (최초 1회) gcloud ADC 인증 + 프로젝트 설정
gcloud auth application-default login
gcloud config set project nano-471710

#설치 파일들
pip install --upgrade pip
pip install torch==2.5.1+cu121 torchvision==0.20.1+cu121 torchaudio==2.5.1+cu121 `
  --index-url https://download.pytorch.org/whl/cu121


pip install "git+https://github.com/huggingface/transformers"
pip install "git+https://github.com/huggingface/diffusers"
pip install accelerate qwen-vl-utils pillow



python qwen25_vl_layout_hybrid.py --image sample_ad.jpg --product_name "실버 꽃모양 목걸이" --save layout.json

python qwen25_vl_layout_hybrid.py `
  --image sample_ad.jpg `
  --product_name "실버 꽃모양 목걸이" `
  --bg_prompt `
  --max_new_tokens 1200 `
  --top_p 0.85 `
  --bg_min_chars 900 `
  --save layout.json

python qwen25_vl_layout_hybrid.py --image .\sample_ad.jpg --product_name "실버 꽃모양 목걸이" --bg_prompt --save layout_out.json



$env:GOOGLE_API_KEY = AIzaSyDHLy5RDLf0tzbEeezIWke7gqCJqSrM4jo



# 4) 실행
conda activate qwen

$env:GOOGLE_CLOUD_PROJECT="nano-471710"
$env:GOOGLE_CLOUD_LOCATION="global"
$env:GOOGLE_GENAI_USE_VERTEXAI="True"

python qwen25_vl_layout_hybrid.py --image .\sample_ad.jpg --product_name "실버 꽃모양 목걸이" --bg_prompt --save layout_out.json


python nano_banana_generate.py `
  --image sample_ad.jpg `
  --layout_json layout.json `
  --out stage3_output.png `
  --max_side 1024 `
  --model gemini-2.5-flash-image-preview


  python ad_text_render.py `
   --image stage4_output.png `
    --layout_json layout_out.json `
   --copy_json copy.json `
   --font_kor "C:\Windows\Fonts\malgunbd.ttf" `
   --out final_ad2.png `
   --skip_layout_underlays `
   --stroke 2