package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Util;
import models.Grp;
import models.GrpUsr;
import models.Usr;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import views.html.*;

import javax.persistence.OptimisticLockException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by admin on 6/8/2016.
 */
public class GroupController  extends Controller {

    @Security.Authenticated(Secured.class)
    public Result showGroupsPage() {return ok(groups.render());}

    @Security.Authenticated(Secured.class)
    public synchronized Result addGroup() {
        JsonNode json;
        try {
            json = request().body().asJson();
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("Incorrect group message"));
        }

        String groupId = json.findPath("groupId").asText();
        if (groupId == null || groupId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect group id"));

        try {
            if (Grp.find.where().eq("groupId", groupId).findUnique() != null)
                return badRequest(Util.getJSONObj(groupId + " group already exists"));

            Grp grp = new Grp();
            grp.setGroupId(groupId);
            grp.setOwnerId(session("userId"));
            GrpUsr gu = new GrpUsr();
            gu.setUserId(session("userId"));
            grp.addUser(gu);
            grp.insert();
            return ok();
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while adding the group"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result deleteGroup(String groupId) {
        if (groupId == null || groupId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect group id"));

        try {
            Grp grp = Grp.find.where().eq("groupId", groupId).findUnique();
            if ( grp == null)
                return badRequest(Util.getJSONObj(groupId + " group does not exist"));
            while (true) {
                try {
                    grp.delete();
                    return ok();
                } catch (OptimisticLockException oE) {
                    grp = Grp.find.where().eq("groupId", groupId).findUnique();
                    if (grp == null)
                        return badRequest(Util.getJSONObj(groupId + " group does not exist"));
                    continue;
                }
            }
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while deleting the group"));
        }
    }

    private ArrayNode getJsonMyGroups(List<GrpUsr> lGrpUsr) {
        ArrayNode res = Json.newArray();
        Iterator<GrpUsr> itGrpUsr = lGrpUsr.iterator();
        while (itGrpUsr.hasNext()) {
            String gId = itGrpUsr.next().getGroup().getGroupId();
            ObjectNode e = res.addObject();
            e.put("groupId", gId);
        }
        return res;
    }

    public Result getMyGroups(String userId) {
        try {
            List<GrpUsr> lGrpUsr = GrpUsr.find.where().eq("userId", userId.toLowerCase()).findList();
            return ok(getJsonMyGroups(lGrpUsr));
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while retrieving the groups of the user"));
        }
    }

    private ArrayNode getJsonGroups(List<Grp> lGrp) {
        ArrayNode res = Json.newArray();
        Iterator<Grp> itGrps = lGrp.iterator();
        while (itGrps.hasNext()) {
            ObjectNode e = res.addObject();
            e.put("groupId", itGrps.next().getGroupId());
        }
        return res;
    }

    @Security.Authenticated(Secured.class)
    public Result getOwnedGroups() {
        try {
            List<Grp> lGrp = Grp.find.where().eq("ownerId", session("userId")).findList();
            return ok(getJsonGroups(lGrp));
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while getting the owned groups"));
        }
    }

    private ArrayNode getJsonGroupUsers(List<GrpUsr> lGrpUsr, boolean bExcludeOwner, String ownerId) {
        ArrayNode res = Json.newArray();
        Iterator<GrpUsr> itGrpUsr = lGrpUsr.iterator();
        while (itGrpUsr.hasNext()) {
            String u = itGrpUsr.next().getUserId();
            if (bExcludeOwner && ownerId.equals(u))
                continue;
            ObjectNode e = res.addObject();
            e.put("userId", u);
        }
        return res;
    }

    @Security.Authenticated(Secured.class)
    public Result getGroupUsers(String groupId) {
        if (groupId == null || groupId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect group id"));
        try {
            Grp grp = Grp.find.where().eq("groupId", groupId).findUnique();
            if ( grp == null)
                return badRequest(Util.getJSONObj(groupId + " group does not exist"));
            return ok(getJsonGroupUsers(grp.getUsers(), true, session("userId")));
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while deleting the group"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result deleteGroupUser(String groupId, String userId) {
        if (groupId == null || groupId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect group id"));

        if (userId == null || userId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect user id"));

        try {
            userId = userId.toLowerCase();
            Grp grp = Grp.find.where().eq("groupId", groupId).findUnique();
            if ( grp == null)
                return badRequest(Util.getJSONObj(groupId + " group does not exist"));

            // delete the user in the group
            GrpUsr gu;
        retry:
            while (true) {
                gu = grp.getGrpUsr(userId);
                if (gu == null)
                    return badRequest(Util.getJSONObj(userId + " does not exist"));
                else {
                    grp.removeUser(gu);
                    try {
                        grp.update();
                        return ok();
                    } catch (OptimisticLockException oE) {
                        grp = Grp.find.where().eq("groupId", groupId).findUnique();
                        if (grp == null)
                            return badRequest(Util.getJSONObj(groupId + " group does not exist"));
                        continue retry;
                    }
                }
            }
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while deleting the user in the group"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result addUserToGroup() {
        JsonNode json;
        try {
            json = request().body().asJson();
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("Incorrect add user message"));
        }
        String groupId = json.findPath("groupId").asText();
        if (groupId == null || groupId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect group id"));

        String userId = json.findPath("userId").asText();
        if (userId == null || userId.isEmpty())
            return badRequest(Util.getJSONObj("Incorrect user id"));

        try {
            userId = userId.toLowerCase();
            Grp grp = Grp.find.where().eq("groupId", groupId).findUnique();
            if ( grp == null)
                return badRequest(Util.getJSONObj(groupId + " group does not exist"));

            GrpUsr gu = new GrpUsr();
            gu.setUserId(userId);
            while(true) {
                try {
                    Usr usr = Usr.find.where().eq("userId", userId).findUnique();
                    if ( usr == null)
                        return badRequest(Util.getJSONObj(userId + " does not exist"));

                    if (grp.isMember(userId))
                        return badRequest(Util.getJSONObj(userId + " already in group"));
                    grp.addUser(gu);
                    grp.update();
                    return ok();
                } catch (OptimisticLockException oE) {
                    grp = Grp.find.where().eq("groupId", groupId).findUnique();
                    if (grp == null)
                        return badRequest(Util.getJSONObj(groupId + " group does not exist"));
                    continue;
                }
            }
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while deleting the user in the group"));
        }
    }

    private ArrayNode getJsonUsers(boolean bAdmin) {
        List<Usr> users = Usr.find.where().findList();
        ArrayNode res = Json.newArray();
        if(users != null) {
            Iterator<Usr> itUsers = users.iterator();
            while (itUsers.hasNext()) {
                ObjectNode e = res.addObject();
                e.put("userId", itUsers.next().userId);
            }
        }
        return res;
    }

    @Security.Authenticated(Secured.class)
    public Result getUsers() {
        try {
            ArrayNode res = getJsonUsers(false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting all the users"));
        }
    }
}
