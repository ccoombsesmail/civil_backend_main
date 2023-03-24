CREATE TABLE "users" (
  id SERIAL PRIMARY KEY,
  clerk_id text NOT NULL,
  email text NOT NULL,
  username text NOT NULL,
  civility integer DEFAULT 0,
  created_at BIGINT NOT NULL,
  icon_src text,
  UNIQUE(username),
  UNIQUE(email)
);


CREATE TABLE topics (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title text NOT NULL,
    description text NOT NULL,
    summary text NOT NULL,
    category text NOT NULL,
    tweet_html text,
    yt_url text,
    content_url text,
    image_url text,
    vod_url text,
    evidence_links text[] DEFAULT '{}',
    likes integer DEFAULT 0,
    created_by text NOT NULL,
    clerk_id text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(title)
);


CREATE TABLE topic_vods (
  id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
  clerk_id text NOT NULL,
  vod_url text NOT NULL,
  topic_id uuid NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_topics
    FOREIGN KEY(topic_id) 
      REFERENCES topics(id)
);

CREATE TABLE sub_topics (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title text NOT NULL,
    created_by text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    topic_id uuid NOT NULL,
    CONSTRAINT fk_topics
      FOREIGN KEY(topic_id) 
	      REFERENCES topics(id)
);




CREATE TYPE sentiment AS ENUM ('POSITIVE', 'NEUTRAL', 'NEGATIVE', 'MEME');


CREATE TABLE comments (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text NOT NULL,
    created_by text NOT NULL,
    subtopic_id uuid NOT NULL,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    CONSTRAINT fk_sub_topics
      FOREIGN KEY(subtopic_id) 
	      REFERENCES sub_topics(id)
);


ALTER TABLE "users"
ADD COLUMN consortium_member boolean DEFAULT false;


ALTER TABLE sub_topics
DROP CONSTRAINT sub_topics_title_key;

ALTER TABLE comments
ADD COLUMN comment_level integer;

ALTER TABLE comments ALTER subtopic_id drop not null;

ALTER TABLE comments
ADD COLUMN source text;

ALTER TABLE "users"
DROP COLUMN civility;


ALTER TABLE topic_vods
ADD COLUMN topic_id uuid NOT NULL;

CREATE TABLE comment_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    UNIQUE(comment_id, user_id)
);


CREATE TABLE topic_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    topic_id uuid NOT NULL,
    UNIQUE(topic_id, user_id)
);

CREATE TABLE comment_civility (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    is_civil boolean,
    UNIQUE(comment_id , user_id)
);

CREATE INDEX user_id_topic_likes_index ON topic_likes (user_id);
CREATE INDEX user_id_comment_likes_index ON comment_likes (user_id);



CREATE TABLE follows (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    follower_id text NOT NULL
);

CREATE INDEX user_id_follows_index ON follows (user_id);
CREATE INDEX follower_id_follows_index ON follows (follower_id);



-- INSERT INTO subtopics (id,title,created_by,topic_id) VALUES ('86c570c2-7100-4737-9a0f-da0e7e1a61cc', 'subtopic1_on_topic1', 'user1', '86c570c2-7100-4737-9a0f-da0e7e1a61c2') RETURNING id, title, created_by, topic_id;

--  psql -U civil_user -h 127.0.0.1 -d civil -f src/main/resources/init.sql

--  psql -U civil_user -h 127.0.0.1 -d civil -f src/main/resources/nuke.sql
-- ALTER TABLE comments
-- ALTER COLUMN sentiment TYPE text;


WITH RECURSIVE comments_tree as (
 select 
  c1.*, 
  0 as comment_level,
  c1.id::VARCHAR as c_id
 from comments c1
 where c1.id = '13aa6632-e465-4be6-b654-a8d55d087653'
 
 union all
 
 select 
  c2.*,
  comment_level+1,
  c_id::VARCHAR || ',' || c2.id::VARCHAR
  from comments c2
 join comments_tree ct on ct.id = c2.parent_id 

) select * from comments_tree;



ALTER TABLE comments
DROP COLUMN comment_level;

ALTER TABLE sub_topics
DROP CONSTRAINT sub_topics_title_key;


"34801c25-5a23-41fd-93ac-40286dd25e56"

"d76a2cce-d57f-48d2-bf9b-3e0b919fadd9"

INSERT INTO comment_civility AS t (user_id,comment_id,is_civil) VALUES ('34801c25-5a23-41fd-93ac-40286dd25e56', 'd76a2cce-d57f-48d2-bf9b-3e0b919fadd9', true) ON CONFLICT DO NOTHING;

INSERT INTO comment_civility AS t (user_id,comment_id,is_civil) VALUES ('18b0b00e-3811-471a-9ed3-96a6559eba12', '0a65b951-3e06-4a22-8b65-5a57afab4a93', false) ON CONFLICT DO NOTHING;


INSERT INTO comment_civility AS t (user_id,comment_id,is_civil) VALUES ('34801c25-5a23-41fd-93ac-40286dd25e56', 'd76a2cce-d57f-48d2-bf9b-3e0b919fadd9', true) ON CONFLICT (comment_id,user_id) DO UPDATE SET is_civil = EXCLUDED.is_civil