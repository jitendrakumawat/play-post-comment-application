package helpers;

import models.Grp;
import models.GrpUsr;
import models.Usr;

/**
 * Created by admin on 6/9/2016.
 */
public class SessionUser {
    private boolean isAdmin;
    private Integer gId;
    private String uId;

    private static final String INDIVIDUAL = "None";
    private static final int INDIVIDUAL_GID = 0;
    public static final int U_VALID = 0;
    public static final int U_NO = 1;
    public static final int G_NO = 2;
    public static final int G_U_NO = 3;
    public static final int BAD_U_OR_G = 4;
    public static final int U_NO_ADMIN = 5;


    public SessionUser(String userId, String pwd, String groupId, String sessionUserId) throws Exception{
        if (userId == null || pwd == null || groupId == null)
            throw new Exception("Incorrect User id or Group Id or Password");

        Usr u = Usr.find.where().eq("userId",userId).findUnique();
        if (u == null)
            throw new Exception("User id does not exist");

        if (groupId.isEmpty())
            gId = new Integer(INDIVIDUAL_GID);
        else {
            Grp g = Grp.find.where().eq("groupId", groupId).findUnique();
            if (g == null)
                throw new Exception("Group id does not exist");
            if (!g.isMember(userId))
                throw new Exception("User does not exist in the group");
            gId = new Integer(g.getId());
        }
        isAdmin = u.isAdmin;
        uId = userId.toLowerCase();

        // authenticate
        if (sessionUserId != null && !sessionUserId.equals(uId))
            throw new Exception(sessionUserId + " has signed in. Sign out and retry");

        if (!u.password.equals(pwd))
            throw new Exception("Incorrect User id or Password");

        // authenticated here
    }

    public SessionUser(String userId, String groupId) throws Exception {

        if (userId == null || groupId == null)
            throw new Exception("Incorrect User id or Group Id");

        Usr u = Usr.find.where().eq("userId",userId).findUnique();
        if (u == null)
            throw new Exception("User id does not exist");

        if (groupId.isEmpty())
            gId = new Integer(INDIVIDUAL_GID);
        else {
            Grp g = Grp.find.where().eq("groupId", groupId).findUnique();
            if (g == null)
                throw new Exception("Group id does not exist");
            if (!g.isMember(userId))
                throw new Exception("User does not exist in the group");
            gId = new Integer(g.getId());
        }
        isAdmin = u.isAdmin;
        uId = userId;
    }

    public static int validateSessionUser(String userId, String groupId, boolean checkAdmin) {
        if (userId == null || groupId == null)
            return BAD_U_OR_G;

        Usr u = Usr.find.where().eq("userId",userId).findUnique();
        if (u == null)
            return U_NO;

        if (!groupId.isEmpty()){
            Grp g = Grp.find.where().eq("groupId", groupId).findUnique();
            if (g == null)
                return G_NO;

            if (!g.isMember(userId))
                return G_U_NO;
        }
        if (checkAdmin)
            return u.isAdmin ? U_VALID : U_NO_ADMIN;

        return U_VALID;
    }


    public boolean isIndividual() {
        return this.gId == INDIVIDUAL_GID;
    }

    public static Integer getIndividualGId() {
        return new Integer(INDIVIDUAL_GID);
    }
    public static String getIndividualGroupId() {
        return INDIVIDUAL;
    }

    public Integer getGId() {
        return gId;
    }

    public String getUId() {
        return uId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
