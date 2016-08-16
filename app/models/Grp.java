package models;

import com.avaje.ebean.Model;
import helpers.Util;

import javax.persistence.*;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * Created by admin on 6/8/2016.
 */
@Entity
public class Grp extends Model {
    @Id
    @Column(name="id")
    Integer id;

    @Column(name="group_id", length=255)
    private String groupId;

    @Column(name="owner_id", length=255)
    private String ownerId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="group")
    private List<GrpUsr> users;

    @Version
    private LocalDateTime lastUpdated;

    private synchronized static LocalDateTime getTimeStamp() throws InterruptedException {
        Thread.sleep(1);
        LocalDateTime inst = LocalDateTime.now(DateTimeZone.UTC);
        return inst;
    }

    @PrePersist
    public void setLastUpdated() throws InterruptedException {
        lastUpdated = getTimeStamp();
    }

    public Integer getId(){ return id; }

    public void setGroupId(String groupId){
        this.groupId = groupId;
    }

    public void setOwnerId(String ownerId){
        this.ownerId = ownerId;
    }

    public String getGroupId(){
        return this.groupId;
    }

    public List<GrpUsr> getUsers(){
        return this.users;
    }

    public void addUser(GrpUsr u) {
        users.add(u);
        if (u.getGroup() != this)
            u.setGroup(this);
    }

    public void removeUser(GrpUsr c) {
        users.remove(c);
    }

    public GrpUsr getGrpUsr(String uId) {
        Iterator<GrpUsr> itC = users.iterator();
        GrpUsr u;
        while(itC.hasNext()) {
            u = itC.next();
            if (u.getUserId().equals(uId))
                return u;
        }
        return null;
    }

    public boolean isMember(String uId) {
        Iterator<GrpUsr> itC = users.iterator();
        GrpUsr u;
        while(itC.hasNext()) {
            u = itC.next();
            if (u.getUserId().equals(uId))
                return true;
        }
        return false;
    }


    public static Finder<String, Grp> find = new Finder<String, Grp>(Grp.class);
}
