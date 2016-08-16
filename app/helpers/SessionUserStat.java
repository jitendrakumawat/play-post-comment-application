package helpers;


/**
 * Created by admin on 6/9/2016.
 */
public class SessionUserStat {
    private int nLoggedIn;
    private String userId;


    public SessionUserStat(String userId) {
        this.userId = new String(userId);
        nLoggedIn = 1;
    }

    public String getUId() { return this.userId;  }

    public void incrementLoginCount() {
        nLoggedIn++;
    }

    public void decrementLoginCount() {
        nLoggedIn--;
    }

    public int getLoginCount() {
        return nLoggedIn;
    }

}
