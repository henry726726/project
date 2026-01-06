import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.preprocessing import LabelEncoder
import lightgbm as lgb
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, classification_report
import re
import matplotlib.pyplot as plt
import seaborn as sns

# --- 0. CSV 파일 로드 및 초기 데이터 준비 ---
print("--- 0. CSV 파일 로드 시작 ---")

# CSV 파일 로드
try:
    df = pd.read_csv('Social_Media_Advertising.csv')
    print("CSV 파일 로드 성공!")
except FileNotFoundError:
    print("오류: 'Social_Media_Advertising.csv' 파일을 찾을 수 없습니다. 파일 경로를 확인해주세요.")
    print("스크립트 실행을 중단합니다. 파일을 동일한 폴더에 넣거나 경로를 수정해주세요.")
    exit()

print("\n--- 로드된 데이터 미리보기 (상위 5개 행) ---")
print(df.head())
print("\n--- 로드된 데이터 컬럼 정보 ---")
print(df.info())
print("\n--- 로드된 데이터 기술 통계 ---")
print(df.describe())
print(f"\n로드된 데이터 프레임 크기: {df.shape}")
print("--- 0. CSV 파일 로드 완료 ---\n")

# 추가: Conversion_Rate의 고유값 및 분포 확인 (문제 진단용)
print("\n--- Conversion_Rate 고유값 및 분포 확인 ---")
print("Conversion_Rate 고유값 개수:", df['Conversion_Rate'].nunique())
print("Conversion_Rate 고유값 및 빈도:")
print(df['Conversion_Rate'].value_counts().sort_index())

plt.figure(figsize=(10, 6))
sns.histplot(df['Conversion_Rate'], bins=20, kde=True)
plt.title('Distribution of Conversion_Rate')
plt.xlabel('Conversion_Rate')
plt.ylabel('Frequency')
plt.grid(True)
plt.show()
print("-------------------------------------------\n")


# --- 1. 데이터 클리닝 및 특징 엔지니어링 (기존 컬럼 유지) ---
print("--- 1. 데이터 클리닝 및 특징 엔지니어링 시작 ---")

# 1.1. 'Acquisition_Cost' 컬럼 전처리: object -> float
if 'Acquisition_Cost' in df.columns:
    df['Acquisition_Cost'] = df['Acquisition_Cost'].astype(str).apply(lambda x: re.sub(r'[$,]', '', x)).astype(float)
    print("   'Acquisition_Cost' 컬럼이 float으로 변환되었습니다.")
else:
    print("   경고: 'Acquisition_Cost' 컬럼을 찾을 수 없습니다. 데이터 처리에서 제외합니다.")

# 1.2. 'Duration' 컬럼 전처리: 'X Days' -> X (int)
if 'Duration' in df.columns:
    df['Duration_Days'] = df['Duration'].astype(str).str.extract('(\d+)').astype(int)
    df = df.drop('Duration', axis=1)
    print("   'Duration' 컬럼이 'Duration_Days' (int)로 변환되었습니다.")
else:
    print("   경고: 'Duration' 컬럼을 찾을 수 없습니다. 데이터 처리에서 제외합니다.")

# 1.3. 불필요한 컬럼 제거
columns_to_drop = ['Campaign_ID', 'Date']
df = df.drop(columns=[col for col in columns_to_drop if col in df.columns], errors='ignore')
print(f"   불필요한 컬럼 {columns_to_drop}이(가) 제거되었습니다.")

# 1.4. 새로운 비율 특징 생성: CTR (Clicks / Impressions)
if 'Clicks' in df.columns and 'Impressions' in df.columns:
    df['CTR'] = df['Clicks'] / df['Impressions'].replace(0, 1e-6)
    print("   새로운 특징 'CTR' (Clicks / Impressions)이 생성되었습니다.")
else:
    print("   경고: 'Clicks' 또는 'Impressions' 컬럼이 없어 'CTR' 특징을 생성할 수 없습니다.")


print("\n--- 클리닝 및 특징 엔지니어링 후 데이터 미리보기 (상위 5개 행) ---")
print(df.head())
print(f"\n클리닝 및 특징 엔지니어링 후 데이터 프레임 크기: {df.shape}")
print("--- 1. 데이터 클리닝 및 특징 엔지니어링 완료 ---\n")


# --- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 (분류 문제에 맞게 수정) ---
print("--- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 시작 ---")

# 타겟 변수 설정 (Conversion_Rate)
target_column = 'Conversion_Rate'
if target_column not in df.columns:
    print(f"오류: 타겟 컬럼 '{target_column}'을(를) 찾을 수 없습니다. CSV 파일의 컬럼명을 확인해주세요.")
    exit()

# Conversion_Rate를 범주형으로 변환 (타겟을 'low', 'medium', 'high' 등으로 매핑)
# 경계값은 데이터 분포를 확인하여 조정 가능합니다.
bins = [0, 0.05, 0.10, df['Conversion_Rate'].max() + 0.01]
labels = ['Low', 'Medium', 'High']
df['Conversion_Rate_Category'] = pd.cut(df['Conversion_Rate'], bins=bins, labels=labels, right=True, include_lowest=True)

