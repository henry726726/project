# FastAPI + Stable Diffusion ControlNet 서버

## 실행 방법 (처음 설정 시)

```bash
#1. pyserver만 실행 
cd pyserver
#2. 가상환경 venv 만들기 
python -m venv .venv
#3. 가상환경 활성화 
#windows
.\.venv\Scripts\Activate.ps1
#mac
source .venv/bin/activate


pip install -r requirements.txt
uvicorn generate_image:app --port 8000
uvicorn generate_image:app --host 0.0.0.0 --port 8000 --reload
