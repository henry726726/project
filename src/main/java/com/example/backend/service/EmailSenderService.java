/*package com.example.backend.service;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage; // Missing import statement for UnsupportedEncodingException

@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender; // Spring Boot가 자동으로 주입해주는 MailSender

    /**
     * PDF 파일을 첨부하여 이메일을 발송합니다.
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body 이메일 본문
     * @param pdfBytes 첨부할 PDF 파일의 바이트 배열
     * @param filename 첨부될 PDF 파일명 (예: "report.pdf")
     */
    /*public void sendEmailWithPdf(String to, String subject, String body, byte[] pdfBytes, String filename) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            // true는 멀티파트 메시지 (첨부파일)를 의미
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("your_email@gmail.com", "광고 자동화 리포트"); // 발신자 설정 (application.properties의 username과 동일해야 함)
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // 본문은 HTML 아님

            // 첨부 파일 추가
            // ByteArrayResource를 사용하여 바이트 배열을 첨부파일로 변환
            helper.addAttachment(filename, new ByteArrayResource(pdfBytes));

            mailSender.send(message); // 이메일 발송
            System.out.println("✅ 이메일 발송 완료! 수신자: " + to + ", 제목: " + subject);

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("❌ 이메일 발송 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}*/