print(f"\n변환된 Conversion_Rate 범주 분포:")
print(df['Conversion_Rate_Category'].value_counts())

# 새로운 타겟 컬럼 설정
target_category_column = 'Conversion_Rate_Category'
y_category = df[target_category_column]

# LightGBM에 사용할 특징 컬럼들을 정의합니다.
feature_columns = [col for col in df.columns if col not in [target_column, target_category_column]]

# 범주형 컬럼과 수치형 컬럼 구분 (CTR은 수치형에 자동으로 포함)
categorical_cols = [col for col in feature_columns if df[col].dtype == 'object']
numerical_cols = [col for col in feature_columns if df[col].dtype != 'object']

print(f"\n   수치형 특징 컬럼: {numerical_cols}")
print(f"   범주형 특징 컬럼: {categorical_cols}")

label_encoders = {} # 각 범주형 컬럼에 대한 LabelEncoder 객체를 저장

# 타겟 범주도 Label Encoding
target_label_encoder = LabelEncoder()
y_encoded = target_label_encoder.fit_transform(y_category)
label_encoders[target_category_column] = target_label_encoder

print(f"\n'{target_category_column}' Label Mapping (Target Category):")
for original, encoded in zip(target_label_encoder.classes_, target_label_encoder.transform(target_label_encoder.classes_)):
    print(f"   '{original}' -> {encoded}")
print("-" * 20)

for col in categorical_cols:
    if col in df.columns:
        le = LabelEncoder()
        df[col] = le.fit_transform(df[col])
        label_encoders[col] = le
        print(f"   '{col}' Label Mapping:")
        for original, encoded in zip(le.classes_, le.transform(le.classes_)):
            print(f"   '{original}' -> {encoded}")
        print("-" * 20)
    else:
        print(f"   경고: 범주형 컬럼 '{col}'이(가) 데이터프레임에 없습니다. 건너뜁니다.")

# 최종 특징 데이터프레임
X = df[feature_columns]

print("\n--- 전처리 후 특징 데이터 미리보기 (상위 5개 행) ---")
print(X.head())
print(f"\n전처리 후 특징 데이터 프레임 크기: {X.shape}")
print("--- 2. 특징 및 타겟 변수 정의 및 범주형 변수 전처리 완료 ---\n")


# --- 3. LightGBM 모델 학습 및 평가 (분류 모델에 맞게 수정) ---
print("--- 3. LightGBM 모델 학습 및 평가 시작 ---")

# 훈련 세트와 테스트 세트 분리
X_train, X_test, y_train_encoded, y_test_encoded = train_test_split(X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded)

# LightGBM 모델 파라미터 설정 (분류 문제)
# RandomizedSearchCV를 위한 파라미터 분포 정의
param_dist = {
    'n_estimators': [300, 500, 800, 1000],
    'learning_rate': [0.01, 0.03, 0.05, 0.1],
    'num_leaves': [20, 31, 50, 63, 80],
    'feature_fraction': [0.6, 0.7, 0.8, 0.9],
    'bagging_fraction': [0.6, 0.7, 0.8, 0.9],
    'bagging_freq': [1],
    'lambda_l1': [0.0, 0.1, 0.5, 1.0],
    'lambda_l2': [0.0, 0.1, 0.5, 1.0],
    'min_child_samples': [10, 20, 30, 50],
}

# LightGBM Classifier 객체 생성
lgbm = lgb.LGBMClassifier(objective='multiclass', num_class=len(target_label_encoder.classes_),
                          metric='multi_logloss', n_jobs=-1, random_state=42, verbose=-1)

# RandomizedSearchCV 설정
random_search = RandomizedSearchCV(estimator=lgbm, param_distributions=param_dist,
                                   n_iter=50, # 탐색할 조합의 수. 이 값을 줄이면 더 빨라짐.
                                   cv=5,
                                   scoring='f1_weighted',
                                   n_jobs=-1, verbose=2, random_state=42)

print("\n--- RandomizedSearchCV를 통한 최적 하이퍼파라미터 탐색 시작 (시간 소요) ---")
random_search.fit(X_train, y_train_encoded)

print("\n--- 최적 파라미터 ---")
print(random_search.best_params_)
print("\n--- 최적 F1-Score (RandomizedSearchCV 결과 - 값이 높을수록 좋음) ---")
print(f"F1-Score: {random_search.best_score_:.4f}")

# 최적의 모델로 예측 및 평가
model = random_search.best_estimator_
y_pred_encoded = model.predict(X_test)

# 성능 평가 (분류 모델)
accuracy = accuracy_score(y_test_encoded, y_pred_encoded)
precision = precision_score(y_test_encoded, y_pred_encoded, average='weighted')
recall = recall_score(y_test_encoded, y_pred_encoded, average='weighted')
f1 = f1_score(y_test_encoded, y_pred_encoded, average='weighted')

print(f"\n정확도 (Accuracy): {accuracy:.4f}")
print(f"정밀도 (Precision): {precision:.4f}")
print(f"재현율 (Recall): {recall:.4f}")
print(f"F1-Score: {f1:.4f}")

