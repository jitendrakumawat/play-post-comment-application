package models;

import com.avaje.ebean.Model;
import helpers.Util;
import org.joda.time.DateTimeZone;
import play.data.validation.*;

import java.math.BigInteger;
import java.util.*;
import javax.persistence.*;
import org.joda.time.LocalDateTime;

/**
 * Created by admin on 5/17/2016.
 */
@Entity
public class Post extends Model {
    public static final short BROADCAST = 1;
    public static final short NORMAL = 2;
    public static final short REM_C_OK = 1;
    public static final short REM_C_NO = 2;

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public BigInteger id;

    @Column(name="post_msg",length=500)
    @Constraints.Required
    public String post;

    @Column(name="post_type")
    public short postType;

    @Column(name="posted_on")
    public LocalDateTime postedOn;

    @Column(name="user_id", length=255)
    public String userId;

    @Column(name="group_id")
    public int groupId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="post_id")
    public List<Comment> comments;

    @Version
    public LocalDateTime lastUpdated;

    private synchronized static LocalDateTime getTimeStamp() throws InterruptedException {
        Thread.sleep(1);
        LocalDateTime inst = LocalDateTime.now(DateTimeZone.UTC);
        return inst;
    }

    @PrePersist
    public void setPostedOn() {
        postedOn = Util.getTimeStamp();
    }

    @PrePersist
    public void setLastUpdated() throws InterruptedException {
        lastUpdated = getTimeStamp();
    }

    public void setPost(String post) { this.post = post; }

    public void setType(short postType) { this.postType = postType; }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public void setGroupId(int groupId){
        this.groupId = groupId;
    }

    public void addComment(Comment c) {
        comments.add(c);
        if (c.getPost() != this)
            c.setPost(this);
    }

    public void removeComment(Comment c) {
        comments.remove(c);
    }

    public short removeComment(BigInteger biC) {
        Iterator<Comment> itC = comments.iterator();
        Comment c;
        while(itC.hasNext()) {
            c = itC.next();
            if (c.id.equals(biC)) {
                comments.remove(c);
                return REM_C_OK;
            }
        }
        return REM_C_NO;
    }

    public Comment getComment(BigInteger biC) {
        Iterator<Comment> itC = comments.iterator();
        Comment c;
        while(itC.hasNext()) {
            c = itC.next();
            if (c.id.equals(biC)) {
                return c;
            }
        }
        return null;
    }

    public static Finder<String, Post> find = new Finder<String, Post>(Post.class);
}
