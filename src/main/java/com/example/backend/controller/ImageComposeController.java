package com.example.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Shape;

@RestController
@RequestMapping("/api")
public class ImageComposeController {

    @PostMapping("/compose")
    public ResponseEntity<byte[]> composeImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam("text") String text) throws IOException {

        // 1) 원본 이미지 읽기
        BufferedImage img = ImageIO.read(file.getInputStream());
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
                throw new IOException("❌ NanumGothic.ttf 폰트를 찾을 수 없습니다. /resources/fonts/ 경로에 넣어주세요.");
            }
            baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            // 폰트 로딩 실패 시 기본 폰트 사용
            baseFont = new Font("SansSerif", Font.PLAIN, 36);
        }

        // 4) 폰트 및 색상 설정
        int fontSize = Math.max(w, h) / 15;
        Font font = baseFont.deriveFont(Font.BOLD, (float) fontSize);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(fontSize / 10f));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        // 5) 텍스트 위치 계산 (하단 중앙)
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (w - textWidth) / 2;
        int y = h - fm.getDescent() - fontSize / 2;

        // 6) 텍스트 그리기 (외곽선 + 채우기)
        g.setColor(Color.BLACK);
        TextLayout tl = new TextLayout(text, font, g.getFontRenderContext());
        Shape outline = tl.getOutline(null);
        g.translate(x, y);
        g.draw(outline); // 외곽선
        g.fill(outline); // 채우기
        g.translate(-x, -y);

        g.dispose();

        // 7) PNG 변환 및 반환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"composite.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(baos.toByteArray());
    }
}
