package models;

import com.avaje.ebean.Model;

import javax.persistence.*;

/**
 * Created by admin on 6/8/2016.
 */
@Entity
public class GrpUsr extends Model {
    @Id
    Integer id;

    @Column(name="user_id", length=255)
    private String userId;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="group_id")
    public Grp group;

    public Grp getGroup() { return this.group; }

    public void setGroup(Grp group) { this.group = group; }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public String getUserId(){
        return this.userId;
    }

    public static Finder<String, GrpUsr> find = new Finder<String, GrpUsr>(GrpUsr.class);
}

