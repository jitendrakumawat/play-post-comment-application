package models;

import com.avaje.ebean.Model;
import play.data.format.*;
import play.data.validation.*;

import java.util.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;


/**
 * Created by admin on 5/17/2016.
 */
@Entity
public class Usr extends Model {

    @Id
    public Integer id;

    @Column(name="user_id", length=255)
    public String userId;

    @Column(name="pwd")
    public String password;

    @Column(name="is_admin")
    public boolean isAdmin;

    public static Finder<String, Usr> find = new Finder<String, Usr>(Usr.class);
}