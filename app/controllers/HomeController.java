package controllers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import helpers.IUserCache;
import helpers.SessionUser;
import helpers.Util;


import play.mvc.*;

import views.html.*;



/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController  extends Controller {
    @Inject
    private IUserCache uC;

    public final static String forbiddenMessage =   "You are not logged in or your account has been deleted " +
                                                    "or your logged in group has been deleted or you are not " +
                                                    "a member of the logged in group anymore.";

    public Result showHomePage() {
        return ok(home.render());
    }

    public Result showForbiddenPage() {return ok(forbidden.render(forbiddenMessage));}

    @BodyParser.Of(BodyParser.Json.class)
    public Result login() {
        JsonNode json = request().body().asJson();
        String userId = json.findPath("userId").asText();
        String password = json.findPath("password").asText();
        String groupId = json.findPath("groupId").asText();

        try {
            SessionUser sU = new SessionUser(userId, password, groupId, session("userId"));

            if (session("userId") == null)
                uC.setUserInCache(sU.getUId());
            session().clear();
            session("userId", sU.getUId());
            session("groupId", groupId);
            return ok();
        } catch(Exception e) {
            return badRequest(Util.getJSONObj(e.getMessage()));
        }
    }


    @Security.Authenticated(Secured.class)
    public Result logout() {
        if (session("userId") != null)
            uC.removeUserInCache(session("userId"));
        session().clear();
        return redirect(routes.HomeController.showHomePage());
    }

    public Result getUserSummary() {
        return ok(uC.getUsers());
    }
}
