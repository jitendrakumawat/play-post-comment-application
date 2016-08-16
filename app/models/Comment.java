package models;

import com.avaje.ebean.Model;
import helpers.Util;
import play.data.validation.*;

import java.math.BigInteger;
import org.joda.time.LocalDateTime;
import javax.persistence.*;


/**
 * Created by admin on 5/17/2016.
 */
@Entity
public class Comment extends Model {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public BigInteger id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="post_id")
    public Post post;

    @Column(name="comment_msg",length=500)
    @Constraints.Required
    public String comment;

    @Column(name="commented_on")
    public LocalDateTime commentedOn;

    @Column(name="user_id", length=255)
    public String userId;

    @PrePersist
    public void setCommentedOn() {
        commentedOn = Util.getTimeStamp();
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public Post getPost() { return this.post;}

    public void setPost(Post post) { this.post = post;}

    public void setComment(String comment){
        this.comment = comment;
    }

    public static Finder<String, Comment> find = new Finder<String, Comment>(Comment.class);
}
