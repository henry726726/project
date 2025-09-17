from pydantic import BaseModel
from typing import Optional
from fastapi import UploadFile

class GenerateRequest(BaseModel):
    product_name: str

# 업로드 파일은 FormData로 다룰 예정이라 따로 모델에서 처리 X
