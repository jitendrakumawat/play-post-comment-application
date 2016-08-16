# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table comment (
  id                            bigint not null,
  post_id                       bigint,
  comment_msg                   varchar(500),
  commented_on                  timestamp,
  user_id                       varchar(255),
  constraint pk_comment primary key (id)
);
create sequence comment_seq;

create table grp (
  id                            serial not null,
  group_id                      varchar(255),
  owner_id                      varchar(255),
  last_updated                  timestamp not null,
  constraint pk_grp primary key (id)
);
insert into grp values (0, 'none', 'any', now());

create table grp_usr (
  id                            serial not null,
  user_id                       varchar(255),
  group_id                      integer,
  constraint pk_grp_usr primary key (id)
);

create table post (
  id                            bigint not null,
  post_msg                      varchar(500),
  post_type                     smallint,
  posted_on                     timestamp,
  user_id                       varchar(255),
  group_id                      integer,
  last_updated                  timestamp not null,
  constraint pk_post primary key (id)
);
create sequence post_seq;

create table usr (
  id                            serial not null,
  user_id                       varchar(255),
  pwd                           varchar(255),
  is_admin                      boolean,
  constraint pk_usr primary key (id)
);

alter table comment add constraint fk_comment_post_id foreign key (post_id) references post (id) on delete restrict on update restrict;
create index ix_comment_post_id on comment (post_id);

alter table grp_usr add constraint fk_grp_usr_group_id foreign key (group_id) references grp (id) on delete restrict on update restrict;
create index ix_grp_usr_group_id on grp_usr (group_id);


# --- !Downs

alter table if exists comment drop constraint if exists fk_comment_post_id;
drop index if exists ix_comment_post_id;

alter table if exists grp_usr drop constraint if exists fk_grp_usr_group_id;
drop index if exists ix_grp_usr_group_id;

drop table if exists comment cascade;
drop sequence if exists comment_seq;

drop table if exists grp cascade;

drop table if exists grp_usr cascade;

drop table if exists post cascade;
drop sequence if exists post_seq;

drop table if exists usr cascade;

