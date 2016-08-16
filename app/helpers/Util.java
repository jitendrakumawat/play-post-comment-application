package helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;


import org.joda.time.LocalDateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by admin on 5/19/2016.
 */
public class Util {
    private final static String sDF = "HH:mm:ss:SSS EEE, d MMM yyyy";

    public static String toSessionTimeZone (LocalDateTime dateTime, String tZoneOffset) throws Exception {
        int lOffset = Integer.parseInt(tZoneOffset);

        LocalDateTime c;
        if (lOffset < 0)
            c = dateTime.plusMinutes(-lOffset);
        else
            c = dateTime.minusMinutes(lOffset);

        DateTimeFormatter formatter = DateTimeFormat.forPattern(sDF);
        return formatter.withZoneUTC().print(c);
    }

    public static String formatLocalDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(sDF);
        return formatter.withZoneUTC().print(dateTime);
    }

    public static LocalDateTime getTimeStamp()  {
        LocalDateTime inst = LocalDateTime.now(DateTimeZone.UTC);
        return inst;
    }

    public static ObjectNode getJSONObj(String s) {
        ObjectNode result = Json.newObject();
        result.put("error", s);
        return result;
    }
}
