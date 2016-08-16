package helpers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.ImplementedBy;

/**
 * Created by admin on 6/14/2016.
 */

@ImplementedBy(Mailer.class)
public interface IMailer {
    void sendEmail(String from, String to, String sub, String body);
    void sendToAll(String from, String sub, String body);
}
