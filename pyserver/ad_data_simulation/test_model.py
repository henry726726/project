import pandas as pd
import joblib
import os
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import OneHotEncoder
from sklearn.metrics import mean_squared_error
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline

# ✅ CSV 파일에서 데이터 로드
df = pd.read_csv("Social_Media_Advertising.csv")  # ← 필요에 따라 경로 수정

# Acquisition_Cost 컬럼 '$' 제거 및 float 변환
df['Acquisition_Cost'] = df['Acquisition_Cost'].replace('[\$,]', '', regex=True).astype(float)

# 특성과 타겟 정의
target = 'Conversion_Rate'
features = ['Target_Audience', 'Campaign_Goal', 'Channel_Used', 'Acquisition_Cost', 'ROI',
            'Location', 'Language', 'Clicks', 'Impressions', 'Engagement_Score']

X = df[features]
y = df[target]

# 범주형 변수와 수치형 변수 분리
categorical_features = ['Target_Audience', 'Campaign_Goal', 'Channel_Used', 'Location', 'Language']
numeric_features = ['Acquisition_Cost', 'ROI', 'Clicks', 'Impressions', 'Engagement_Score']

# 파이프라인 구성
preprocessor = ColumnTransformer(
    transformers=[
        ('cat', OneHotEncoder(handle_unknown='ignore'), categorical_features)
    ],
    remainder='passthrough'  # 나머지는 그대로 전달
)

pipeline = Pipeline([
    ('preprocessing', preprocessor),
    ('regressor', LinearRegression())
])

# 데이터 분할 및 학습
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

pipeline.fit(X_train, y_train)

# 평가
y_pred = pipeline.predict(X_test)
mse = mean_squared_error(y_test, y_pred)
print(f"사전학습 모델 MSE: {mse:.4f}")

# 모델 저장 디렉토리 생성
os.makedirs("model", exist_ok=True)

# 모델 저장
joblib.dump(pipeline, "model/lgbm_social_media.pkl")
print("✅ 사전학습 모델 저장 완료 (model/lgbm_social_media.pkl)")