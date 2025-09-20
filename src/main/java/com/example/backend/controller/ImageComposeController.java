package com.example.backend.controller;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Collections;

import javax.imageio.ImageIO;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.service.ImageGenerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageComposeController {

    private final ImageGenerationService imageGenerationService;

    @PostMapping("/compose")
    public ResponseEntity<byte[]> composeImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam("text") String text) throws IOException {

        // 1) 원본 이미지 읽기
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) {
            throw new IOException("❌ 업로드된 파일을 이미지로 읽을 수 없습니다.");
        }
        int w = img.getWidth(), h = img.getHeight();

        // 2) Graphics2D 설정
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ✅ 3) 한글 폰트 불러오기 (resources/fonts/NanumGothic.ttf)
        Font baseFont;
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/NanumGothic.ttf");
            if (fontStream == null) {
                // 폰트 파일을 찾지 못했을 때의 IOException을 명확히 throw하도록 수정
                // (이 IOException은 아래 catch 블록에서 잡힙니다)
                throw new IOException("❌ NanumGothic.ttf 폰트를 찾을 수 없습니다. /resources/fonts/ 경로에 넣어주세요.");
            }
            baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        } catch (FontFormatException e) { // 폰트 파일 형식 오류
            e.printStackTrace(); // 콘솔에 스택 트레이스 출력
            System.err.println("폰트 형식 오류: " + e.getMessage() + ". 기본 SansSerif 폰트를 사용합니다.");
            baseFont = new Font("SansSerif", Font.PLAIN, 36); // 기본 폰트 사용
        } catch (IOException e) { // 파일 읽기 오류 (폰트 파일을 못 찾거나 읽을 수 없을 때)
            e.printStackTrace(); // 콘솔에 스택 트레이스 출력
            System.err.println("폰트 파일 읽기 오류 (혹은 파일을 찾을 수 없음): " + e.getMessage() + ". 기본 SansSerif 폰트를 사용합니다.");
            baseFont = new Font("SansSerif", Font.PLAIN, 36); // 기본 폰트 사용
        }

        // 4) 폰트 및 색상 설정
        int fontSize = Math.max(w, h) / 15;
        Font font = baseFont.deriveFont(Font.BOLD, (float) fontSize);
        g.setFont(font);
        g.setColor(Color.WHITE); // 텍스트 채우기 색상 (초기 설정)
        g.setStroke(new BasicStroke(fontSize / 10f)); // 외곽선 두께
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f)); // 투명도

        // 5) 텍스트 위치 계산 (하단 중앙)
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (w - textWidth) / 2;
        int y = h - fm.getDescent() - fontSize / 2;

        // 6) 텍스트 그리기 (외곽선 + 채우기)
        g.setColor(Color.BLACK); // 외곽선 색상 (먼저 그리기 위해)
        TextLayout tl = new TextLayout(text, font, g.getFontRenderContext());
        Shape outline = tl.getOutline(null);
        g.translate(x, y); // 텍스트 그릴 위치로 이동
        g.draw(outline); // 외곽선 그리기

        g.setColor(Color.WHITE); // 채우기 색상
        g.fill(outline); // 채우기

        g.translate(-x, -y); // 원래 위치로 되돌리기 (필요에 따라)

        g.dispose(); // Graphics2D 리소스 해제

        // 7) PNG 변환 및 반환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
            baos.flush(); // 스트림에 쓴 내용을 확실히 플러시
            System.out.println("Image byte size: " + baos.size()); // 💡💡💡 이미지 바이트 크기 로그 출력 (0보다 커야 정상)
        } catch (IOException e) {
            System.err.println("이미지 PNG 변환 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e; // 오류 발생 시 클라이언트에게 다시 예외 던지기
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"composite.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(baos.toByteArray()); // 바이트 배열로 이미지 데이터 반환
    }



    // 새로 추가하는 프록시 엔드포인트 (기존 것은 손대지 않음)
    @PostMapping(value = "/compose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> composeProxy(
            // 파일: image 또는 image_file 중 아무거나
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "image_file", required = false) MultipartFile imageFile,

            @RequestPart(value = "product", required = false) String product,
            @RequestPart(value = "text", required = false) String text,

            // 텍스트 파라미터들 — 기존/신규 이름 모두 허용
            @RequestPart(value = "productName", required = false) String productName,
            @RequestPart(value = "product_name", required = false) String product_name,

            @RequestPart(value = "headline", required = false) String headline,
            @RequestPart(value = "caption", required = false) String caption,
            

            @RequestPart(value = "logoPath", required = false) String logoPath,
            @RequestPart(value = "logo_path", required = false) String logo_path,

            @RequestPart(value = "fontKor", required = false) String fontKor,
            @RequestPart(value = "font_kor", required = false) String font_kor
    ) {
        // 1) 이름 정규화 (기존 명칭을 우선 사용하고, 없으면 신규 키로 대체)
        MultipartFile resolvedImage = (image != null) ? image : imageFile;
        if (resolvedImage == null || resolvedImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image or image_file is required");
        }

        String resolvedProduct = firstNonEmpty(product, productName, product_name);
        String resolvedText    = firstNonEmpty(text, headline, caption);
        String resolvedLogoPath    = firstNonEmpty(logoPath, logo_path);
        String resolvedFontKor     = firstNonEmpty(fontKor, font_kor);
        

        // 2) 서비스로 위임 (서비스 내부에서 compose_service.py에 맞게 키 매핑)
        String base64 = imageGenerationService.composeProxyPassThrough(
                resolvedImage,
                resolvedProduct,
                resolvedText
        );

        return ResponseEntity.ok(Collections.singletonMap("image_base64", base64));
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
    
}