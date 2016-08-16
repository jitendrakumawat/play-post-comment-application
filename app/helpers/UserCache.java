package helpers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.libs.Json;

import java.util.HashMap;
import java.util.Iterator;


/**
 * Created by admin on 6/3/2016.
 */
@Singleton
public class UserCache implements IUserCache{
    @Inject
    play.cache.CacheApi cache;

    // todo: concurrency in distributed mode
    public synchronized void setUserInCache(String userId) {
        HashMap<String, SessionUserStat> users = cache.get("users");
        if(users == null) {
            users = new HashMap<String, SessionUserStat>();
            cache.set("users", users);
        }
        if (users.containsKey(userId)) {
            SessionUserStat sUInMap = users.get(userId);
            sUInMap.incrementLoginCount();
            return;
        }

        users.put(userId, new SessionUserStat(userId));
    }

    public synchronized void removeUserInCache(String userId) {
        HashMap<String, SessionUserStat> users = cache.get("users");
        if(users == null)
            return;
        if (users.containsKey(userId)) {
            SessionUserStat sUInMap = users.get(userId);
            sUInMap.decrementLoginCount();
            if (sUInMap.getLoginCount() <= 0)
                users.remove(userId);
        }
    }

    public synchronized void forceRemoveUserInCache(String userId) {
        HashMap<String, SessionUserStat> users = cache.get("users");
        if(users == null)
            return;
        if (users.containsKey(userId)) {
            users.remove(userId);
        }
    }

    public synchronized ArrayNode getUsers() {
        HashMap<String, SessionUserStat> users = cache.get("users");
        ArrayNode res = Json.newArray();
        if(users != null) {
            Iterator<SessionUserStat> itUsers = users.values().iterator();
            while (itUsers.hasNext()) {
                ObjectNode e = res.addObject();
                SessionUserStat sU = itUsers.next();
                e.put("userId", sU.getUId());
                e.put("nSessions", sU.getLoginCount());
            }
        }
        return res;
    }
}
