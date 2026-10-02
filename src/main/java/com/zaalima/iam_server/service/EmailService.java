
package com.zaalima.iam_server.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final String sendGridApiKey;

    public EmailService() {

        sendGridApiKey =
                System.getenv("SENDGRID_API_KEY");

        if (sendGridApiKey == null ||
                sendGridApiKey.isBlank()) {

            throw new IllegalStateException(
                    "SENDGRID_API_KEY environment variable is not set"
            );
        }
    }

    public boolean sendOtpEmail(
            String toEmail,
            String otp) {

        try {

            Email from =
            		  new Email("neerajagosala236@gmail.com");

            Email to =
                    new Email(toEmail);

            String subject =
                    "Zaalima IAM - Your OTP";

            Content content =
                    new Content(
                            "text/plain",
                            "Your Zaalima IAM verification OTP is: "
                                    + otp
                                    + "\n\n"
                                    + "This OTP is valid for 5 minutes."
                    );

            Mail mail =
                    new Mail(
                            from,
                            subject,
                            to,
                            content
                    );

            SendGrid sendGrid =
                    new SendGrid(sendGridApiKey);

            Request request =
                    new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response =
                    sendGrid.api(request);

            System.out.println(
                    "SENDGRID STATUS: "
                            + response.getStatusCode()
            );

            System.out.println(
                    "SENDGRID BODY: "
                            + response.getBody()
            );

            System.out.println(
                    "SENDGRID HEADERS: "
                            + response.getHeaders()
            );

            return response.getStatusCode() >= 200
                    && response.getStatusCode() < 300;

        } catch (Exception e) {

            System.out.println(
                    "SENDGRID ERROR: "
                            + e.getMessage()
            );

            e.printStackTrace();

            return false;
        }
    }
}

