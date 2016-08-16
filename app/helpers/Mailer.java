package helpers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import models.Usr;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;

import java.util.Iterator;
import java.util.List;

@Singleton
public class Mailer implements IMailer {
    @Inject
    MailerClient mailerClient;

    public void sendEmail(String from, String to, String sub, String body) {
        Email email = new Email()
                .setSubject(sub)
                .setFrom(from)
                .addTo(to)
                // sends text, HTML or both...
                .setBodyText(sub)
                .setBodyHtml("<html><body><p>" + body + "</p></body></html>");
        mailerClient.send(email);
    }

    public void sendToAll(String from, String sub, String body) {
        List<Usr> lU = Usr.find.where().ne("userId", from).findList();
        Iterator<Usr> iU = lU.iterator();
        while (iU.hasNext()) {
            sendEmail(from, iU.next().userId, sub, body);
        }
    }
}
