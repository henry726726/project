from fastapi import FastAPI
from pydantic import BaseModel
from konlpy.tag import Okt

app = FastAPI()
okt = Okt()

keywords = ["목걸이", "반지", "귀걸이", "시계", "지갑", "가방", "신발", "자켓"]

class Request(BaseModel):
    text: str

@app.post("/extract-keyword")
def extract_keyword(req: Request):
    text = req.text.strip()  # 앞뒤 공백 제거
    
    # 1) 형태소 분석 기반 매칭
    tokens = okt.morphs(text)  
    for token in tokens:
        if token in keywords:
            return {"keyword": token}
    
    # 2) substring 기반 매칭 (혹시 못 잡았을 때)
    for keyword in keywords:
        if keyword in text:
            return {"keyword": keyword}
    
    # 3) 기본값
    return {"keyword": "기타"}
