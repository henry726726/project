import pandas as pd
import numpy as np
from datetime import datetime
from sklearn.preprocessing import LabelEncoder
from sklearn.multioutput import MultiOutputRegressor
import lightgbm as lgb
from sklearn.model_selection import train_test_split
import joblib
import pymysql
import os

# --- 1. 사전학습 모델 로딩 ---
model_path = "model/lgbm_social_media.pkl"
if not os.path.exists(model_path):
    raise FileNotFoundError("사전학습 모델이 존재하지 않습니다. 먼저 train_model.py를 실행하세요.")

multi_model = joblib.load(model_path)
print("✅ 사전학습 모델 로드 완료")

# --- 2. MySQL에서 Meta 광고 데이터 불러오기 ---
db_config = {
    'host': 'database-1.c580mikw8lqh.ap-northeast-2.rds.amazonaws.com',
    'user': 'hongik1',
    'password': 'hongik1234',
    'database': 'aws_rds',
    'port': 3306
}

query = "SELECT * FROM ad_insight"
conn = pymysql.connect(**db_config)
df = pd.read_sql(query, conn)
conn.close()

print("📦 불러온 데이터 컬럼:", df.columns.tolist())

# --- 3. 타겟 및 피처 선정 ---
target_columns = ['ctr', 'cpc']
feature_columns = ['age', 'gender', 'clicks', 'impressions', 'frequency', 'reach', 'spend']

# --- 4. 결측치 처리 ---
print(f"📏 원본 데이터 크기: {df.shape}")
df.dropna(subset=target_columns + feature_columns, inplace=True)
print(f"📏 결측치 제거 후 크기: {df.shape}")

if df.shape[0] < 10:
    raise ValueError("데이터 수가 너무 적습니다. 최소 10개 이상 필요.")

# --- 5. 범주형 인코딩 ---
categorical_cols = ['age', 'gender']
label_encoders = {}
for col in categorical_cols:
    le = LabelEncoder()
    df[col] = le.fit_transform(df[col])
    label_encoders[col] = le

# --- 6. 입력/타겟 분리 ---
X = df[feature_columns]
y = df[target_columns]

# --- 7. 새로운 광고 예측 ---
new_ad = {
    'age': '25-34',
    'gender': 'male',
    'clicks': 5000,
    'impressions': 30000,
    'frequency': 3,
    'reach': 18000,
    'spend': 100.0
}
new_df = pd.DataFrame([new_ad])

# --- 8. 범주형 인코딩 (예측 입력용) ---
for col in categorical_cols:
    if col in new_df:
        try:
            new_df[col] = label_encoders[col].transform(new_df[col])
        except ValueError:
            new_df[col] = 0  # 학습에 없던 값 처리

new_X = new_df[feature_columns].reindex(columns=X.columns, fill_value=0)

# --- 9. 예측 수행 ---
pred = multi_model.predict(new_X)[0]
pred_ctr, pred_cpc = pred[0], pred[1]

# --- 10. 이진 분류 기준 적용 ---
new_df['CTR'] = pred_ctr
new_df['CPC'] = pred_cpc
new_df['CTR_Result'] = np.where(pred_ctr >= 0.0114, 1, 0)
new_df['CPC_Result'] = np.where(pred_cpc <= 1.07, 1, 0)

# --- 11. 결과 출력 ---
print(f"\n📊 예측 CTR: {pred_ctr:.4f} ({pred_ctr * 100:.2f}%)")
print(f"💰 예측 CPC: ${pred_cpc:.4f}")
print(f"✅ CTR 성공 여부 (≥ 0.0114): {'성공' if new_df['CTR_Result'][0] else '실패'}")
print(f"✅ CPC 성공 여부 (≤ $1.07): {'성공' if new_df['CPC_Result'][0] else '실패'}")

print("\n🔍 전체 예측 결과:")
print(new_df[['CTR', 'CTR_Result', 'CPC', 'CPC_Result']])

# --- 12. 로그 저장 ---
timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
log = f"[{timestamp}] CTR: {pred_ctr:.4f}, CPC: {pred_cpc:.4f}, CTR_Result: {new_df['CTR_Result'][0]}, CPC_Result: {new_df['CPC_Result'][0]}\n"

with open("prediction_log.txt", "a", encoding="utf-8") as f:
    f.write(log)