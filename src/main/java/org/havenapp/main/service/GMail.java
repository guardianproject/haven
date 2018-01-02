package org.havenapp.main.service;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by percy on 2/1/18.
 */

class GMail {

    private final String emailPort = "587";// gmail's smtp port
    private final String smtpAuth = "true";
    private final String starttls = "true";
    private final String emailHost = "smtp.gmail.com";

    private String fromEmail;
    private String fromPassword;
    private ArrayList<String> toEmailList;
    private String emailSubject;
    private String emailBody;

    private Properties emailProperties;
    private Session mailSession;
    private MimeMessage emailMessage;
    private String attachment;
    private Date triggerDate;

    GMail(String fromEmail, String fromPassword,
          ArrayList toEmailList, String emailSubject, String emailBody, String attachment, Date triggerDate) {
        this.fromEmail = fromEmail;
        this.fromPassword = fromPassword;
        this.toEmailList = toEmailList;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.attachment = attachment;
        this.triggerDate = triggerDate;
        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", smtpAuth);
        emailProperties.put("mail.smtp.starttls.enable", starttls);
        Log.i("GMail", "Mail server properties set.");
    }

    void createEmailMessage() throws AddressException,
            MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);

        emailMessage.setFrom(new InternetAddress(fromEmail, fromEmail));
        for (String toEmail : toEmailList) {
            Log.i("GMail","toEmail: "+toEmail);
            emailMessage.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(toEmail));
        }
        MimeBodyPart messageBodyPart = new MimeBodyPart();

        Multipart multipart = new MimeMultipart();
        emailMessage.setSubject(emailSubject);
        DataSource source = new FileDataSource(attachment);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(triggerDate.toString());
        multipart.addBodyPart(messageBodyPart);
        emailMessage.setContent(multipart);// for a html email
        // emailMessage.setText(emailBody);// for a text email
        Log.i("GMail", "Email Message created.");
    }

    void sendEmail() throws AddressException, MessagingException {

        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromEmail, fromPassword);
        Log.i("GMail","allrecipients: "+ Arrays.toString(emailMessage.getAllRecipients()));
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
        Log.i("GMail", "Email sent successfully.");
    }

}
