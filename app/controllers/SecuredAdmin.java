package controllers;

import com.google.inject.Inject;
import helpers.IUserCache;
import helpers.SessionUser;
import models.Usr;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.util.List;

import views.html.*;

/**
 * Created by admin on 6/5/2016.
 */
public class SecuredAdmin  extends Security.Authenticator {
    @Inject
    private IUserCache uC;

    @Override
    public String getUsername(Http.Context ctx) {
        String u = ctx.session().get("userId");
        String g = ctx.session().get("groupId");
        int vC = SessionUser.validateSessionUser(u, g, true);
        if (vC == SessionUser.U_NO) {
            ctx.session().remove("userId");
            ctx.session().remove("groupId");
            uC.forceRemoveUserInCache(u);
            u = null;
        }
        else if (vC == SessionUser.G_NO || vC == SessionUser.G_U_NO) {
            ctx.session().remove("userId");
            ctx.session().remove("groupId");
            uC.removeUserInCache(u);
            u = null;
        }
        else if (vC == SessionUser.BAD_U_OR_G)
            u = null;
        else if (vC == SessionUser.U_NO_ADMIN){
            u = null;
        }
        return u;
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        return forbidden(forbidden.render(AdminController.forbiddenMessage));
    }
}
