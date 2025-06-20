import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import lightgbm as lgb
from sklearn.metrics import mean_squared_error, r2_score
import re # For cleaning Acquisition_Cost

# --- 0. CSV 파일 로드 및 초기 데이터 준비 ---
print("--- 0. CSV 파일 로드 시작 ---")

# CSV 파일 로드
try:
    df = pd.read_csv('Social_Media_Advertising.csv')
    print("CSV 파일 로드 성공!")
except FileNotFoundError:
    print("오류: 'Social_Media_Advertising.csv' 파일을 찾을 수 없습니다. 파일 경로를 확인해주세요.")
    print("스크립트 실행을 중단합니다. 파일을 동일한 폴더에 넣거나 경로를 수정해주세요.")
    exit() # 파일이 없으면 스크립트 종료

print("\n--- 로드된 데이터 미리보기 (상위 5개 행) ---")
print(df.head())
print("\n--- 로드된 데이터 컬럼 정보 ---")
print(df.info())
print("\n--- 로드된 데이터 기술 통계 ---")
print(df.describe())
print(f"\n로드된 데이터 프레임 크기: {df.shape}")
print("--- 0. CSV 파일 로드 완료 ---\n")


# --- 1. 데이터 클리닝 및 특징 엔지니어링 (기존 컬럼만 활용) ---
print("--- 1. 데이터 클리닝 및 특징 엔지니어링 시작 ---")

# 1.1. 'Acquisition_Cost' 컬럼 전처리: object -> float
# '$' 또는 ',' 제거 후 float으로 변환
if 'Acquisition_Cost' in df.columns:
    df['Acquisition_Cost'] = df['Acquisition_Cost'].astype(str).apply(lambda x: re.sub(r'[$,]', '', x)).astype(float)
    print("  'Acquisition_Cost' 컬럼이 float으로 변환되었습니다.")
else:
    print("  경고: 'Acquisition_Cost' 컬럼을 찾을 수 없습니다. 데이터 처리에서 제외합니다.")

# 1.2. 'Duration' 컬럼 전처리: 'X Days' -> X (int)
# 'Duration' 컬럼을 기반으로 'Duration_Days'라는 새로운 수치형 컬럼을 생성합니다.
# 이는 기존 컬럼의 정보를 추출하는 것이므로 "새로운 개념의 컬럼 추가"가 아닌,
# 기존 정보를 모델이 활용할 수 있게 변환하는 과정으로 간주합니다.
if 'Duration' in df.columns:
    df['Duration_Days'] = df['Duration'].astype(str).str.extract('(\d+)').astype(int)
    df = df.drop('Duration', axis=1) # 원본 'Duration' 컬럼 제거
    print("  'Duration' 컬럼이 'Duration_Days' (int)로 변환되었습니다.")
else:
    print("  경고: 'Duration' 컬럼을 찾을 수 없습니다. 데이터 처리에서 제외합니다.")

# 1.3. 불필요한 컬럼 제거 (기존 컬럼 중 모델 학습에 불필요한 식별자/날짜 컬럼)
# Campaign_ID는 단순히 고유 ID이므로 예측에 직접적인 도움이 되지 않습니다.
# Date는 날짜 정보이며, 복잡도를 낮추기 위해 제외합니다.
columns_to_drop = ['Campaign_ID', 'Date']
df = df.drop(columns=[col for col in columns_to_drop if col in df.columns], errors='ignore')
print(f"  불필요한 컬럼 {columns_to_drop}이(가) 제거되었습니다.")


print("\n--- 클리닝 및 특징 엔지니어링 후 데이터 미리보기 (상위 5개 행) ---")
print(df.head())
print(f"\n클리닝 및 특징 엔지니어링 후 데이터 프레임 크기: {df.shape}")
print("--- 1. 데이터 클리닝 및 특징 엔지니어링 완료 ---\n")


# --- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 ---
print("--- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 시작 ---")

# 타겟 변수 설정 (Conversion_Rate)
target_column = 'Conversion_Rate'
if target_column not in df.columns:
    print(f"오류: 타겟 컬럼 '{target_column}'을(를) 찾을 수 없습니다. CSV 파일의 컬럼명을 확인해주세요.")
    exit()

y = df[target_column]

# LightGBM에 사용할 특징 컬럼들을 정의합니다.
# 데이터 클리닝/특징 엔지니어링 후 남은 컬럼들 중에서 타겟 컬럼을 제외합니다.
feature_columns = [col for col in df.columns if col != target_column]

# 범주형 컬럼과 수치형 컬럼 구분
# 'object' 타입이거나 특정 컬럼 이름으로 범주형을 명시
categorical_cols = [col for col in feature_columns if df[col].dtype == 'object']
numerical_cols = [col for col in feature_columns if df[col].dtype != 'object']

print(f"\n  수치형 특징 컬럼: {numerical_cols}")
print(f"  범주형 특징 컬럼: {categorical_cols}")

label_encoders = {} # 각 범주형 컬럼에 대한 LabelEncoder 객체를 저장

for col in categorical_cols:
    if col in df.columns:
        le = LabelEncoder()
        df[col] = le.fit_transform(df[col])
        label_encoders[col] = le # 인코더 객체 저장
        print(f"  '{col}' Label Mapping:")
        for original, encoded in zip(le.classes_, le.transform(le.classes_)):
            print(f"    '{original}' -> {encoded}")
        print("-" * 20)
    else:
        print(f"  경고: 범주형 컬럼 '{col}'이(가) 데이터프레임에 없습니다. 건너_ㅂ니다.")

# 최종 특징 데이터프레임
X = df[feature_columns]

print("\n--- 전처리 후 특징 데이터 미리보기 (상위 5개 행) ---")
print(X.head())
print(f"\n전처리 후 특징 데이터 프레임 크기: {X.shape}")
print("--- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 완료 ---\n")


