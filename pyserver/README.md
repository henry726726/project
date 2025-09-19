제안된 프로젝트 구조는 **파이프라인 방식**으로, 각 단계가 독립적인 모듈로 구성되어 서로 데이터를 주고받는 방식

---

### **1. MySQL RDS**

프로젝트의 중앙 데이터 저장소 역할을 합니다. 모든 작업의 입력과 출력을 관리합니다.

* **`ad_contents` 테이블**: 이 테이블이 핵심입니다.
    * `id`: 광고 콘텐츠의 고유 식별자.
    * `original_image_base64`: 제품 사진 등 **원본 이미지**가 base64 형식으로 저장됩니다.
    * `product`: 제품명과 같은 **제품 정보**가 저장됩니다.
    * `final_result`: Qwen 모델과 Gemini 모델의 **결과 JSON**이 저장됩니다. 여기에는 Qwen의 레이아웃 정보와 Gemini가 생성한 배경 이미지 데이터가 포함됩니다.
    * `logo_image_base64`: 로고 이미지가 base64 형식으로 저장됩니다.
    * `final_image_base64`: 모든 파이프라인이 완료된 후, 최종적으로 텍스트가 합성된 **완성 광고 이미지**가 저장됩니다.

---

### **2. 모듈화된 파이프라인**

각 단계는 독립적인 Python 클래스로 구현되어 FastAPI 엔드포인트를 통해 호출됩니다.

* **`mysql_module.py`**:
    * `MySQLLoader` 클래스: MySQL RDS에 연결하여 데이터를 읽고 쓰는 역할을 담당합니다.
    * `get_ad_content_for_rendering(id)`: 특정 ID의 콘텐츠를 가져옵니다.
    * `update_final_ad_sync(id, image_data)`: 최종 이미지를 DB에 업데이트합니다.
* **`qwen_module.py`**:
    * `QwenProcessor` 클래스: Qwen 모델을 사용하여 이미지와 제품명으로부터 **광고 레이아웃**을 생성합니다.
* **`nanobanana_module.py`**:
    * `GeminiImageGenerator` 클래스: 원본 이미지를 분석하여 Gemini 모델을 사용해 **새로운 배경 이미지**를 생성합니다.
* **`ad_text_render_module.py`**:
    * `AdTextRenderer` 클래스: 최종 단계입니다.
    * Gemini가 만든 배경 이미지와 Qwen이 만든 레이아웃 정보, 그리고 DB의 제품명을 기반으로 **텍스트와 로고를 합성**하여 최종 광고 이미지를 만듭니다.

---

### **3. FastAPI 기반 API 서버**

프로젝트의 전체 워크플로우를 관리하는 API 엔드포인트를 제공합니다.

* **`main.py`**:
    * `FastAPI` 애플리케이션을 초기화하고, 각 모듈의 클래스를 인스턴스화합니다.
    * **`/generate-ad-with-text` 엔드포인트**:
        1.  `ad_content_id`를 요청으로 받습니다.
        2.  `MySQLLoader`를 사용하여 DB에서 `original_image_base64`, `product`, `final_result` 등의 모든 필요한 데이터를 가져옵니다.
        3.  DB에서 가져온 `final_result` JSON에서 Qwen의 레이아웃과 Gemini의 이미지 바이트 데이터를 분리합니다.
        4.  `AdTextRenderer`를 호출하여 배경 이미지, 레이아웃 정보, 제품명을 결합해 텍스트와 로고를 렌더링합니다.
        5.  `MySQLLoader`를 다시 호출하여 최종 완성된 이미지를 DB에 저장합니다.
        6.  최종 결과를 응답으로 반환합니다.

