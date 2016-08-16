package controllers;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import helpers.IMailer;
import helpers.IUserCache;
import helpers.SessionUser;
import helpers.Util;

import models.Comment;
import models.Grp;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.libs.Json;
import play.mvc.*;

import models.Post;

import views.html.*;

import javax.persistence.OptimisticLockException;


/**
 * Created by admin on 5/17/2016.
 */
public class PostController  extends Controller {

    @Inject
    private IUserCache uC;

    @Inject
    private IMailer iM;

    private static final int pageSize = 5;
    private static final BigInteger biStep = new BigInteger("10000");
    private static final BigInteger biNoRecord = new BigInteger("-1");
    private static final BigInteger biZero = new BigInteger("0");

    private void broadcastPost(Post post) {
        String sub = "Post by " + session("userId") + " on " +  Util.formatLocalDateTime(post.postedOn);
        iM.sendToAll(session("userId"), sub, post.post);
    }

    private class PostComparator implements Comparator<Post> {
        public int compare(Post p1, Post p2) {
            int cR = p1.id.compareTo(p2.id);
            if (cR < 0)
                cR = 1;
            else if (cR > 0)
                cR = -1;
            return cR;
        }
    }

    @Security.Authenticated(Secured.class)
    public Result addPost(){
        JsonNode json;
        try {
            json = request().body().asJson();
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("Invalid post message"));
        }
        String pm = json.findPath("post").asText();
        if (pm == null || pm.equals(""))
            return badRequest(Util.getJSONObj("Invalid post message"));

        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            Post post = new Post();
            post.setPost(pm);
            post.setUserId(sU.getUId());
            post.setGroupId(sU.getGId());
            post.setType(Post.NORMAL);
            post.insert();
            return ok();
        } catch(Exception e) {
            return badRequest(Util.getJSONObj("A technical error has occurred while saving the post"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result addPostAndBroadcast(){
        JsonNode json;
        try {
            json = request().body().asJson();
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("Invalid post message"));
        }
        String pm = json.findPath("post").asText();
        if (pm == null || pm.equals(""))
            return badRequest(Util.getJSONObj("Invalid post message"));

        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            Post post = new Post();
            post.setPost(pm);
            post.setUserId(sU.getUId());
            post.setGroupId(SessionUser.getIndividualGId());
            post.setType(Post.BROADCAST);
            post.insert();
            broadcastPost(post);
            return ok();
        } catch(Exception e) {
            e.printStackTrace();
            return badRequest(Util.getJSONObj("A technical error has occurred while saving the post"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result deletePost(String postId) {
        try {
            Post p = Post.find.byId(postId);
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            if (p != null && (p.userId.equals(sU.getUId()) || sU.isAdmin())) {
                while (true) {
                    try {
                        p.delete();
                        return ok();
                    } catch (OptimisticLockException oE) {
                        p = Post.find.byId(postId);
                        if (p == null)
                            return badRequest(Util.getJSONObj("Post does not exist"));
                        continue;
                    }
                }
            } else if (p == null) {
                return badRequest(Util.getJSONObj("Post does not exist"));
            } else // (!p.userId.equals(session("userId")))
                return badRequest(Util.getJSONObj("You can delete only your post"));
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while deleting the post"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getPosts() {
        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            return ok(posts.render(new Boolean(sU.isAdmin())));
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the post page"));
        }
    }

    private BigInteger getLastId(boolean bForGroup, Integer groupId, String userId, boolean bUserOnly, boolean bOnlyBroadcastedPost) {
        ExpressionList<Post> eL;
        if (bForGroup)
            eL = Post.find.setMaxRows(1).where().eq("groupId", groupId);
        else
            eL = Post.find.setMaxRows(1).where();

        if (bUserOnly)
            eL = eL.eq("userId", userId);

        if (bOnlyBroadcastedPost)
            eL = eL.eq("postType", Post.BROADCAST);

        List<Post> l = eL.orderBy("id desc").findList();
        if (l.size() == 0)
            return biNoRecord;
        else
            return l.get(0).id;
    }

    private BigInteger getFirstId(boolean bForGroup, Integer groupId, String userId, boolean bUserOnly, boolean bOnlyBroadcastedPost) {
        ExpressionList<Post> eL;
        if (bForGroup)
            eL = Post.find.setMaxRows(1).where().eq("groupId", groupId);
        else
            eL = Post.find.setMaxRows(1).where();

        if (bUserOnly)
            eL = eL.eq("userId", userId);

        if (bOnlyBroadcastedPost)
            eL = eL.eq("postType", Post.BROADCAST);

        List<Post> l = eL.orderBy("id asc").findList();
        if (l.size() == 0)
            return biNoRecord;
        else
            return l.get(0).id;
    }

    private JsonNode getPageJson(String id, boolean bIncludeId, boolean back, Integer groupId, boolean bForGroup, String userId, boolean bUserOnly, boolean bOnlyBroadcastedPost) throws Exception {
        BigInteger bId = new BigInteger(id);
        ExpressionList<Post> eL, eL1;
        List<Post> psts;
        BigInteger bO;
        boolean bBreak = false;
        if (back) {
            bO = bId.subtract(biStep);
            if (bO.compareTo(biZero) < 0) {
                bO = new BigInteger("0");
            }
        }
        else
            bO = bId.add(biStep);

        do {
            if (bForGroup)
                eL = Post.find.setMaxRows(pageSize).where().eq("groupId", new Integer(groupId));
            else
                eL = Post.find.setMaxRows(pageSize).where();

            if (bUserOnly)
                eL = eL.eq("userId", userId);

            if (bOnlyBroadcastedPost)
                eL = eL.eq("postType", Post.BROADCAST);

            if (back)
                psts = bIncludeId ? eL.le("id", bId).ge("id", bO).orderBy("id desc").findList() : eL.lt("id", bId).ge("id", bO).orderBy("id desc").findList();
            else
                psts = bIncludeId ? eL.ge("id", bId).le("id", bO).orderBy("id asc").findList() : eL.gt("id", bId).le("id", bO).orderBy("id asc").findList();

            if (back) {
                BigInteger biFId = getFirstId(bForGroup, groupId, userId, bUserOnly, bOnlyBroadcastedPost);
                if (biFId.compareTo(biNoRecord) == 0) {
                    psts = null;
                    bBreak = true;
                } else if (bO.compareTo(biFId) <= 0) {
                    bBreak = true;
                } else {
                    bO = bId.subtract(biStep);
                    if (bO.compareTo(biZero) < 0) {
                        bO = new BigInteger("0");
                    }
                }
            }
            else {
                BigInteger biLId = getLastId(bForGroup, groupId, userId, bUserOnly, bOnlyBroadcastedPost);
                if (biLId.compareTo(biNoRecord) == 0) {
                    psts = null;
                    bBreak = true;
                } else if (bO.compareTo(biLId) >= 0 && psts.size() != pageSize) {
                    if (bForGroup)
                        eL1 = Post.find.setMaxRows(pageSize).where().eq("groupId", groupId);
                    else
                        eL1 = Post.find.setMaxRows(pageSize).where();

                    if (bUserOnly)
                        eL1 = eL1.eq("userId", userId);

                    if (bOnlyBroadcastedPost)
                        eL1 = eL1.eq("postType", Post.BROADCAST);

                    psts = eL1.orderBy("id desc").findList();
                    bBreak = true;
                } else
                    bO = bId.add(biStep);
            }
        } while(!bBreak && psts.size() != pageSize);

        ObjectNode result = Json.newObject();

        if (psts == null || psts.size() == 0) {
            result.put("numPosts", "0");
            result.put("arr", Json.newArray());
            result.put("beginId", biNoRecord.toString());
            result.put("endId", biNoRecord.toString());
            result.put("bLastPage", false);
        } else {
            if (!back)
                psts.sort(new PostComparator());

            BigInteger biLId = getLastId(bForGroup, groupId, userId, bUserOnly, bOnlyBroadcastedPost);
            if (biLId.compareTo(biNoRecord) == 0)
                result.put("numPosts", "0");
            else {
                int c;
                if (bForGroup)
                    eL1 = Post.find.setMaxRows(pageSize).where().eq("groupId", groupId);
                else
                    eL1 = Post.find.setMaxRows(pageSize).where();

                if (bUserOnly)
                    eL1 = eL1.eq("userId", userId);

                if (bOnlyBroadcastedPost)
                    eL1 = eL1.eq("postType", Post.BROADCAST);

                c = eL1.findRowCount();
                result.put("numPosts", new Integer(c).toString());
            }

            if(psts.get(psts.size()-1).id.compareTo(getFirstId(bForGroup, groupId, userId, bUserOnly, bOnlyBroadcastedPost)) <= 0)
                result.put("bLastPage", true);
            else
                result.put("bLastPage", false);
            result.put("beginId", psts.get(0).id.toString());
            result.put("endId", psts.get(psts.size()-1).id.toString());
            ArrayNode arr = Json.newArray();
            Iterator<Post> it = psts.iterator();
            while(it.hasNext()) {
                Post pst = it.next();
                ObjectNode e = arr.addObject();
                e.put("post", pst.post);
                e.put("postedOn", Util.toSessionTimeZone(pst.postedOn, request().cookie("tzoffset").value()));
                e.put("userId", pst.userId);
                e.put("id", pst.id.toString());
            }
            result.put("arr", arr);
        }
        return result;
    }

    @Security.Authenticated(Secured.class)
    public Result refreshPage(String endId) {
        try {
            BigInteger eId = new BigInteger(endId);
            if (eId.compareTo(biNoRecord) <= 0)
                return badRequest(Util.getJSONObj("Invalid post id"));

            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            boolean bGroupFilter = (sU.isAdmin() && sU.isIndividual()) ? false : true;
            JsonNode res = getPageJson(eId.toString(), true, false, sU.getGId(), bGroupFilter, null, false, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while refreshing the page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getFirstPage() {
        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            boolean bGroupFilter = (sU.isAdmin() && sU.isIndividual()) ? false : true;
            JsonNode res = getPageJson(getLastId(bGroupFilter, sU.getGId(), null, false, false).toString(), false, false, sU.getGId(), bGroupFilter, null, false, false);
            return ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(Util.getJSONObj("A technical error occurred while getting the first page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getPreviousPage(String firstId) {
        try {
            if (new BigInteger(firstId).compareTo(biNoRecord) <= 0)
                return getFirstPage();
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            boolean bGroupFilter = (sU.isAdmin() && sU.isIndividual()) ? false : true;
            JsonNode res = getPageJson(firstId, false, true, sU.getGId(), bGroupFilter, null, false, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the previous page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getNextPage(String lastId) {
        try {
            if (new BigInteger(lastId).compareTo(biNoRecord) <= 0)
                return getFirstPage();
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            boolean bGroupFilter = (sU.isAdmin() && sU.isIndividual()) ? false : true;
            JsonNode res = getPageJson(lastId, false, false, sU.getGId(), bGroupFilter, null, false, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the next page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result refreshPageUser(String endId) {
        try {
            BigInteger eId = new BigInteger(endId);
            if (eId.compareTo(biNoRecord) <= 0)
                return badRequest(Util.getJSONObj("Invalid post id"));
            JsonNode res = getPageJson(eId.toString(), true, false, SessionUser.getIndividualGId(), true, session("userId"), true, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while refreshing the page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getFirstPageUser() {
        try {
            JsonNode res = getPageJson(getLastId(true, SessionUser.getIndividualGId(), session("userId"), true, false).toString(), false, false, SessionUser.getIndividualGId(), true, session("userId"), true, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the first page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getPreviousPageUser(String firstId) {
        try {
            if (new BigInteger(firstId).compareTo(biNoRecord) <= 0)
                return getFirstPageUser();
            JsonNode res = getPageJson(firstId, false, true, SessionUser.getIndividualGId(), true, session("userId"), true, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the previous page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getNextPageUser(String lastId) {
        try {
            if (new BigInteger(lastId).compareTo(biNoRecord) <= 0)
                return getFirstPageUser();
            JsonNode res = getPageJson(lastId, false, false, SessionUser.getIndividualGId(), true, session("userId"), true, false);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the next page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result refreshPageUserBroadcast(String endId) {
        try {
            BigInteger eId = new BigInteger(endId);
            if (eId.compareTo(biNoRecord) <= 0)
                return badRequest(Util.getJSONObj("Invalid post id"));
            JsonNode res = getPageJson(eId.toString(), true, false, SessionUser.getIndividualGId(), true, session("userId"), true, true);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while refreshing the page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getFirstPageUserBroadcast() {
        try {
            JsonNode res = getPageJson(getLastId(true, SessionUser.getIndividualGId(), session("userId"), true, true).toString(), false, false, SessionUser.getIndividualGId(), true, session("userId"), true, true);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the first page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getPreviousPageUserBroadcast(String firstId) {
        try {
            if (new BigInteger(firstId).compareTo(biNoRecord) <= 0)
                return getFirstPageUser();
            JsonNode res = getPageJson(firstId, false, true, SessionUser.getIndividualGId(), true, session("userId"), true, true);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the previous page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getNextPageUserBroadcast(String lastId) {
        try {
            if (new BigInteger(lastId).compareTo(biNoRecord) <= 0)
                return getFirstPageUser();
            JsonNode res = getPageJson(lastId, false, false, SessionUser.getIndividualGId(), true, session("userId"), true, true);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the next page"));
        }
    }
}
