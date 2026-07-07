package com.thx.component;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class MailService {

    @Resource
    private JavaMailSender mailSender;

    // 简单文本邮件
    public void sendSimpleMail(String to, String subject, String content) {
        log.info("发送简单邮件开始...");
        try {
            log.info("发送者：{} 接收者：{} 主题：{} 内容：{} 发送时间：{}", "2789399494@qq.com", to, subject, content, DateUtil.now());
            SimpleMailMessage message = new SimpleMailMessage();
            // 发送者
            message.setFrom("2789399494@qq.com");
            // 接收者
            message.setTo(to);
            // 主题
            message.setSubject(subject);
            // 正文
            message.setText(content);
            mailSender.send(message);
            log.info("发送简单邮件结束...");
        } catch (Exception e) {
            log.error("发送简单邮件异常：{}", e.getMessage());
        }
    }
}
