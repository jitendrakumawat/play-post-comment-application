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
import helpers.IUserCache;
import helpers.SessionUser;
import helpers.Util;
import models.Post;

import play.libs.Json;
import play.mvc.*;
import models.Comment;

import javax.persistence.OptimisticLockException;


/**
 * Created by admin on 5/17/2016.
 */
public class CommentController  extends Controller {

    @Inject
    private IUserCache uC;

    private static int pageSize = 3;
    private static BigInteger biStep = new BigInteger("10000");
    private static BigInteger biNoRecord = new BigInteger("-1");
    private static BigInteger biZero = new BigInteger("0");

    private class CommentComparator implements Comparator<Comment> {
        public int compare(Comment c1, Comment c2) {
            int cR = c1.id.compareTo(c2.id);
            return cR;
        }
    }

    private Post getPost(String postId) {
        return Post.find.ref(postId);
    }

    @Security.Authenticated(Secured.class)
    public Result addComment(String postId) {
        Post p;
        if ((p = getPost(postId)) == null)
            return badRequest(Util.getJSONObj("Post does not exist"));

        JsonNode json;
        try {
            json = request().body().asJson();
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("Invalid comment message"));
        }
        String cm = json.findPath("comment").asText();
        if (cm == null || cm.equals(""))
            return badRequest(Util.getJSONObj("Invalid comment message"));

        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));
            Comment comment = new Comment();
            comment.setComment(cm);
            comment.setUserId(sU.getUId());
            while (true) {
                try {
                    p.addComment(comment);
                    p.update();
                    return ok();
                } catch (OptimisticLockException oE){
                    p = Post.find.ref(postId);
                    if (p == null)
                        return badRequest(Util.getJSONObj("Post does not exist"));
                    continue;
                }
            }
        } catch (Exception e) {
                e.printStackTrace();
                return badRequest(Util.getJSONObj("A technical error has occurred while saving the comment"));
        }

    }

    @Security.Authenticated(Secured.class)
    public Result deleteComment(String postId, String commentId) {
        Post p;
        if ((p = getPost(postId)) == null)
            return badRequest(Util.getJSONObj("Post does not exist"));

        try {
            SessionUser sU = new SessionUser(session("userId"), session("groupId"));

            Comment c;
            BigInteger biC = new BigInteger(commentId);
        retry:
            while(true) {
                c = p.getComment(biC);
                if (c == null)
                    return badRequest(Util.getJSONObj("Comment does not exist"));
                else if (c.userId.equals(sU.getUId()) || sU.isAdmin()) {
                    p.removeComment(c);
                    try {
                        p.update();
                        return ok();
                    } catch (OptimisticLockException oE) {
                        p = Post.find.ref(postId);
                        if (p == null)
                            return badRequest(Util.getJSONObj("Post does not exist"));
                        continue retry;
                    }
                } else
                    return badRequest(Util.getJSONObj("You can delete only your comment"));
            }
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while deleting the comment"));
        }
    }

    private BigInteger getLastId(String postId) {
        List<Comment> l = Comment.find.setMaxRows(1).where().eq("post.id", new BigInteger(postId)).orderBy("id desc").findList();
        if (l.size() == 0)
            return biNoRecord;
        else
            return l.get(0).id;
    }

    private BigInteger getFirstId(String postId) {
        List<Comment> l = Comment.find.setMaxRows(1).where().eq("post.id", new BigInteger(postId)).orderBy("id asc").findList();
        if (l.size() == 0)
            return biNoRecord;
        else
            return l.get(0).id;
    }

    private JsonNode getPageJson(String id, boolean bIncludeId, boolean back, String postId) throws Exception {
        BigInteger bId = new BigInteger(id);
        ExpressionList<Comment> eL;
        List<Comment> cmts;
        BigInteger bO;
        boolean bFirstPage = false;
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
            eL = Comment.find.setMaxRows(pageSize).where().eq("post.id", new BigInteger(postId));

            if (back)
                cmts = bIncludeId ? eL.le("id", bId).ge("id", bO).orderBy("id desc").findList() : eL.lt("id", bId).ge("id", bO).orderBy("id desc").findList();
            else
                cmts = bIncludeId ? eL.ge("id", bId).le("id", bO).orderBy("id asc").findList() : eL.gt("id", bId).le("id", bO).orderBy("id asc").findList();

            if (back) {
                BigInteger biFId = getFirstId(postId);
                if (biFId.compareTo(biNoRecord) == 0) {
                    cmts = null;
                    bBreak = true;
                } else if (bO.compareTo(biFId) <= 0) {
                    if (cmts.size() != pageSize) {
                        cmts = Comment.find.setMaxRows(pageSize).where().eq("post.id", new BigInteger(postId)).orderBy("id asc").findList();
                        bFirstPage = true;
                    }
                    bBreak = true;
                } else {
                    bO = bId.subtract(biStep);
                    if (bO.compareTo(biZero) < 0) {
                        bO = new BigInteger("0");
                    }
                }
            }
            else {
                BigInteger biLId = getLastId(postId);
                if (biLId.compareTo(biNoRecord) == 0) {
                    cmts = null;
                    bBreak = true;
                } else if (bO.compareTo(biLId) >= 0 && cmts.size() != pageSize) {
                    cmts = Comment.find.setMaxRows(pageSize).where().eq("post.id", new BigInteger(postId)).orderBy("id desc").findList();
                    cmts.sort(new CommentComparator()); // ascending order
                    bBreak = true;
                } else
                    bO = bId.add(biStep);
            }
        } while(!bBreak && cmts.size() != pageSize);

        ObjectNode result = Json.newObject();

        if (cmts == null || cmts.size() == 0) {
            result.put("numComments", "0");
            result.put("arr", Json.newArray());
            result.put("beginId", biNoRecord.toString());
            result.put("endId", biNoRecord.toString());
            result.put("bLastPage", false);
        } else {
            if (back && !bFirstPage)
                cmts.sort(new CommentComparator()); // ascending order

            BigInteger biLId = getLastId(postId);
            if (biLId.compareTo(biNoRecord) == 0)
                result.put("numComments", "0");
            else {
                int c = Comment.find.where().eq("post.id", new BigInteger(postId)).findRowCount();
                result.put("numComments", new Integer(c).toString());
            }

            if(cmts.get(0).id.compareTo(getFirstId(postId)) <= 0)
                result.put("bLastPage", true);
            else
                result.put("bLastPage", false);
            result.put("beginId", cmts.get(0).id.toString());
            result.put("endId", cmts.get(cmts.size()-1).id.toString());
            ArrayNode arr = Json.newArray();
            Iterator<Comment> it = cmts.iterator();
            while(it.hasNext()) {
                Comment cmt = it.next();
                ObjectNode e = arr.addObject();
                e.put("comment", cmt.comment);
                e.put("commentedOn", Util.toSessionTimeZone(cmt.commentedOn, request().cookie("tzoffset").value()));
                e.put("userId", cmt.userId);
                e.put("id", cmt.id.toString());
            }
            result.put("arr", arr);
        }
        return result;
    }

    @Security.Authenticated(Secured.class)
    public Result refreshPage(String postId, String beginId) {
        Post p;
        if ((p = getPost(postId)) == null)
            return badRequest(Util.getJSONObj("Post does not exist"));

        try {
            BigInteger eId = new BigInteger(beginId);
            if (eId.compareTo(biNoRecord) <= 0)
                return badRequest(Util.getJSONObj("Invalid comment id"));
            JsonNode res = getPageJson(eId.toString(), true, false, postId);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while refreshing the page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getFirstPage(String postId) {
        try {
            Post p;
            if ((p = getPost(postId)) == null)
                return badRequest(Util.getJSONObj("Post does not exist"));

            JsonNode res = getPageJson(getFirstId(postId).toString(), true, false, postId);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the first page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getLastPage(String postId) {
        try {
            Post p;
            if ((p = getPost(postId)) == null)
                return badRequest(Util.getJSONObj("Post does not exist"));

            JsonNode res = getPageJson(getLastId(postId).toString(), true, false, postId);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the last page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getPreviousPage(String postId, String firstId) {
        try {
            Post p;
            if (new BigInteger(firstId).compareTo(biNoRecord) <= 0)
                return getFirstPage(postId);
            if ((p = getPost(postId)) == null)
                return badRequest(Util.getJSONObj("Post does not exist"));
            JsonNode res = getPageJson(firstId, false, true, postId);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the previous page"));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result getNextPage(String postId, String lastId) {
        try {
            Post p;
            if (new BigInteger(lastId).compareTo(biNoRecord) <= 0)
                return getFirstPage(postId);
            if ((p = getPost(postId)) == null)
                return badRequest(Util.getJSONObj("Post does not exist"));
            JsonNode res = getPageJson(lastId, false, false, postId);
            return ok(res);
        } catch (Exception e) {
            return badRequest(Util.getJSONObj("A technical error occurred while getting the next page"));
        }
    }
}