print("\n--- 분류 리포트 (Classification Report) ---")
print(classification_report(y_test_encoded, y_pred_encoded, target_names=target_label_encoder.classes_))


# --- 특징 중요도 확인 (분류 모델) ---
feature_importance = pd.DataFrame({
    'feature': X.columns,
    'importance': model.feature_importances_ # 수정된 부분: model.feature_importance -> model.feature_importances_
}).sort_values(by='importance', ascending=False)

print("\n--- 특징 중요도 (Feature Importance) ---")
print(feature_importance)

# --- 특징 중요도 기준점 설정 및 필터링 (예시) ---
importance_threshold = 1000.0 # 이 값은 분류 모델의 중요도 분포에 따라 조정 필요

important_features = feature_importance[feature_importance['importance'] >= importance_threshold]

if not important_features.empty:
    print(f"\n--- 중요도 {importance_threshold:.1f} 이상인 주요 특징 ---")
    print(important_features)
else:
    print(f"\n--- 중요도 {importance_threshold:.1f} 이상인 특징이 없습니다. 임계값을 조정해보세요. ---")

print("--- 3. LightGBM 모델 학습 및 평가 완료 ---\n")


# --- 4. 새로운 임의 데이터로 예측 (분류 결과 예측 및 판단) ---
print("--- 4. 새로운 임의 데이터로 예측 시작 ---")

# 예측할 새로운 임의 광고 데이터 포인트를 생성합니다.
new_ad_data_raw = {
    'Target_Audience': 'Men 25-34',
    'Campaign_Goal': 'Increase Sales',
    'Channel_Used': 'Email',
    'Acquisition_Cost': 50.0,
    'ROI': 4.5,
    'Location': 'United States',
    'Language': 'English',
    'Clicks': 25000,
    'Impressions': 75000,
    'Engagement_Score': 8,
    'Customer_Segment': 'Health',
    'Company': 'Aura Align',
    'Duration_Days': 30
}

new_ad_df = pd.DataFrame([new_ad_data_raw])

# 새로운 데이터도 모델 학습에 사용된 것과 동일한 전처리 과정을 거쳐야 합니다.
# CTR 특징도 새로운 데이터에 대해 생성합니다.
if 'Clicks' in new_ad_df.columns and 'Impressions' in new_ad_df.columns:
    new_ad_df['CTR'] = new_ad_df['Clicks'] / new_ad_df['Impressions'].replace(0, 1e-6)
else:
    new_ad_df['CTR'] = 0

# 범주형 변수 인코딩
for col in categorical_cols:
    if col in new_ad_df.columns and col in label_encoders:
        try:
            new_ad_df[col] = label_encoders[col].transform(new_ad_df[col])
        except ValueError as e:
            print(f"경고: 새로운 데이터의 범주형 컬럼 '{col}' 값에 학습 데이터에 없는 값이 포함되어 있습니다: {new_ad_df[col].iloc[0]} - {e}")
            new_ad_df[col] = 0

# LightGBM 모델에 입력할 특징 컬럼 순서를 학습 데이터와 동일하게 맞춥니다.
final_new_ad_df = new_ad_df.reindex(columns=X.columns, fill_value=0)

# 새로운 데이터에 대한 예측 (범주형)
predicted_category_encoded = model.predict(final_new_ad_df)[0]
predicted_category_name = target_label_encoder.inverse_transform([predicted_category_encoded])[0]

print(f"\n--- 새로운 임의 광고의 예상 {target_category_column} (범주): '{predicted_category_name}' ---")

# --- 콘텐츠 교체 판단 알고리즘 적용 (Rule 기반 - 분류 결과 기반) ---
print("\n--- 콘텐츠 교체 판단 ---")

# 새로운 광고 데이터의 Customer_Segment 원본 값을 가져옴
new_ad_segment = new_ad_data_raw.get('Customer_Segment')

# 새로운 광고의 Customer_Segment에 따른 산업 매핑
segment_to_industry_map = {
    'Fashion': '의류/패션',
    'Health': '치과 및 치과 서비스',
}
target_industry_for_new_ad = segment_to_industry_map.get(new_ad_segment, '비즈니스 서비스')

print(f"새로운 광고의 고객 세그먼트: '{new_ad_segment}'")
print(f"매핑된 산업: '{target_industry_for_new_ad}'")

# 교체 필요 조건: 예측된 CVR 범주가 'Low'인 경우
content_replacement_needed = False
if predicted_category_name == 'Low':
    content_replacement_needed = True
    print(f"   - 조건 충족: 예측된 전환율 범주가 '{predicted_category_name}'이므로 교체가 필요합니다.")
else:
    print(f"   - 조건 미충족: 예측된 전환율 범주가 '{predicted_category_name}'이므로 교체 필요 조건에 해당하지 않습니다.")


if content_replacement_needed:
    print("\n💡 결론: 이 광고 콘텐츠는 교체가 필요합니다.")
else:
    print("\n👍 결론: 이 광고 콘텐츠는 현재로서는 교체가 필요하지 않습니다.")

print("--- 4. 새로운 임의 데이터로 예측 및 콘텐츠 교체 판단 완료 ---")