# --- 3. LightGBM 모델 학습 및 평가 ---
print("--- 3. LightGBM 모델 학습 및 평가 시작 ---")

# 훈련 세트와 테스트 세트 분리
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# LightGBM 데이터셋 생성
train_data = lgb.Dataset(X_train, label=y_train)
test_data = lgb.Dataset(X_test, label=y_test, reference=train_data)

# LightGBM 모델 파라미터 설정 (회귀 문제)
params = {
    'objective': 'regression',       # 타겟이 Conversion_Rate이므로 회귀
    'metric': 'rmse',                # 평가 지표: Root Mean Squared Error
    'n_estimators': 1000,            # 부스팅 단계 수
    'learning_rate': 0.05,           # 학습률
    'feature_fraction': 0.8,         # 각 트리를 훈련할 때 사용할 특징의 비율
    'bagging_fraction': 0.8,         # 데이터를 샘플링하여 사용할 비율
    'bagging_freq': 1,               # 배깅 수행 빈도
    'lambda_l1': 0.1,                # L1 정규화
    'lambda_l2': 0.1,                # L2 정규화
    'num_leaves': 31,                # 하나의 트리가 가질 수 있는 최대 리프 노드 수
    'verbose': -1,                   # 출력 메시지 제어 (-1은 메시지 끄기)
    'n_jobs': -1,                    # 사용할 코어 수
    'seed': 42                       # 랜덤 시드
}

# 모델 학습
model = lgb.train(
    params,
    train_data,
    valid_sets=[test_data],
    callbacks=[lgb.early_stopping(stopping_rounds=50, verbose=False)] # 조기 중단
)

# 테스트 세트에 대한 예측
y_pred = model.predict(X_test, num_iteration=model.best_iteration)

# 모델 성능 평가
rmse = np.sqrt(mean_squared_error(y_test, y_pred))
r2 = r2_score(y_test, y_pred)

print(f"\nRMSE (Root Mean Squared Error): {rmse:.4f}")
print(f"R-squared (결정 계수): {r2:.4f}")

# 특징 중요도 확인
feature_importance = pd.DataFrame({
    'feature': X.columns,
    'importance': model.feature_importance(importance_type='gain') # 'gain' 또는 'split'
}).sort_values(by='importance', ascending=False)

print("\n--- 특징 중요도 (Feature Importance) ---")
print(feature_importance)
print("--- 3. LightGBM 모델 학습 및 평가 완료 ---\n")


# --- 4. 새로운 임의 데이터로 예측 ---
print("--- 4. 새로운 임의 데이터로 예측 시작 ---")

# 예측할 새로운 임의 광고 데이터 포인트를 생성합니다.
# 이 값들은 CSV 파일에서 발견된 실제 값 범위 내에 있어야 하며,
# 범주형 컬럼은 원본 문자열 값으로 넣어줍니다.
# (이 부분은 실제 여러분의 AI 시스템에서 생성될 새로운 광고의 특징 데이터를 모방합니다.)
new_ad_data_raw = {
    'Target_Audience': 'Men 25-34',
    'Campaign_Goal': 'Increase Sales',
    'Channel_Used': 'Email',
    'Acquisition_Cost': 50.0, # 숫자 (float으로 변환했으므로 숫자 형태)
    'ROI': 4.5,
    'Location': 'United States',
    'Language': 'English',
    'Clicks': 25000,
    'Impressions': 75000,
    'Engagement_Score': 8,
    'Customer_Segment': 'Technology',
    'Company': 'Aura Align',
    'Duration_Days': 30 # 숫자 (기존 Duration에서 추출했으므로 숫자 형태)
}

new_ad_df = pd.DataFrame([new_ad_data_raw])

# 새로운 데이터도 모델 학습에 사용된 것과 동일한 전처리 과정을 거쳐야 합니다.

# 범주형 변수 인코딩
for col in categorical_cols:
    if col in new_ad_df.columns and col in label_encoders:
        # 학습 시 사용한 LabelEncoder 객체로 변환
        try:
            new_ad_df[col] = label_encoders[col].transform(new_ad_df[col])
        except ValueError as e:
            print(f"경고: 새로운 데이터의 범주형 컬럼 '{col}' 값에 학습 데이터에 없는 값이 포함되어 있습니다: {new_ad_df[col].iloc[0]} - {e}")
            # 실제 서비스에서는 이 경우를 대비하여 학습 데이터에 없는 값이 들어오면
            # -1 또는 학습 데이터의 최빈값 등으로 대체하는 로직이 필요합니다.
            # 여기서는 예시를 위해 해당 컬럼은 0으로 처리하겠습니다 (주의 필요).
            new_ad_df[col] = 0
    # else: 해당 컬럼이 new_ad_df에 없거나 label_encoders에 없으면 무시 (경고는 출력하지 않음)

# LightGBM 모델에 입력할 특징 컬럼 순서를 학습 데이터와 동일하게 맞춥니다.
# 학습 데이터 (X)의 컬럼 순서를 기준으로 새로운 데이터프레임을 재구성합니다.
# 만약 new_ad_df에 X.columns에 없는 컬럼이 있다면 오류가 발생할 수 있으므로,
# X.columns에 있는 컬럼만 선택하고, new_ad_df에 없는 컬럼은 0 등으로 채워야 합니다.
final_new_ad_df = new_ad_df.reindex(columns=X.columns, fill_value=0)


predicted_conversion_rate = model.predict(final_new_ad_df)

print(f"\n--- 새로운 임의 광고의 예상 {target_column}: {predicted_conversion_rate[0]:.4f} ---")
print("--- 4. 새로운 임의 데이터로 예측 완료 ---")