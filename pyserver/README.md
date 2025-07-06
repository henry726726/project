# FastAPI + Stable Diffusion ControlNet 서버

## 실행 방법 (처음 설정 시)

```bash
cd pyserver
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn generate_image:app --port 8000
uvicorn generate_image:app --host 0.0.0.0 --port 8000 --reload
