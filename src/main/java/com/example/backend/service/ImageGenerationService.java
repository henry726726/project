package com.example.backend.service;

import java.util.Map;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.dto.ComposeResponse;
import com.example.backend.entity.AdContent;
import com.example.backend.entity.AdResultJson;
import com.example.backend.repository.AdContentRepository;
import com.example.backend.repository.AdResultJsonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImageGenerationService {

    @Value("${compose.base-url:http://localhost:8010}")
    private String composeBaseUrl;

    // 별도 Bean 주입 없이 내부에서 생성 (충분합니다)
    private final RestTemplate restTemplate = new RestTemplate();

    /** (A) 최소 필수 3-인자 버전 */
    public String composeProxyPassThrough(
            MultipartFile imageFile,
            String product,
            String text
    ) {
        return doCall(imageFile, product, text, null, null);
    }

    /** (B) 컨트롤러 호출과 맞춘 5-인자 버전 (logoPath, fontKor 포함) */
    public String composeProxyPassThrough(
            MultipartFile imageFile,
            String product,
            String text,
            String logoPath,
            String fontKor
    ) {
        return doCall(imageFile, product, text, logoPath, fontKor);
    }

    /** 내부 공통 로직: compose_service.py로 멀티파트 포워딩 */
    private String doCall(
            MultipartFile imageFile,
            String product,
            String text,
            String logoPath,
            String fontKor
    ) {
        // [CHANGED] 하드코딩 제거 → 설정 값 사용
        String url = composeBaseUrl + "/compose";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource imageResource = new ByteArrayResource(toBytes(imageFile)) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        };

        // compose_service.py는 image 또는 image_file 둘 다 받음
        body.add("image", imageResource);

        // 프론트 기준 키로 전달 (파이썬에서 product→product_name, text→headline로 정규화)
        if (notBlank(product)) body.add("product", product);
        if (notBlank(text))    body.add("text", text);

        // 선택 전달
        if (notBlank(logoPath)) body.add("logo_path", logoPath);
        if (notBlank(fontKor))  body.add("font_kor", fontKor);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("compose_service call failed: " + resp.getStatusCode());
        }
        Object val = resp.getBody().get("image_base64");
        if (val == null) throw new RuntimeException("compose_service returned no 'image_base64'");
        return String.valueOf(val);
    }

    private byte[] toBytes(MultipartFile f) {
        try { return f.getBytes(); } catch (Exception e) { throw new RuntimeException(e); }
    }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /** 간단 프록시: 컨트롤러에서 기존 generateImage(caption, image) 호출 시 사용 */
    public String generateImage(String caption, MultipartFile image) {
        // caption을 compose_service의 'text'로 보내고, product는 비워서 보냄
        return composeProxyPassThrough(image, null, caption, null, null);
    }

    @Autowired private AdContentRepository adContentRepo;
    @Autowired private AdResultJsonRepository adResultJsonRepo;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Py 서버에서 image_base64 + layout + copy 를 받아
     * ad_contents(원본/결과/문구)와 ad_result_json(JSON들)을 저장
     */
    public Long generateAndSave(String caption, MultipartFile imageFile) throws Exception {
        // 1) 원본 이미지 base64
        String originalB64 = Base64.getEncoder().encodeToString(toBytes(imageFile));

        // [CHANGED] 하드코딩 제거 → 설정 값 사용
        String url = composeBaseUrl + "/compose";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imageResource = new ByteArrayResource(toBytes(imageFile)) {
            @Override public String getFilename() { return imageFile.getOriginalFilename(); }
        };
        MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
        body.add("text", caption);     // 또는 caption
        body.add("image", imageResource);

        HttpEntity<MultiValueMap<String,Object>> req = new HttpEntity<>(body, headers);

        ComposeResponse resp = restTemplate.postForObject(url, req, ComposeResponse.class);
        if (resp == null) {
            throw new RuntimeException("AI compose failed: empty response");
        }

        // [CHANGED] DTO 게터 이름 수정: getImage_base64() → getImageBase64()
        //  - ComposeResponse는 @JsonNaming(SnakeCaseStrategy)로 snake_case를 camelCase에 바인딩해야 합니다.
        if (resp.getImageBase64() == null) {
            throw new RuntimeException("AI compose failed: missing imageBase64");
        }

        // 3) ad_content 저장 (원본 + 결과 + 문구)
        AdContent ac = new AdContent();
        ac.setOriginalImageBase64(originalB64);
        // [CHANGED] camelCase 게터 사용
        ac.setGeneratedImageBase64(resp.getImageBase64());
        ac.setAdText(caption);
        ac = adContentRepo.save(ac);  // id 확보

        // 4) JSON 저장
        if (resp.getLayout() != null) {
            AdResultJson row = new AdResultJson();
            row.setAdContentId(ac.getId());
            row.setJsonType("layout");
            row.setPayload(om.writeValueAsString(resp.getLayout()));
            adResultJsonRepo.save(row);
        }
        if (resp.getCopy() != null) {
            AdResultJson row = new AdResultJson();
            row.setAdContentId(ac.getId());
            row.setJsonType("copy");
            row.setPayload(om.writeValueAsString(resp.getCopy()));
            adResultJsonRepo.save(row);
        }
        // meta도 저장하고 싶으면 json_type='meta'로 한 줄 더 추가 가능

        return ac.getId(); // 프론트로 adContentId 반환
    }

}
