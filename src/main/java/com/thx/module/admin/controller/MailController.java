package com.thx.module.admin.controller;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.component.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @PostMapping("/send")
    @AnonymousAccess
    public void send(@RequestParam String to) {
        mailService.sendSimpleMail(to, "简单邮件", "简单邮件测试！");
    }
}
