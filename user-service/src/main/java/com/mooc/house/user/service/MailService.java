package com.mooc.house.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
  
  @Autowired
  private JavaMailSender mailSender;
  
  @Value("${spring.mail.username}")
  private String from;
  
  public void sendSimpleMail(String subject,String content,String toEmail){
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);//发件人Email地址
    message.setTo(toEmail);//收件人Email地址
    message.setSubject(subject);//邮件主题
    message.setText(content);//邮件主题内容
    mailSender.send(message);
  }
  
}
