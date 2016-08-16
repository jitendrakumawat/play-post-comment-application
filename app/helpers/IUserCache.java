package helpers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.ImplementedBy;

/**
 * Created by admin on 6/3/2016.
 */
@ImplementedBy(UserCache.class)
public interface IUserCache {
    void setUserInCache(String userId);
    void removeUserInCache(String userId);
    void forceRemoveUserInCache(String userId);
    ArrayNode getUsers();
}
