#text rendering 수정방안

google fonts (구글 폰트에서 지정한 feeling 그대로 연결 지어 사용)
business 나눔 고딕 



1.  ad_text_render.py에 폰트 및 색상 매칭 정보 추가 버전
ad_text_render_v3.py
├── [기존 함수들]
├── compute_style_scores
├── adjust_lightness
├── luminance
├── choose_font_from_style
├── choose_color_from_palette
├── load_design_context
├── main()
    ├── args parsing
    ├── load_design_context → 감성 기반 폰트/색상 선택
    ├── 기존 layout 처리
    ├── 텍스트 렌더링 시 색상 자동 적용


2. json 파일 입력해서 chatgpt api에 넣어 감성 분석 및 폰트 및 색상 추천 버전 