CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS tsm_system_rows;
create extension file_fdw;

CREATE TYPE like_action AS ENUM ('LikedState', 'DislikedState', 'NeutralState');

CREATE TYPE sentiment AS ENUM ('POSITIVE', 'NEUTRAL', 'NEGATIVE', 'MEME');


CREATE TABLE "users" (
  id SERIAL PRIMARY KEY,
  user_id varchar(100) NOT NULL,
  username varchar(50) NOT NULL,
  tag varchar(50) UNIQUE,
  civility NUMERIC(20, 5) DEFAULT 13.60585,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  icon_src text,
  consortium_member boolean DEFAULT false,
  is_did_user boolean,
  bio text,
  experience text,
  preferences text[] DEFAULT '{}'::text[],
  UNIQUE(user_id),
  UNIQUE(username)
);

CREATE INDEX user_id_users_index ON "users" (user_id);
CREATE INDEX tag_users_index ON "users" (tag);



CREATE TABLE spaces(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title varchar(5000) NOT NULL,
    editor_state text NOT NULL,
    editor_text_content varchar(4820) NOT NULL,
    category varchar(50) default 'Technology',
    reference_links text[] DEFAULT '{}'::text[],
    likes integer DEFAULT 0,
    created_by_username text NOT NULL,
    created_by_user_id text NOT NULL,
    report_status text DEFAULT 'Clean',
    user_verification_type varchar(50) default 'NO_VERIFICATION',
    space_id uuid DEFAULT NULL,
    discussion_id uuid DEFAULT NULL,
    content_height decimal,
    user_uploaded_image_url varchar(300),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(title)
);

CREATE INDEX id_spaces_index ON spaces (id);
CREATE INDEX user_id_spaces_index ON spaces (created_by_user_id);

CREATE TABLE space_metadata(
    id SERIAL PRIMARY KEY,
    space_id uuid UNIQUE NOT NULL,
    space_categories text[] DEFAULT '{}'::text[],
    space_key_words text[] DEFAULT '{}'::text[],
    space_named_entities text[] DEFAULT '{}'::text[],
    text_content varchar(4820) NOT NULL,
    CONSTRAINT fk_space_metadata
      FOREIGN KEY(space_id)
        REFERENCES spaces(id)

);

CREATE TABLE space_similarity_scores(
    space_id1 uuid NOT NULL,
    space_id2 uuid NOT NULL,
    similarity_score float NOT NULL,
    PRIMARY KEY(space_id1, space_id2),
    FOREIGN KEY(space_id1) REFERENCES spaces(id),
    FOREIGN KEY(space_id2) REFERENCES spaces(id)
);

CREATE TABLE external_links(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    space_id uuid,
    link_type varchar(50) NOT NULL,
    external_content_url text NOT NULL,
    embed_id varchar(50),
    thumb_img_url varchar(2048),
    UNIQUE(space_id)
);

CREATE INDEX external_links_space_id_index ON external_links (space_id);



CREATE TABLE space_vods(
  id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
  user_id text NOT NULL,
  vod_url text NOT NULL,
  space_id uuid NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_spaces
    FOREIGN KEY( space_id)
      REFERENCES spaces(id)
);


CREATE TABLE discussions(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_username varchar(100) NOT NULL,
    created_by_user_id varchar(100) NOT NULL,
    title varchar(100) NOT NULL,
    editor_state text NOT NULL,
    editor_text_content varchar(420) NOT NULL,
    evidence_links text[] DEFAULT '{}',
    likes integer DEFAULT 0,
    user_uploaded_image_url text,
    user_uploaded_vod_url text,
    discussion_key_words text[] DEFAULT '{}'::text[],
    space_id uuid NOT NULL,
    discussion_id uuid DEFAULT NULL,
    content_height decimal,
    report_status text NOT NULL,
    UNIQUE(title, space_id),
    CONSTRAINT fk_spaces
      FOREIGN KEY(space_id)
	      REFERENCES spaces(id)
);


CREATE INDEX id_discussions_index ON discussions (id);
CREATE INDEX user_id_discussions_index ON discussions (created_by_user_id);


CREATE TABLE discussion_metadata(
    id SERIAL PRIMARY KEY,
    discussion_id uuid UNIQUE NOT NULL,
    discussion_categories text[] DEFAULT '{}'::text[],
    discussion_key_words text[] DEFAULT '{}'::text[],
    discussion_named_entities text[] DEFAULT '{}'::text[],
    text_content text NOT NULL,
    CONSTRAINT fk_discussion_metadata
      FOREIGN KEY(discussion_id)
        REFERENCES discussions(id)
);

CREATE TABLE discussion_similarity_scores(
    discussion_id1 uuid NOT NULL,
    discussion_id2 uuid NOT NULL,
    similarity_score float NOT NULL,
    PRIMARY KEY(discussion_id1, discussion_id2),
    FOREIGN KEY(discussion_id1) REFERENCES discussions(id),
    FOREIGN KEY(discussion_id2) REFERENCES discussions(id)
);




CREATE TABLE external_links_discussions(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    discussion_id uuid NOT NULL,
    link_type varchar(50) NOT NULL,
    external_content_url text NOT NULL,
    embed_id varchar(50),
    thumb_img_url varchar(2048),
    UNIQUE(discussion_id),
     CONSTRAINT discussions_spaces
        FOREIGN KEY(discussion_id)
          REFERENCES discussions(id)
);

CREATE INDEX external_links_discussions_discussion_id_index ON external_links_discussions (discussion_id);



CREATE TABLE comments(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    editor_state text NOT NULL,
    editor_text_content varchar(420) NOT NULL,
    created_by_username varchar(100) NOT NULL,
    created_by_user_id varchar(100) NOT NULL,
    discussion_id uuid,
    space_id uuid,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    source text,
    report_status text NOT NULL,
    toxicity_status varchar(50),
    CONSTRAINT fk_discussions
      FOREIGN KEY(discussion_id)
	      REFERENCES discussions(id)
);

CREATE INDEX id_comments_index ON comments (id);
CREATE INDEX user_id_comments_index ON comments (created_by_user_id);
CREATE INDEX parent_id_comments_index ON comments (parent_id);


CREATE TABLE tribunal_comments(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    editor_state text NOT NULL,
    editor_text_content varchar(420) NOT NULL,
    created_by_username varchar(100) NOT NULL,
    created_by_user_id varchar(100) NOT NULL,
    reported_content_id uuid,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    source text,
    comment_type varchar(50) NOT NULL,
    toxicity_status varchar(50)
);

CREATE INDEX id_tribunal_comments_index ON tribunal_comments (id);
CREATE INDEX created_by_user_id_tribunal_comments_index ON tribunal_comments (created_by_user_id);
CREATE INDEX parent_id_tribunal_comments_index ON tribunal_comments (parent_id);



CREATE TABLE comment_likes(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    like_state like_action DEFAULT 'NeutralState',
    UNIQUE(comment_id, user_id)
);

CREATE INDEX user_id_comment_likes_index ON comment_likes (user_id);
CREATE INDEX comment_id_comment_likes_index ON comment_likes (comment_id);

CREATE TABLE space_likes(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    space_id uuid NOT NULL,
    like_state like_action DEFAULT 'NeutralState',
    UNIQUE(space_id, user_id)
);

CREATE INDEX user_id_space_likes_index ON space_likes (user_id);
CREATE INDEX space_id_space_likes_index ON space_likes (space_id);


CREATE TABLE space_follows(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    followed_space_id uuid NOT NULL
);


CREATE INDEX user_id_space_follows_index ON space_follows (user_id);
CREATE INDEX space_id_space_follows_index ON space_follows (followed_space_id);

CREATE TABLE discussion_likes(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    discussion_id uuid NOT NULL,
    like_state like_action DEFAULT 'NeutralState',
    UNIQUE(discussion_id, user_id)
);

CREATE INDEX user_id_discussion_likes_index ON discussion_likes (user_id);
CREATE INDEX discussion_id_discussion_likes_index ON discussion_likes (discussion_id);


CREATE TABLE discussion_follows(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    followed_discussion_id uuid NOT NULL
);

CREATE INDEX user_id_discussion_follows_index ON discussion_follows (user_id);
CREATE INDEX discussion_id_discussion_follows_index ON discussion_follows (followed_discussion_id);


CREATE TABLE comment_civility (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    value NUMERIC(4, 1) DEFAULT 0,
    UNIQUE(comment_id , user_id)
);

CREATE INDEX user_id_comment_civility_index ON comment_civility (user_id);
CREATE INDEX comment_id_comment_civility_index ON comment_civility (comment_id);


CREATE TABLE follows (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    followed_user_id text NOT NULL
);



CREATE INDEX user_id_follows_index ON follows (user_id);
CREATE INDEX follower_id_follows_index ON follows (followed_user_id);



CREATE TABLE reports (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    toxic boolean,
    spam boolean,
    personal_attack boolean,
    content_type varchar(10),
    UNIQUE(content_id, user_id)
);

CREATE TABLE reviewed_reports (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    toxic boolean,
    spam boolean,
    personal_attack boolean,
    content_type varchar(10),
    reviewed_at timestamp without time zone DEFAULT NOW(),
);

CREATE INDEX user_id_reports_index ON reports (user_id);
CREATE INDEX content_id_reports_index ON reports (content_id);

CREATE TABLE report_timings(
    id SERIAL PRIMARY KEY,
    content_id uuid NOT NULL,
    report_period_start TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    report_period_end bigint NOT NULL,
    review_ending_times bigint[],
    ongoing boolean default true,
    content_type varchar(10),
    UNIQUE(content_id)
);

CREATE INDEX content_id_report_timings_index ON report_timings (content_id);

CREATE TABLE tribunal_jury_members(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    content_type varchar(10),
    UNIQUE(content_id, user_id)
);

CREATE INDEX user_id_tribunal_jury_members_index ON tribunal_jury_members (user_id);
CREATE INDEX content_id_tribunal_jury_members_index ON tribunal_jury_members (content_id);



CREATE TABLE tribunal_votes(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    vote_to_strike boolean,
    vote_to_acquit boolean,
    check ( num_nonnulls(vote_to_strike, vote_to_acquit) = 1),
    UNIQUE(content_id, user_id)
);

CREATE INDEX user_id_tribunal_votes_index ON tribunal_votes (user_id);
CREATE INDEX content_id_tribunal_votes_index ON tribunal_votes (content_id);


CREATE TABLE recommendations(
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid NOT NULL,
    similarity_score decimal DEFAULT 0
);


CREATE TABLE opposing_recommendations (
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid,
    external_recommended_content text,
    similarity_score decimal DEFAULT 0,
    is_discussion boolean
);

ALTER TABLE opposing_recommendations
ADD CONSTRAINT chk_only_one_is_not_null CHECK (num_nonnulls(recommended_content_id, external_recommended_content) = 1);

CREATE INDEX target_content_id_opposing_recommendations_index ON opposing_recommendations (target_content_id);



CREATE TABLE polls(
    id SERIAL PRIMARY KEY,
    content_id uuid NOT NULL,
    question text NOT NULL,
    version int NOT NULL,
    UNIQUE(content_id)
);

CREATE INDEX content_id_polls_index ON polls (content_id);


CREATE TABLE poll_options(
    id SERIAL PRIMARY KEY,
    poll_id bigint NOT NULL,
    text text NOT NULL,
    uid text NOT NULL,
    CONSTRAINT fk_polls
          FOREIGN KEY(poll_id)
    	      REFERENCES polls(id)
);

CREATE INDEX poll_id_poll_options_index ON poll_options (poll_id);

CREATE TABLE poll_votes(
    id SERIAL PRIMARY KEY,
    poll_option_id uuid NOT NULL,
    user_id text NOT NULL,
    UNIQUE(poll_option_id, user_id)

--    CONSTRAINT fk_poll_options
--          FOREIGN KEY(poll_option_id)
--    	      REFERENCES poll_options(id)
);

CREATE INDEX poll_options_id_poll_votes_index ON poll_votes (poll_option_id);


CREATE TABLE for_you_spaces(
  id SERIAL PRIMARY KEY,
  user_id text NOT NULL,
  space_ids text[] DEFAULT '{}'::text[],
  UNIQUE(user_id)
);


CREATE TABLE names (
  id SERIAL PRIMARY KEY,
  name VARCHAR(250) NOT NULL
);

-- Replace the values in this array with your desired names
DO $$
DECLARE
  names_arr TEXT[] := array['AI will revolutionize the workforce', 'Social media can impact mental health', 'Renewable energy can reduce emissions', 'Plant-based diets are better for you and the planet', 'The intersection of race and gender matters', 'Technology can improve or hinder personal relationships', 'Genetic engineering is a double-edged sword', 'Mindfulness reduces stress and anxiety', 'Space exploration will change our future', 'Climate change affects global health', 'Community support is key for mental wellness', 'Sustainable agriculture can fight food insecurity', 'Music can shape and reflect society', 'Gratitude improves overall wellbeing', 'Air pollution causes respiratory problems', 'Sustainable fashion is the future', 'Poverty impacts mental health', 'Data privacy is a human right', 'Outdoor activities are good for mental health', 'Renewable energy will transform transportation', 'Nature time improves mental health', 'Art has cultural and social significance', 'Exercise is beneficial for mental health', 'Hip-hop is a cultural phenomenon', 'Community involvement is essential for sustainability', 'Noise pollution is detrimental to mental health', 'Sustainable transportation is necessary', 'Film shapes and reflects society', 'Autonomous vehicles raise ethical questions', 'Self-care promotes overall health', 'Ocean pollution harms marine life and coastal areas', 'Diversity and inclusivity create stronger communities', 'Memory and recall are important cognitive functions', 'Literature reflects and influences society', 'Social media can impact communication and relationships', 'Graffiti is an art form with a rich history', 'AI raises ethical concerns and opportunities', 'Mindfulness improves work performance', 'Deforestation destroys habitats and biodiversity', 'Renewable energy is the future of homes and buildings', 'Self-compassion improves mental wellbeing', 'Fashion has cultural and social significance', 'Community support is critical for disaster response', 'Circadian rhythm affects sleep and overall health', 'Privacy and surveillance impact human rights', 'Meditation promotes stress reduction and mental clarity', 'Deforestation harms indigenous communities', 'Renewable energy is crucial for developing countries', 'Photography shapes and reflects society', 'Community support is essential for economic sustainability'];
  i INT;
BEGIN
  FOR i IN 1..array_length(names_arr, 1)
  LOOP
    INSERT INTO names (name) VALUES (names_arr[i]);
  END LOOP;
END $$;


--  psql -U postgres -h 127.0.0.1 -d civil -f src/main/resources/init.sql

insert into users (id, user_id, username, tag, civility, experience, bio, icon_src, preferences) values (1, '0x0eb59d2f60738dc3d8c996d49dbf99ce70bb4a0f', 'dseamark0', 'lchadwen0', 402, 'Rhetoric', 'engage clicks-and-mortar niches', 'https://robohash.org/etsedofficiis.png?size=50x50&set=set1', ARRAY['science_&_technology', 'family', 'fitness_&_health']::text[]);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (2, '0xded3cde0e5a828f05422130d5e161521bd5b8e2f', 'bswatland1', 'kcaccavale1', 85, 'Swimming', 'envisioneer holistic architectures', 'https://robohash.org/hicducimuset.png?size=50x50&set=set1', ARRAY ['business_&_entrepreneurs', 'food_&_dining', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (3, '0xd92b48841482ba973014b63a50d378a6d0ccadde', 'ydumphreys2', 'cmower2', 680, 'NLRB', 'transform killer functionalities', 'https://robohash.org/quidemsedpraesentium.png?size=50x50&set=set1', ARRAY ['sports', 'diaries_&_daily_life', 'arts_&_culture']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (4, '0x09ff4cf2f620c7bb103823b3320b6cda2e913c9f', 'hjime3', 'eaird3', 586, 'Software Installation', 'unleash magnetic interfaces', 'https://robohash.org/repudiandaeconsequaturnatus.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'business_&_entrepreneurs', 'learning_&_educational']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (5, '0x03cc8573d756b181cdcf7b2e3a16214fedeff632', 'brive4', 'silyas4', 367, 'Information Security Awareness', 'leverage e-business web-readiness', 'https://robohash.org/natusundeveniam.png?size=50x50&set=set1', ARRAY ['learning_&_educational', 'food_&_dining', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (6, '0xa1a57937a5662388ae87431b6e9f3e07faf1ec8b', 'mlewsley5', 'iloosmore5', 448, 'GMLAN', 'incentivize real-time eyeballs', 'https://robohash.org/abliberosoluta.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'music', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (7, '0x4dba15aa5f609b2fc21301df74be5ac3dd5348f9', 'fkoomar6', 'lmacilurick6', 374, 'Ukulele', 'deliver bleeding-edge communities', 'https://robohash.org/temporasintrerum.png?size=50x50&set=set1', ARRAY ['music', 'other_hobbies', 'food_&_dining']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (8, '0xaaf09fc3d29f3117b2323b86f1ff99b7f32057d2', 'bmarzellano7', 'csydney7', 978, 'MLP', 'synergize dynamic web-readiness', 'https://robohash.org/maximevoluptasvelit.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'arts_&_culture', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (9, '0xc3d929c521da752cfe25a23355ae949ec3a79ce9', 'citzak8', 'xdoni8', 920, 'SSAE 16', 'orchestrate open-source markets', 'https://robohash.org/inventoresintratione.png?size=50x50&set=set1', ARRAY ['gaming', 'business_&_entrepreneurs', 'learning_&_educational']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (10, '0x54d236260e6d2c5212185347f27421132c957bc3', 'jbartaloni9', 'gbracci9', 825, 'Uranium', 'recontextualize intuitive networks', 'https://robohash.org/repudiandaeullammagnam.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'arts_&_culture', 'other_hobbies']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (11, '0x4f2bfca885edb0c6be8ffd28bc32bcf16ec3ef19', 'balexisa', 'mshergilla', 108, 'Aerospace Manufacturing', 'innovate virtual relationships', 'https://robohash.org/fugitveritatisitaque.png?size=50x50&set=set1', ARRAY ['business_&_entrepreneurs', 'learning_&_educational', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (12, '0x07d9654a276c6ea79170f2aed57a47755fdef711', 'cfilpob', 'tmogenotb', 933, 'XPDL', 'e-enable strategic systems', 'https://robohash.org/utomnisneque.png?size=50x50&set=set1', ARRAY ['family', 'relationships', 'fashion_&_style']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (13, '0x3f70f0f06d47a1ebe64ef8bd3ef153a1854bc241', 'eslackc', 'sbonsulc', 136, 'CPP', 'recontextualize real-time systems', 'https://robohash.org/velitlaborepraesentium.png?size=50x50&set=set1', ARRAY ['other_hobbies', 'film_tv_&_video', 'gaming']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (14, '0x835197989fc81efe3040675c7a44bc4c609cb3ea', 'jcossonsd', 'bmaykind', 461, 'Lymphoma', 'e-enable sexy e-tailers', 'https://robohash.org/reiciendiseosea.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'other_hobbies', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (15, '0x5997145003187247dee106f5bf8bec9d78aa8832', 'vboheae', 'eferschkee', 283, 'VB.NET', 'drive viral ROI', 'https://robohash.org/idsintnostrum.png?size=50x50&set=set1', ARRAY ['film_tv_&_video', 'science_&_technology', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (16, '0x4dec4bb6f846b2e68223ea9ec7e8b3546ec93f44', 'xsacasef', 'nattridef', 964, 'TUPE', 'exploit sexy experiences', 'https://robohash.org/reiciendisipsamvel.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'music', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (17, '0x7d3a14600a0055b0b85cb67c9467d44d1db3b839', 'dirnysg', 'gilyinykhg', 123, 'Motion Graphics', 'generate seamless paradigms', 'https://robohash.org/utmodiexcepturi.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'learning_&_educational', 'film_tv_&_video']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (18, '0x001bb925157315ea6b4e189812f814c9e6a8e8d7', 'jivaninh', 'fadamowitzh', 837, 'General Aviation', 'unleash viral e-commerce', 'https://robohash.org/doloresquisquamiusto.png?size=50x50&set=set1', ARRAY ['arts_&_culture', 'news_&_social_concern', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (19, '0x31808a93908b7e94546ea6373fa0f46151d20ea9', 'fgammiei', 'sduffreei', 821, 'WTX', 'seize granular infomediaries', 'https://robohash.org/voluptatemcupiditatein.png?size=50x50&set=set1', ARRAY ['sports', 'fitness_&_health', 'gaming']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (20, '0x76aad80d668f76c12be71952fc049be10715ee63', 'aknappittj', 'ccharterj', 707, 'Political Economy', 'transform strategic methodologies', 'https://robohash.org/officiareprehenderittotam.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'science_&_technology', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (21, '0xccd80824c3a2bf69a56ba0e52fd88dd9d19374cb', 'saceyk', 'ceasomk', 563, 'ODI', 'visualize clicks-and-mortar technologies', 'https://robohash.org/faciliseiuset.png?size=50x50&set=set1', ARRAY ['gaming', 'travel_&_adventure', 'fashion_&_style']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (22, '0x3c11920383df65eed55a0f8a2b476bb3e456599b', 'ozellnerl', 'slangthornel', 815, 'HRO', 'deploy cross-platform initiatives', 'https://robohash.org/voluptatemollitianecessitatibus.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'film_tv_&_video', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (23, '0x242c4111ffca6c15c5c3594354bf7d27b5e2fd56', 'sjakucewiczm', 'abrickdalem', 157, 'Interest Rate Swaps', 'synergize proactive mindshare', 'https://robohash.org/etquoaut.png?size=50x50&set=set1', ARRAY ['gaming', 'celebrity_&_pop_culture', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (24, '0x6f18c470d95b97d67d330c02e6380e01ad448c7e', 'sjestn', 'tsturmann', 200, 'Microcontrollers', 'target compelling e-markets', 'https://robohash.org/praesentiumexsint.png?size=50x50&set=set1', ARRAY ['family', 'music', 'film_tv_&_video']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (25, '0x415383e6602e02a9fbb52cb1cbadac2d31f2d280', 'kmegaineyo', 'qindero', 518, 'Published Author', 'engineer web-enabled content', 'https://robohash.org/molestiaesuntquod.png?size=50x50&set=set1', ARRAY ['learning_&_educational', 'travel_&_adventure', 'family']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (26, '0x77e2418d20939eec6334d5d8acc117212c0c4f41', 'hemesp', 'mdundinp', 205, 'NNTP', 'enable efficient solutions', 'https://robohash.org/minimaquisquamvel.png?size=50x50&set=set1', ARRAY ['sports', 'news_&_social_concern', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (27, '0xbb7cf69292872666765b0176989c02a576244322', 'tpittetq', 'econleyq', 117, 'Fashion Shows', 'whiteboard innovative schemas', 'https://robohash.org/rationedoloremquae.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'fashion_&_style', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (28, '0xb7bce84686a3ac671e5375eadfd43d5f713d2a3b', 'drapor', 'norumr', 848, 'HRIS Database Management', 'grow seamless e-business', 'https://robohash.org/maximeanimirepellat.png?size=50x50&set=set1', ARRAY ['celebrity_&_pop_culture', 'family', 'gaming']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (29, '0x8be17eb36c0433bfc427b056526ed8b79df38edc', 'bwestropes', 'vlammertzs', 630, 'Hmong', 'unleash cross-media solutions', 'https://robohash.org/nobisvoluptatembeatae.png?size=50x50&set=set1', ARRAY ['relationships', 'learning_&_educational', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (30, '0x0c02092b6f57d1a42cdc8bf6a8a89c8cad3e9ef9', 'omcmurrught', 'sbeneyt', 263, 'Hazard Analysis', 'embrace best-of-breed markets', 'https://robohash.org/porrodoloreet.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'news_&_social_concern', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (31, '0xcfa32cad371f6a0cf30f7f0a19c4094c064f2351', 'eoxherdu', 'glinseyu', 843, 'EEG', 'innovate user-centric users', 'https://robohash.org/nonvelmaiores.png?size=50x50&set=set1', ARRAY ['arts_&_culture', 'relationships', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (32, '0x4dc8eb54751387067b1f797edbb9c64f6d165353', 'bflellov', 'jstallybrassv', 286, 'IFR', 'drive sticky networks', 'https://robohash.org/etlaborumvel.png?size=50x50&set=set1', ARRAY ['gaming', 'arts_&_culture', 'other_hobbies']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (33, '0x7ecfc50aeb9adf33bfa5d5eb1e575b175ea502e8', 'cbiglyw', 'adrinkalew', 25, 'Spanish', 'innovate vertical architectures', 'https://robohash.org/accusantiumdeseruntexplicabo.png?size=50x50&set=set1', ARRAY ['other_hobbies', 'film_tv_&_video', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (34, '0x234b1c91301081619060102ebd50458c1e6fee51', 'blauchlanx', 'lmanclarkx', 794, 'Transportation Management', 'orchestrate sticky supply-chains', 'https://robohash.org/animiquiaodio.png?size=50x50&set=set1', ARRAY ['science_&_technology', 'fashion_&_style', 'news_&_social_concern']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (35, '0x659535dd699b38fb7c5d87ca7e4e75186357f736', 'awarlocky', 'gthextony', 223, 'Game Design', 'optimize global platforms', 'https://robohash.org/molestiaevoluptatemcupiditate.png?size=50x50&set=set1', ARRAY ['family', 'science_&_technology', 'gaming']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (36, '0x5a36fa369b5437826963555f9005fdbe754c2ab0', 'blehrmannz', 'rwoodardz', 392, 'Data Structures', 'deliver innovative platforms', 'https://robohash.org/voluptatumvelsunt.png?size=50x50&set=set1', ARRAY ['relationships', 'fashion_&_style', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (37, '0xf6a8133e20f73861047e86ac82466741761ec492', 'ucowin10', 'nnormavell10', 615, 'Lytec', 'strategize efficient convergence', 'https://robohash.org/omnisametharum.png?size=50x50&set=set1', ARRAY ['other_hobbies', 'youth_&_student_life', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (38, '0xc43bfa0b602b2c6f3d200c2652dd89bc8de208f1', 'hdevil11', 'grustman11', 262, 'Bash', 'target back-end e-business', 'https://robohash.org/aducimusrepellendus.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'news_&_social_concern', 'science_&_technology']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (39, '0xd972896d9bd9c7a0b083410e8de1e53df128559c', 'oinfante12', 'mculpen12', 588, 'SAP OM', 'empower proactive convergence', 'https://robohash.org/quosuntnon.png?size=50x50&set=set1', ARRAY ['food_&_dining', 'science_&_technology', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (40, '0xd88915569b04e914d6dd898ab581047496ceeb87', 'wcobby13', 'tmeharg13', 270, 'Sports Writing', 'architect granular eyeballs', 'https://robohash.org/aperiameumprovident.png?size=50x50&set=set1', ARRAY ['learning_&_educational', 'relationships', 'other_hobbies']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (41, '0x073af8edecfe26ab9ea6c79c7ecd56159489aed5', 'sconnachan14', 'cbattison14', 132, 'FDM', 'disintermediate dynamic markets', 'https://robohash.org/etutminima.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'science_&_technology', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (42, '0x843dfccecba6ad24435c3895272f30dec95c05d1', 'cstripp15', 'sfollett15', 584, 'KOL Identification', 'matrix granular web-readiness', 'https://robohash.org/adrerumfuga.png?size=50x50&set=set1', ARRAY ['other_hobbies', 'fashion_&_style', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (43, '0x686336d182507c3e009b5090ce98297468b7cf3e', 'ewagstaffe16', 'dbrettle16', 507, 'QSIG', 'aggregate value-added paradigms', 'https://robohash.org/undeetid.png?size=50x50&set=set1', ARRAY ['celebrity_&_pop_culture', 'music', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (44, '0x9ab3b069c2f9331b4d4d957f43256ba03b05b061', 'ballicock17', 'kcrimin17', 415, 'CVM', 'repurpose cutting-edge metrics', 'https://robohash.org/eaquedoloresut.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'youth_&_student_life', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (45, '0xb83be9d58964eb7de419bc209ff01e393e4087d6', 'tlumsdon18', 'bcottom18', 961, 'RFI', 'benchmark granular architectures', 'https://robohash.org/estnatuscupiditate.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'travel_&_adventure', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (46, '0x6259a96146ef1bba47d0ff13582f967c93036f70', 'kglenwright19', 'bberthome19', 316, 'Mycobacteriology', 'evolve value-added web services', 'https://robohash.org/nobisdolordolore.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'music', 'celebrity_&_pop_culture']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (47, '0xf59caab7b38b8cf117a05bcf770fe9cc6eb50dd6', 'jmilesap1a', 'rector1a', 854, 'Core Java', 'recontextualize leading-edge convergence', 'https://robohash.org/velitautet.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'science_&_technology', 'family']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (48, '0x00167e8909ba1512eac0d22758dd1ed043cb122e', 'cblunn1b', 'ceaston1b', 260, 'HACCP', 'cultivate plug-and-play infrastructures', 'https://robohash.org/autsittempora.png?size=50x50&set=set1', ARRAY ['arts_&_culture', 'relationships', 'fashion_&_style']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (49, '0xda910f21f256ddb81acce309e40c163afff8686c', 'twannell1c', 'drosensaft1c', 67, 'Sony Vegas', 'grow open-source e-services', 'https://robohash.org/facerevoluptasautem.png?size=50x50&set=set1', ARRAY ['sports', 'gaming', 'celebrity_&_pop_culture']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (50, '0xe7514ee5f6e21472f7195975f6e2261ef4134b60', 'cgilchrist1d', 'ngoodley1d', 296, 'Apache ZooKeeper', 'scale clicks-and-mortar functionalities', 'https://robohash.org/velitsitfugiat.png?size=50x50&set=set1', ARRAY ['film_tv_&_video', 'fashion_&_style', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (51, '0xd3468b5149784cab749d21bfeb674b8e5f717c51', 'jtuther1e', 'doguz1e', 403, 'ETL Tools', 'disintermediate seamless convergence', 'https://robohash.org/aperiamutvoluptatibus.png?size=50x50&set=set1', ARRAY ['gaming', 'travel_&_adventure', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (52, '0x233012bd2cb8328fc16f98821d8e678c245498e3', 'mdornin1f', 'nmarvel1f', 422, 'Athlete Development', 'deploy mission-critical relationships', 'https://robohash.org/quienimquisquam.png?size=50x50&set=set1', ARRAY ['relationships', 'food_&_dining', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (53, '0xee30546faf66f0c2b1006a84c1a35a1fb836b256', 'rrosenfelder1g', 'mjuppe1g', 185, 'MTOs', 'deploy 24/7 supply-chains', 'https://robohash.org/maximebeataedeserunt.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'sports', 'fashion_&_style']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (54, '0xf0849bffe8dd0e0e1282ffc0faa016c23261ff57', 'lverdon1h', 'moggers1h', 843, 'Global Marketing', 'enable seamless solutions', 'https://robohash.org/assumendaetodit.png?size=50x50&set=set1', ARRAY ['fashion_&_style', 'celebrity_&_pop_culture', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (55, '0x03f809ce7220fbbdc7e62e49b3309c0851c7142b', 'chebbes1i', 'lgabbett1i', 838, 'EBPP', 'whiteboard back-end paradigms', 'https://robohash.org/atquesapientesit.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'arts_&_culture', 'news_&_social_concern']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (56, '0x7e9f33aa5a2aa4990eede0065d04078fac7b1c72', 'ctheseira1j', 'tgeelan1j', 22, 'Guided Imagery', 'reinvent integrated systems', 'https://robohash.org/omnisautquis.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'sports', 'food_&_dining']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (57, '0xd74ea8a06f4b889139d8929f85a0c8c10aabce9a', 'asummerell1k', 'tdawks1k', 608, 'BDC', 'deploy collaborative users', 'https://robohash.org/eiusatexplicabo.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'science_&_technology', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (58, '0xd53d3c7505b03e99754043745c7d073aa599301c', 'jstafford1l', 'cmyers1l', 38, 'PCP', 'matrix intuitive solutions', 'https://robohash.org/veldoloresaut.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'other_hobbies', 'family']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (59, '0x1bbb121d238f6d82543dc74bf4ec9b1eec121f79', 'iedden1m', 'lpiddick1m', 495, 'DVB-S2', 'enable plug-and-play portals', 'https://robohash.org/adquoest.png?size=50x50&set=set1', ARRAY ['gaming', 'diaries_&_daily_life', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (60, '0x05a9357115b7ad4586b7fe31988253e1c6797ba0', 'dbolesma1n', 'sshuxsmith1n', 69, 'IDL', 'drive magnetic channels', 'https://robohash.org/sedplaceatsapiente.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'gaming', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (61, '0x030d35fc932866b218cb8c6e07c67d19c6ab15f2', 'egirodin1o', 'nmildenhall1o', 48, 'Allen-Bradley', 'facilitate magnetic web services', 'https://robohash.org/temporibussuntnumquam.png?size=50x50&set=set1', ARRAY ['fitness_&_health', 'other_hobbies', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (62, '0xa735e175b43a95c2a06367f87c95cc40373886ed', 'nkliemke1p', 'aedghinn1p', 561, 'Fraud', 'seize visionary models', 'https://robohash.org/nonconsequunturquibusdam.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'other_hobbies', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (63, '0xa214ac4111191d686605cff6e2df8355d5bc711f', 'mdrance1q', 'qburdas1q', 459, 'Enterprise Architecture', 'scale next-generation e-business', 'https://robohash.org/vitaesimiliquequidem.png?size=50x50&set=set1', ARRAY ['other_hobbies', 'youth_&_student_life', 'learning_&_educational']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (64, '0x84e2b56426a47acfc32fd7c71cbee3a141bd5d83', 'bdella1r', 'jflahive1r', 384, 'WFS', 'deliver plug-and-play solutions', 'https://robohash.org/teneturrepudiandaevel.png?size=50x50&set=set1', ARRAY ['business_&_entrepreneurs', 'celebrity_&_pop_culture', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (65, '0x13fa7e0f1f1155de93e906b285201f4c1b474d8c', 'gguerrazzi1s', 'cshovel1s', 625, 'Hotel Booking', 'deliver next-generation convergence', 'https://robohash.org/consequaturfacerequasi.png?size=50x50&set=set1', ARRAY ['relationships', 'music', 'news_&_social_concern']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (66, '0xe8ebb600b046467785eb16de3ba914b434714172', 'aenderby1t', 'srooke1t', 53, 'PostgreSQL', 'syndicate integrated platforms', 'https://robohash.org/modiametet.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'news_&_social_concern', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (67, '0x384f180db8c9101ad1a2ebf0c17f1a96db1a552d', 'ftrubshaw1u', 'espaxman1u', 850, 'FFE', 'optimize sexy deliverables', 'https://robohash.org/impeditdoloribusconsequatur.png?size=50x50&set=set1', ARRAY ['music', 'food_&_dining', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (68, '0x90edc05709d5b065fc94b43e6faf8154aa9d8b0c', 'gmcmillam1v', 'lstreater1v', 92, 'QRC', 'optimize 24/365 partnerships', 'https://robohash.org/idutquidem.png?size=50x50&set=set1', ARRAY ['gaming', 'travel_&_adventure', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (69, '0x7690f7b70082644e1a380d7fb89825059570a69e', 'lmotherwell1w', 'cbuntain1w', 639, 'Professional Services', 'syndicate sexy applications', 'https://robohash.org/quisvoluptatemlaudantium.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'business_&_entrepreneurs', 'news_&_social_concern']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (70, '0xa1a8c7b1123012d039ddcd2b140f41c00977e3f6', 'mglasner1x', 'ccalderonello1x', 389, 'TSW', 'grow integrated e-services', 'https://robohash.org/cumqueetfacilis.png?size=50x50&set=set1', ARRAY ['relationships', 'fashion_&_style', 'news_&_social_concern']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (71, '0xa1f30e9f7d4a6dc69a9317d1e784d0eac259b9a3', 'lmccurrie1y', 'hwebber1y', 626, 'SBS', 'transform one-to-one e-services', 'https://robohash.org/minimaautemtemporibus.png?size=50x50&set=set1', ARRAY ['business_&_entrepreneurs', 'film_tv_&_video', 'learning_&_educational']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (72, '0x47a91073138e725271ce437eb431ebd14c313263', 'bsawyers1z', 'mbroadhurst1z', 763, 'Molecular Cloning', 'streamline magnetic content', 'https://robohash.org/voluptatemperspiciatisautem.png?size=50x50&set=set1', ARRAY ['family', 'relationships', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (73, '0x3c2dea9fd6527f031f9514474dfbe18eb0a84d7e', 'bhuthart20', 'lmorley20', 363, 'Amazon VPC', 'drive front-end initiatives', 'https://robohash.org/sitsitrem.png?size=50x50&set=set1', ARRAY ['sports', 'gaming', 'food_&_dining']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (74, '0x4a43374a83fd6b13c40a6cf17a1a951b09df4007', 'gjanatka21', 'tbrunroth21', 975, 'Citrix XenApp', 'productize extensible technologies', 'https://robohash.org/quiaiustoet.png?size=50x50&set=set1', ARRAY ['music', 'science_&_technology', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (75, '0x27d6df6afd0ee547428ec32abd06aabb412b80f8', 'dglynne22', 'mmasurel22', 493, 'TCM', 'recontextualize B2B niches', 'https://robohash.org/estdignissimosinventore.png?size=50x50&set=set1', ARRAY ['celebrity_&_pop_culture', 'food_&_dining', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (76, '0x050fb7a41dbe0ddec136bc3e13f6a3c9c8b65b3f', 'ecavolini23', 'mmagor23', 701, 'DHCP', 'target plug-and-play vortals', 'https://robohash.org/numquamvoluptatevelit.png?size=50x50&set=set1', ARRAY ['fashion_&_style', 'news_&_social_concern', 'business_&_entrepreneurs']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (77, '0x4a6bcc2c65315bbf005b0792f7b87440e75069c3', 'boehm24', 'aphilippet24', 513, 'Lutron', 'matrix synergistic infomediaries', 'https://robohash.org/utmaximererum.png?size=50x50&set=set1', ARRAY ['celebrity_&_pop_culture', 'fashion_&_style', 'family']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (78, '0xd7b4a10737bf95dbdb4cdf597c4d6128ee7c0dba', 'fcharlesworth25', 'sjuszczak25', 459, 'VTC', 'reintermediate one-to-one action-items', 'https://robohash.org/maximedolorespraesentium.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'news_&_social_concern', 'gaming']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (79, '0x1a072e8bc66dd8cb61cc56d7709ddb2750ae1c1d', 'rwellstead26', 'hedinboro26', 847, 'SRT', 'exploit global functionalities', 'https://robohash.org/fugiatquiminus.png?size=50x50&set=set1', ARRAY ['family', 'science_&_technology', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (80, '0x001891e51f3d5a2dab2db7c86710b04fb806f623', 'mmassie27', 'bdeclercq27', 974, 'OLED', 'drive one-to-one initiatives', 'https://robohash.org/exercitationemealaudantium.png?size=50x50&set=set1', ARRAY ['science_&_technology', 'music', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (81, '0xc66d6a91363bcdc68edd587990903b3e31268595', 'bvaudin28', 'tconkie28', 238, 'Japanese to English', 'embrace frictionless models', 'https://robohash.org/nostrumexdoloribus.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'diaries_&_daily_life', 'celebrity_&_pop_culture']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (82, '0x0102bd6f43723a1e62cb2d3a53233f4fc8cd19a9', 'dsmallsman29', 'urawood29', 777, 'Warranty', 'enable front-end schemas', 'https://robohash.org/occaecatiquisquamconsectetur.png?size=50x50&set=set1', ARRAY ['learning_&_educational', 'diaries_&_daily_life', 'science_&_technology']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (83, '0x4cbe49aadf1b7aef919d9a4f2619bb4992dc8fde', 'dcullagh2a', 'spoynser2a', 919, 'Abstracting', 'matrix dynamic portals', 'https://robohash.org/totamenimtempora.png?size=50x50&set=set1', ARRAY ['family', 'celebrity_&_pop_culture', 'fashion_&_style']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (84, '0x8b6b3809685b5ffb0b12ca4714c7fece9ae1ecdc', 'jlillistone2b', 'pkingdon2b', 819, 'Immunology', 'redefine next-generation markets', 'https://robohash.org/laboriosamvoluptaset.png?size=50x50&set=set1', ARRAY ['film_tv_&_video', 'family', 'arts_&_culture']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (85, '0x92619039c4b419f720a9eecac3cdc76818df03f7', 'krablen2c', 'cgoodsir2c', 107, 'Aesthetic Surgery', 'repurpose rich methodologies', 'https://robohash.org/commodiarchitectoeum.png?size=50x50&set=set1', ARRAY ['science_&_technology', 'travel_&_adventure', 'youth_&_student_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (86, '0xf4dd92513fddc20ba8cd7c2434a584ca3f92b044', 'rfrangleton2d', 'ssatchell2d', 988, 'CgFX', 'seize dynamic schemas', 'https://robohash.org/illoblanditiisquia.png?size=50x50&set=set1', ARRAY ['gaming', 'celebrity_&_pop_culture', 'film_tv_&_video']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (87, '0x885319b58ab68d08767a9f2313b56d6d020321a5', 'rtaphouse2e', 'mgoodlet2e', 51, 'VTC', 'expedite killer portals', 'https://robohash.org/molestiasestut.png?size=50x50&set=set1', ARRAY ['family', 'sports', 'other_hobbies']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (88, '0x28ca366d3c7e866db45812514b9cac19531091eb', 'thasell2f', 'bpynn2f', 145, 'Dubbing', 'embrace mission-critical paradigms', 'https://robohash.org/estdoloresquis.png?size=50x50&set=set1', ARRAY ['science_&_technology', 'film_tv_&_video', 'music']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (89, '0xae5f9f4a8c289832a27d105ca510394a29d4bfde', 'hgorling2g', 'bandrelli2g', 185, 'DCL', 'deploy customized channels', 'https://robohash.org/explicabopraesentiumnon.png?size=50x50&set=set1', ARRAY ['family', 'travel_&_adventure', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (90, '0xe13653d863cd9affeba128cff67d91f1d727f88d', 'rmcareavey2h', 'gkibbe2h', 857, 'MRTG', 'unleash user-centric relationships', 'https://robohash.org/atetvoluptatem.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'learning_&_educational', 'film_tv_&_video']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (91, '0xbab13816ffaf25de719b29eb78f9f8097c83ec41', 'jsims2i', 'efarrens2i', 900, 'MIS', 'transition web-enabled infomediaries', 'https://robohash.org/repellatestquo.png?size=50x50&set=set1', ARRAY ['youth_&_student_life', 'gaming', 'science_&_technology']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (92, '0xc4ecada40a8fbb0abefde8ae898992bed79ce6e4', 'gspark2j', 'afaulkener2j', 586, 'cXML', 'orchestrate user-centric niches', 'https://robohash.org/fugiatquitempore.png?size=50x50&set=set1', ARRAY ['diaries_&_daily_life', 'gaming', 'sports']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (93, '0xfa55f0e2bae167283469580ca17eba79c040fb5c', 'obyforth2k', 'lclitherow2k', 108, 'Document Drafting', 'benchmark 24/365 relationships', 'https://robohash.org/pariaturautemqui.png?size=50x50&set=set1', ARRAY ['travel_&_adventure', 'family', 'diaries_&_daily_life']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (94, '0xe83780ba56587881ddb0feb74b0bb2710571e190', 'kborrett2l', 'amatthensen2l', 357, 'XSL', 'unleash revolutionary deliverables', 'https://robohash.org/veritatisatquos.png?size=50x50&set=set1', ARRAY ['fitness_&_health', 'family', 'other_hobbies']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (95, '0x8c2946b98b5d2efd95c5ed1e359fd10ff1508ad7', 'lfitzroy2m', 'mjeanin2m', 375, 'AHWD', 'brand user-centric markets', 'https://robohash.org/perferendisfugitdoloribus.png?size=50x50&set=set1', ARRAY ['film_tv_&_video', 'family', 'science_&_technology']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (96, '0x64b6c7797174276d65425a48b7d19f299d629292', 'lgianni2n', 'cbrownsmith2n', 751, 'Economic Research', 'streamline turn-key e-tailers', 'https://robohash.org/etsedipsum.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'food_&_dining', 'relationships']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (97, '0x1af1fb0e4f06fee661845f84caa62731e6261291', 'bcurwen2o', 'gawin2o', 232, 'osCommerce', 'revolutionize granular solutions', 'https://robohash.org/nemoautvoluptatem.png?size=50x50&set=set1', ARRAY ['news_&_social_concern', 'film_tv_&_video', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (98, '0xdd1182eb0b81bac3dd20e1660c13d2d5d084b1d2', 'mcosin2p', 'mbraham2p', 38, 'Hospitality Management', 'maximize frictionless vortals', 'https://robohash.org/aliasdelectusassumenda.png?size=50x50&set=set1', ARRAY ['business_&_entrepreneurs', 'relationships', 'travel_&_adventure']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (99, '0xbb042e85688479d8d502697fdb12d526c404d97b', 'ibalchin2q', 'zcicchelli2q', 99, 'Lung', 'brand interactive e-services', 'https://robohash.org/nequenostrumplaceat.png?size=50x50&set=set1', ARRAY ['film_tv_&_video', 'youth_&_student_life', 'fitness_&_health']);
insert into users (id, user_id, username, tag, civility, experience, bio, icon_src,  preferences) values (100, '0x17dacc26ff8dc7f81e19c94d2c7e25afd049224d', 'dmcdougald2r', 'dmaciaszek2r', 14, 'TLM', 'exploit turn-key systems', 'https://robohash.org/eaquenequesunt.png?size=50x50&set=set1', ARRAY ['music', 'other_hobbies', 'learning_&_educational']);




--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ecff323f-13f3-4f4f-a297-928ae1134e49', 'concept2193', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youarchive', 'http://dummyimage.com/172x100.png/ff4444/ffffff', null, null, 8934, 'Filbert', 'Hill', null, '2022-03-31 06:26:34', '2022-09-29 04:00:25');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('90b1e8a6-40a5-47d0-8dab-53dd3876cfc0', 'Pre-emptive1469', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youeco-centric', 'http://dummyimage.com/207x100.png/cc0000/ffffff', null, null, 8692, 'Cam', 'Ross', null, '2022-04-21 10:15:38', '2022-05-17 06:33:03');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ae7f699e-7172-4503-87f6-3fde087dcdf6', 'Integrated1695', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youhardware', 'http://dummyimage.com/196x100.png/dddddd/000000', null, null, 3022, 'Annelise', 'Killie', null, '2022-03-13 00:06:36', '2022-06-20 10:14:37');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d4196645-ea61-47e1-b461-66169e6c903d', 'methodical3003', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youcontingency', 'http://dummyimage.com/103x100.png/ff4444/ffffff', null, null, 7142, 'Marcus', 'Hermann', null, '2022-07-03 15:32:39', '2022-08-01 23:42:11');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('b9ae9789-a689-4755-8655-2620bd7f19c7', 'extranet2815', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youhelp-desk', 'http://dummyimage.com/237x100.png/ff4444/ffffff', null, null, 1395, 'Lorita', 'Shelton', null, '2022-05-02 09:27:36', '2022-09-13 07:34:01');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('3d9d302f-3b0d-42be-98a0-b16b4d71f23b', 'model4427', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youNetworked', 'http://dummyimage.com/148x100.png/ff4444/ffffff', null, null, 6213, 'Art', 'Henry', null, '2022-06-06 01:33:40', '2022-08-17 09:29:47');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('c3cd9cc3-f79e-45e3-aeef-d6ac2f4e1f89', 'Seamless4799', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youCompatible', 'http://dummyimage.com/242x100.png/dddddd/000000', null, null, 2527, 'Meggy', 'Chet', null, '2022-04-17 17:38:33', '2022-07-17 16:19:41');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('45413a2f-70b9-4564-ae3b-39b77103e8d6', 'Robust3739', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youopen system', 'http://dummyimage.com/224x100.png/5fa2dd/ffffff', null, null, 8269, 'Audy', 'Justis', null, '2022-09-16 15:09:55', '2022-04-16 21:32:26');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('5b07acc5-6aa6-4eac-97fe-1e7c5ab4cb49', 'Grass-roots2111', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youstructure', 'http://dummyimage.com/158x100.png/dddddd/000000', null, null, 9254, 'Zebulon', 'Andrea', null, '2022-08-24 16:53:24', '2022-04-22 22:42:21');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('10f73d4f-0b6d-4898-972f-32bb5b045b43', 'intranet3575', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youintranet', 'http://dummyimage.com/238x100.png/ff4444/ffffff', null, null, 735, 'Twyla', 'Leif', null, '2022-10-11 00:38:39', '2022-03-08 08:45:20');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('18dd0b19-13a4-43d6-badc-0346090d290e', 'database2557', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youimpactful', 'http://dummyimage.com/115x100.png/ff4444/ffffff', null, null, 5368, 'Waylin', 'Hewitt', null, '2022-07-06 02:35:38', '2022-08-28 22:36:39');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('3b4dfd12-1e9e-4b3f-b2a7-3a06a92ac1bd', 'structure3746', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youinstallation', 'http://dummyimage.com/125x100.png/ff4444/ffffff', null, null, 551, 'Rosmunda', 'Pavlov', null, '2022-06-24 03:23:41', '2022-08-13 15:10:41');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('23e436b6-577c-4148-961d-669a7c99c410', 'adapter885', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youanalyzing', 'http://dummyimage.com/124x100.png/5fa2dd/ffffff', null, null, 6713, 'Corby', 'Ernestus', null, '2022-05-23 13:47:51', '2022-05-16 12:29:52');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('e12683b5-ed3b-4bff-a56d-ea2a39423c0b', 'heuristic5023', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youmoderator', 'http://dummyimage.com/244x100.png/cc0000/ffffff', null, null, 216, 'Moll', 'Salomon', null, '2022-06-23 01:16:33', '2022-10-03 22:44:59');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('c6e77f14-6575-439e-894e-8d4f8e151914', 'interface2460', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youservice-desk', 'http://dummyimage.com/216x100.png/cc0000/ffffff', null, null, 8718, 'Ezra', 'Broddie', null, '2022-09-02 17:42:58', '2022-03-10 15:36:12');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('abb9cbaa-6e0a-454f-b8ab-e556a06b9a5b', 'Advanced1009', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youcustomer loyalty', 'http://dummyimage.com/135x100.png/cc0000/ffffff', null, null, 7076, 'Skipper', 'Oswell', null, '2022-03-20 01:08:39', '2022-09-30 06:42:10');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('435724ae-13fa-4474-9f73-c50337b770da', 'Up-sized2557', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youoptimal', 'http://dummyimage.com/143x100.png/dddddd/000000', null, null, 1811, 'Jannel', 'Jean', null, '2022-07-01 16:30:07', '2022-04-14 14:20:31');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('7e61fd32-5b2e-46c7-a8b6-b9741f47dde0', 'Switchable4214', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youOpen-architected', 'http://dummyimage.com/203x100.png/dddddd/000000', null, null, 2163, 'Paola', 'Toby', null, '2022-10-17 19:52:33', '2022-07-19 16:37:41');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('813d9545-e3bd-47f9-9143-6b84133de4ce', 'attitude3865', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youuniform', 'http://dummyimage.com/209x100.png/ff4444/ffffff', null, null, 650, 'Harmony', 'Sergeant', null, '2022-11-06 06:55:30', '2022-06-01 06:55:14');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ebbca71d-f025-4682-9c2d-6855d69d69ec', 'parallelism3758', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youintranet', 'http://dummyimage.com/154x100.png/ff4444/ffffff', null, null, 1647, 'Georgeanne', 'Werner', null, '2022-07-27 17:15:04', '2022-09-29 09:03:36');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('9022c316-841c-4f22-8533-210fe4b8b1d5', 'frame3994', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youparallelism', 'http://dummyimage.com/101x100.png/dddddd/000000', null, null, 2221, 'Daniele', 'Dannel', null, '2022-06-24 22:23:08', '2022-04-05 16:42:30');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('efc5d807-536a-4ee7-a085-d3f526dd40c8', 'focus group309', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youutilisation', 'http://dummyimage.com/172x100.png/ff4444/ffffff', null, null, 3632, 'Robinet', 'Gilburt', null, '2022-10-19 19:06:11', '2022-05-30 07:01:32');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('0926b052-ecdb-4e6c-a7ed-461c8e5b9dbf', 'directional3872', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youfunctionalities', 'http://dummyimage.com/247x100.png/5fa2dd/ffffff', null, null, 6205, 'Mickie', 'Burton', null, '2022-08-02 06:12:14', '2022-04-08 10:32:52');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('e26b3cf9-ae6d-4b97-bfe6-15d141f5882e', 'Customer-focused1497', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youapproach', 'http://dummyimage.com/159x100.png/dddddd/000000', null, null, 9014, 'Ted', 'Sidnee', null, '2022-07-23 16:35:30', '2022-05-15 08:45:50');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('843c7657-5649-49c9-889b-95eb30dd76fe', 'user-facing1784', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youleverage', 'http://dummyimage.com/197x100.png/5fa2dd/ffffff', null, null, 3823, 'Thurstan', 'Darill', null, '2022-05-15 16:47:11', '2022-03-13 06:06:23');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('7a7d5473-55db-4628-8b1d-0ff431cd8034', 'Open-source610', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youExtended', 'http://dummyimage.com/217x100.png/ff4444/ffffff', null, null, 3666, 'Bernardo', 'Piotr', null, '2022-09-20 23:50:03', '2022-04-21 01:08:18');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('2f31a95a-6a60-408c-ac07-94560575f0b8', 'leading edge4868', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousupport', 'http://dummyimage.com/142x100.png/5fa2dd/ffffff', null, null, 1160, 'Michal', 'Noam', null, '2022-06-11 21:21:55', '2022-06-07 14:22:26');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('6ce248ad-0f13-4ce4-8b78-1910404bbd4e', 'high-level4301', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youmethodical', 'http://dummyimage.com/222x100.png/ff4444/ffffff', null, null, 9870, 'Wallache', 'Tulley', null, '2022-06-16 12:46:57', '2022-04-11 14:04:44');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('5ff33908-942a-4ed8-8e15-e4ce44e5cede', 'Exclusive1640', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youextranet', 'http://dummyimage.com/200x100.png/cc0000/ffffff', null, null, 1025, 'Alana', 'Lincoln', null, '2022-05-29 22:09:12', '2022-07-03 05:09:16');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('b3d10b72-edcf-4f76-b450-42cacab9ed15', 'Graphic Interface4525', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youdemand-driven', 'http://dummyimage.com/175x100.png/cc0000/ffffff', null, null, 8014, 'Rozelle', 'Matias', null, '2022-05-26 01:27:50', '2022-07-08 16:36:16');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('e579220b-85fb-48b3-a391-e7de3150b6de', 'Automated4234', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youproductivity', 'http://dummyimage.com/208x100.png/cc0000/ffffff', null, null, 1776, 'Alfy', 'Otho', null, '2022-03-31 05:57:30', '2022-04-01 15:43:41');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('47931a5d-0365-466d-acff-435aa8b8296e', 'Customer-focused671', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youRe-engineered', 'http://dummyimage.com/146x100.png/5fa2dd/ffffff', null, null, 571, 'Laughton', 'Kipp', null, '2022-03-31 03:25:44', '2022-06-24 19:22:23');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('f880ebdd-64d5-40ec-85d6-1267509438ca', 'Front-line4966', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youGraphical User Interface', 'http://dummyimage.com/153x100.png/cc0000/ffffff', null, null, 9271, 'Meara', 'Tailor', null, '2022-10-10 22:33:57', '2022-07-16 19:15:55');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('1295e0ef-5daa-4ac1-9109-32322c3e4b61', 'Customizable1542', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youmigration', 'http://dummyimage.com/186x100.png/5fa2dd/ffffff', null, null, 4842, 'Alessandro', 'Ramon', null, '2022-03-25 23:57:56', '2022-10-10 17:56:07');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('fee6ec6f-a889-41d3-82ec-624b9ab86786', 'Decentralized3396', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youreal-time', 'http://dummyimage.com/150x100.png/5fa2dd/ffffff', null, null, 2833, 'Esra', 'Franky', null, '2022-04-30 07:54:32', '2022-05-08 13:23:57');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('4aee35e9-93a7-41f7-b5c9-205c7752c992', 'middleware4347', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youclient-driven', 'http://dummyimage.com/181x100.png/dddddd/000000', null, null, 8022, 'Dannye', 'Hugh', null, '2022-06-23 13:20:43', '2022-06-30 11:22:26');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('4e84cdfe-29b5-47d8-ae5b-33b0ddb03d17', 'Monitored4', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youOrganic', 'http://dummyimage.com/140x100.png/ff4444/ffffff', null, null, 6927, 'Herbie', 'Huey', null, '2022-03-20 18:36:54', '2022-08-27 12:24:25');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('553e705c-94b9-4307-bfa4-6d8e717c4c09', 'pricing structure1148', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youencryption', 'http://dummyimage.com/137x100.png/ff4444/ffffff', null, null, 6248, 'Xaviera', 'Carroll', null, '2022-06-27 00:13:23', '2022-08-11 14:09:33');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('538e7f41-96e6-4c9f-be0b-1907ef141832', 'Ameliorated2842', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youDevolved', 'http://dummyimage.com/184x100.png/5fa2dd/ffffff', null, null, 3316, 'Sammy', 'Antonino', null, '2022-08-24 03:39:00', '2022-06-23 15:45:53');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('06663c08-02d8-411e-a441-7aefb4a4dc1e', 'Virtual1599', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youVisionary', 'http://dummyimage.com/112x100.png/ff4444/ffffff', null, null, 7936, 'Hasheem', 'Dmitri', null, '2022-03-31 07:44:12', '2022-03-28 00:02:24');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('46ce73c1-131e-4245-b7a5-f43cccabe92b', 'value-added4234', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youheuristic', 'http://dummyimage.com/114x100.png/dddddd/000000', null, null, 1940, 'Jasmin', 'Baron', null, '2022-03-09 10:31:00', '2022-08-17 13:01:06');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('b8062760-12d8-459a-8086-9fd778fe2229', 'data-warehouse2295', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youapproach', 'http://dummyimage.com/175x100.png/ff4444/ffffff', null, null, 4852, 'Cletus', 'Uriel', null, '2022-04-06 16:27:17', '2022-08-20 01:22:20');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ce4ee129-6e00-4b31-955b-8d1b32150c94', 'portal2306', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youFundamental', 'http://dummyimage.com/165x100.png/cc0000/ffffff', null, null, 8594, 'Brunhilda', 'Torey', null, '2022-09-01 00:11:28', '2022-09-20 05:44:46');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('79e8e9c5-032a-4782-ad78-502a7bf3424d', 'synergy2967', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youManaged', 'http://dummyimage.com/152x100.png/ff4444/ffffff', null, null, 8553, 'Kathlin', 'Clevie', null, '2022-09-27 06:03:48', '2022-08-30 17:47:40');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('fa40a528-0c2f-4877-966b-f0534a515c3b', 'tangible5024', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youneural-net', 'http://dummyimage.com/118x100.png/cc0000/ffffff', null, null, 266, 'Nikki', 'Bernardo', null, '2022-10-23 02:02:06', '2022-05-03 14:57:02');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('98e0a9d9-a695-4d7e-8401-ffb09d2ed821', 'uniform1234', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love you3rd generation', 'http://dummyimage.com/166x100.png/cc0000/ffffff', null, null, 1045, 'Tamarah', 'Isadore', null, '2022-10-23 21:18:25', '2022-10-06 22:51:30');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('df58e497-84be-4946-a3fb-c6ded13d1569', 'methodology3901', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youadapter', 'http://dummyimage.com/113x100.png/dddddd/000000', null, null, 5615, 'Suki', 'Abramo', null, '2022-05-04 00:37:05', '2022-08-17 19:22:30');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('2c611dd8-599d-4129-a746-3d68913b3f35', 'context-sensitive1763', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youSwitchable', 'http://dummyimage.com/229x100.png/5fa2dd/ffffff', null, null, 5792, 'Hervey', 'Jaymie', null, '2022-10-24 03:55:20', '2022-06-17 07:13:19');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('f3e7c159-eb22-4cf4-a0b5-c8eceebf3875', 'disintermediate377', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youhomogeneous', 'http://dummyimage.com/145x100.png/cc0000/ffffff', null, null, 3953, 'Ebonee', 'Bartel', null, '2022-08-17 06:39:31', '2022-09-15 21:35:56');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('17d5065e-0165-42ae-a3ac-fe27b514c2f3', 'software4517', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youUpgradable', 'http://dummyimage.com/150x100.png/5fa2dd/ffffff', null, null, 2743, 'Kalli', 'Brig', null, '2022-10-29 21:58:14', '2022-06-09 22:56:39');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('b7475dd7-f6c5-496a-a43f-10fa6809556c', 'open architecture925', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youtoolset', 'http://dummyimage.com/153x100.png/dddddd/000000', null, null, 2796, 'Martie', 'Llewellyn', null, '2022-07-15 02:08:51', '2022-09-30 10:05:42');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('dac898ee-ba34-4f99-95c8-24b946429452', 'demand-driven3345', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youasymmetric', 'http://dummyimage.com/175x100.png/cc0000/ffffff', null, null, 7563, 'Nerti', 'Lorrie', null, '2022-04-05 04:09:03', '2022-08-24 00:52:26');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('84a45881-0457-467e-93f3-fd7311e3a692', 'Mandatory351', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youutilisation', 'http://dummyimage.com/119x100.png/5fa2dd/ffffff', null, null, 6970, 'Zane', 'Dunstan', null, '2022-05-22 15:03:35', '2022-06-19 10:40:04');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('cf227a81-afd0-4f71-b9eb-618d1f055197', 'structure300', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youAmeliorated', 'http://dummyimage.com/114x100.png/ff4444/ffffff', null, null, 4984, 'Cynde', 'Reube', null, '2022-04-04 21:00:28', '2022-04-05 12:31:12');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('06a0b7c2-ee55-473a-a980-b578e7e2122c', '24/74768', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youzero administration', 'http://dummyimage.com/250x100.png/dddddd/000000', null, null, 2341, 'Esma', 'Harcourt', null, '2022-08-24 09:13:09', '2022-04-22 13:54:13');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('455989f9-aab4-49ca-be60-2de4e95da64b', 'superstructure32', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youinstallation', 'http://dummyimage.com/178x100.png/dddddd/000000', null, null, 7978, 'Edeline', 'Alberto', null, '2022-09-08 18:03:51', '2022-05-19 03:13:30');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('9e69df49-5abd-4330-9f7a-75830b97632e', 'Operative804', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youSeamless', 'http://dummyimage.com/134x100.png/5fa2dd/ffffff', null, null, 8377, 'Jeanette', 'Orren', null, '2022-09-18 05:33:03', '2022-04-18 15:25:20');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('f123d352-2e1d-46a9-a34c-660efd44b743', 'Self-enabling3956', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youtangible', 'http://dummyimage.com/162x100.png/cc0000/ffffff', null, null, 7306, 'Hayward', 'Knox', null, '2022-07-17 09:18:28', '2022-09-22 22:01:05');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('389fb208-2e98-41e9-9322-86931d343c6f', 'Multi-tiered3442', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youVersatile', 'http://dummyimage.com/245x100.png/cc0000/ffffff', null, null, 4309, 'Rebe', 'Jervis', null, '2022-09-08 23:28:06', '2022-09-13 12:11:50');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('0724e19c-eb99-4564-83d6-eedc50fe5e36', 'web-enabled2105', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youCross-platform', 'http://dummyimage.com/143x100.png/5fa2dd/ffffff', null, null, 6439, 'Alexis', 'Toiboid', null, '2022-08-18 08:14:31', '2022-04-25 13:22:53');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('fb6166b0-82d4-40cf-adbd-781d7e0e93aa', 'Team-oriented2076', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youweb-enabled', 'http://dummyimage.com/232x100.png/cc0000/ffffff', null, null, 9788, 'Sunshine', 'Neron', null, '2022-09-04 10:04:58', '2022-10-01 01:12:16');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('539326c3-6c05-44eb-bdeb-310bfd6bcf94', 'Persistent4316', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youconcept', 'http://dummyimage.com/249x100.png/cc0000/ffffff', null, null, 3510, 'Tory', 'Paolo', null, '2022-06-20 23:44:03', '2022-09-26 17:04:12');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('1b46322e-729e-42d1-a17e-412647b0a7f9', 'high-level4995', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youparallelism', 'http://dummyimage.com/172x100.png/cc0000/ffffff', null, null, 9843, 'Mohandis', 'Terry', null, '2022-10-05 00:27:30', '2022-04-27 18:50:37');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d7c095f4-1521-4caa-a009-4128cd007880', 'cohesive2333', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youVision-oriented', 'http://dummyimage.com/169x100.png/5fa2dd/ffffff', null, null, 3089, 'Fiann', 'Stanford', null, '2022-10-16 13:25:43', '2022-08-05 02:29:15');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d390758c-7b37-4662-9ef6-c45cd1d34099', 'Distributed1015', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youAdaptive', 'http://dummyimage.com/235x100.png/5fa2dd/ffffff', null, null, 9392, 'Ichabod', 'Remus', null, '2022-08-16 03:34:00', '2022-08-03 21:28:38');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ca078063-088a-45f1-812e-ccd36250a48f', 'focus group3946', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youProfound', 'http://dummyimage.com/213x100.png/ff4444/ffffff', null, null, 3248, 'Cassandra', 'Christoph', null, '2022-08-26 04:32:45', '2022-06-08 19:42:47');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('6ff19039-51e7-4ac7-af21-435a1ae905da', 'Graphical User Interface2285', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youmethodology', 'http://dummyimage.com/136x100.png/ff4444/ffffff', null, null, 2890, 'Tansy', 'Toddie', null, '2022-08-30 07:57:41', '2022-10-04 00:01:13');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('2f1b92c0-b76a-48bf-8084-b91471c2ed99', 'knowledge user2359', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youstructure', 'http://dummyimage.com/134x100.png/cc0000/ffffff', null, null, 1327, 'Rosalie', 'Chariot', null, '2022-10-31 16:10:07', '2022-08-17 18:43:19');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d54eabc6-68bf-42a1-990c-336397ec824a', 'mission-critical880', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youObject-based', 'http://dummyimage.com/208x100.png/5fa2dd/ffffff', null, null, 5736, 'Daryl', 'Patty', null, '2022-03-18 16:13:15', '2022-08-07 06:42:49');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('0c9479f6-fa10-4733-bb73-3177f9bc4518', 'Exclusive2387', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youencompassing', 'http://dummyimage.com/240x100.png/dddddd/000000', null, null, 7796, 'Barn', 'Daniel', null, '2022-07-27 08:57:54', '2022-10-07 01:09:02');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('f227b30a-3180-4a4e-a000-b20040f21d64', 'encoding2540', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youconcept', 'http://dummyimage.com/211x100.png/5fa2dd/ffffff', null, null, 1562, 'Thomasine', 'Arron', null, '2022-10-29 00:26:03', '2022-08-11 00:35:52');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('356cd772-56a6-4540-8be4-91a72e0c78f8', 'Fully-configurable1827', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youlocal area network', 'http://dummyimage.com/129x100.png/ff4444/ffffff', null, null, 9458, 'Nicolina', 'Griffy', null, '2022-06-06 00:53:36', '2022-03-16 13:38:05');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('9b143ac2-6d26-4eb7-8a9a-1725657b31f1', 'firmware783', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youCentralized', 'http://dummyimage.com/210x100.png/5fa2dd/ffffff', null, null, 8215, 'Levy', 'Dallis', null, '2022-09-30 07:18:26', '2022-10-06 22:25:34');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('99e9b569-c240-4bb4-bb1c-32a25a27ac55', 'Advanced3703', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youbottom-line', 'http://dummyimage.com/188x100.png/dddddd/000000', null, null, 8336, 'Shayne', 'Page', null, '2022-07-26 02:04:57', '2022-05-02 11:08:45');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('85f0da0c-4916-4ec9-911b-70bb3b64e44d', 'policy2612', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youInnovative', 'http://dummyimage.com/223x100.png/cc0000/ffffff', null, null, 1059, 'Tonye', 'Kleon', null, '2022-04-18 05:27:28', '2022-09-21 16:03:26');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('3070b10b-ccbe-42ac-896b-16db76052c98', 'archive3839', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youreal-time', 'http://dummyimage.com/165x100.png/ff4444/ffffff', null, null, 1431, 'Happy', 'Neron', null, '2022-04-19 01:11:56', '2022-09-27 07:10:16');
--insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('558414fe-7d8a-43f4-8e24-f87c66deda1f', 'parallelism45', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousolution-oriented', 'http://dummyimage.com/230x100.png/5fa2dd/ffffff', null, null, 1866, 'Mikey', 'Nate', null, '2022-04-01 16:23:07', '2022-08-17 14:43:53');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('f1fd857f-8599-4911-bb70-cc45bae23ad6', 'Customer-focused3150', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youworkforce', 'http://dummyimage.com/123x100.png/5fa2dd/ffffff', null, null, 2719, 'Bessy', 'Clyde', null, '2022-08-19 14:39:44', '2022-06-18 00:44:04');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('2a55ee9c-0d49-495a-8978-46f6095c16ae', 'Enhanced1152', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youdemand-driven', 'http://dummyimage.com/245x100.png/cc0000/ffffff', null, null, 8524, 'Marven', 'Gaultiero', null, '2022-08-09 17:36:01', '2022-03-14 17:24:59');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('a388441c-4aa4-4569-9947-26bd5000444b', 'Visionary955', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youreal-time', 'http://dummyimage.com/163x100.png/5fa2dd/ffffff', null, null, 2186, 'Sileas', 'Madison', null, '2022-08-22 02:22:25', '2022-04-20 13:46:03');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('4d7c3cfc-796a-47d8-acd6-6fe0c61650f8', 'dynamic713', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youobject-oriented', 'http://dummyimage.com/198x100.png/ff4444/ffffff', null, null, 7494, 'Brand', 'Moritz', null, '2022-07-28 15:24:31', '2022-04-04 17:06:44');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ce871ef2-5043-4922-abd9-213aed559498', 'full-range4591', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youencoding', 'http://dummyimage.com/221x100.png/cc0000/ffffff', null, null, 8302, 'Candis', 'Harlin', null, '2022-04-16 04:17:11', '2022-06-27 09:16:35');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('6287b7ba-8328-4212-b977-ac0b95b7ae8d', 'functionalities3211', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youtertiary', 'http://dummyimage.com/149x100.png/dddddd/000000', null, null, 3245, 'Claudette', 'Fulton', null, '2022-10-03 23:22:38', '2022-08-15 05:37:30');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('67fbf873-062d-4b61-a730-7abdafd9a206', 'Exclusive2477', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youtertiary', 'http://dummyimage.com/119x100.png/ff4444/ffffff', null, null, 5001, 'Sig', 'Justin', null, '2022-06-05 07:30:35', '2022-05-24 17:33:38');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('7afbd140-dbdd-45b0-92ca-835eab934cd9', 'motivating3623', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youTotal', 'http://dummyimage.com/249x100.png/5fa2dd/ffffff', null, null, 6398, 'Dickie', 'Carney', null, '2022-04-03 05:01:21', '2022-03-13 18:40:01');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('21db488f-1de3-4bdb-b893-5d18088ba098', 'cohesive3981', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousoftware', 'http://dummyimage.com/239x100.png/5fa2dd/ffffff', null, null, 4232, 'Esme', 'Ximenes', null, '2022-06-15 09:20:19', '2022-03-10 12:37:27');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d01733fc-8181-43d8-89ca-64b25e07f660', 'orchestration3783', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youInnovative', 'http://dummyimage.com/232x100.png/5fa2dd/ffffff', null, null, 3720, 'Flin', 'Cully', null, '2022-04-06 16:26:11', '2022-06-19 00:17:55');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('e8d7fc48-2cd4-4d10-a2d1-830512ed7ca1', 'success501', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youclient-driven', 'http://dummyimage.com/110x100.png/ff4444/ffffff', null, null, 8081, 'Hattie', 'Thornie', null, '2022-03-13 01:55:32', '2022-06-07 20:53:23');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('980172e6-4dfc-4ca0-82ed-3206dc95e36e', 'support2471', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youtertiary', 'http://dummyimage.com/216x100.png/cc0000/ffffff', null, null, 9646, 'Michaella', 'Wyn', null, '2022-04-18 07:13:10', '2022-04-21 04:20:51');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d3b52261-7a4b-4fac-90d5-2e92afc906c6', 'Synergized3512', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousoftware', 'http://dummyimage.com/202x100.png/cc0000/ffffff', null, null, 3447, 'Tarra', 'Gunther', null, '2022-10-27 11:21:38', '2022-03-27 06:28:20');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('458ecf3e-f872-4c57-be46-e642337040d0', 'Horizontal1716', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousolution', 'http://dummyimage.com/159x100.png/5fa2dd/ffffff', null, null, 3939, 'Morena', 'Brewster', null, '2022-06-22 21:25:14', '2022-06-04 06:33:39');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('1ec950b1-bcb1-4750-b399-d4d0fa757528', 'attitude1843', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youFundamental', 'http://dummyimage.com/169x100.png/dddddd/000000', null, null, 7048, 'Hernando', 'Jammal', null, '2022-10-20 10:04:49', '2022-06-09 18:57:09');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('3eef8b52-0ab2-4a23-91c3-1cd2e86ae229', 'frame84', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youRobust', 'http://dummyimage.com/237x100.png/cc0000/ffffff', null, null, 3822, 'Thomas', 'Erwin', null, '2022-09-19 19:00:46', '2022-09-27 10:41:51');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('5d9148ea-0a9c-4270-bc2e-d99a33227de5', 'definition1401', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youFace to face', 'http://dummyimage.com/146x100.png/ff4444/ffffff', null, null, 6338, 'Morris', 'Giacobo', null, '2022-10-08 03:06:27', '2022-07-03 15:53:41');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('ad91eaaf-88b0-44cc-882a-0c9a7ee4395e', 'hierarchy3612', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youarray', 'http://dummyimage.com/149x100.png/ff4444/ffffff', null, null, 8137, 'Rhonda', 'Tirrell', null, '2022-09-10 19:34:15', '2022-06-12 22:43:27');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('d0b9bfa5-045d-448f-b556-d110f4c182d9', 'attitude-oriented3460', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youclient-server', 'http://dummyimage.com/230x100.png/dddddd/000000', null, null, 1688, 'Brade', 'Rafferty', null, '2022-07-13 04:27:21', '2022-09-08 05:43:44');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('8358da39-670a-4514-888e-437f82314a23', 'solution-oriented2744', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love younon-volatile', 'http://dummyimage.com/144x100.png/cc0000/ffffff', null, null, 4941, 'Rowney', 'Jammal', null, '2022-05-12 13:17:21', '2022-08-20 18:49:40');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('17712aa8-5202-4333-9db4-f7de0f511097', 'attitude1145', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love yousuccess', 'http://dummyimage.com/138x100.png/dddddd/000000', null, null, 92, 'Trista', 'Bryce', null, '2022-08-27 00:40:14', '2022-06-23 11:12:14');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('48282b07-a514-4b24-bac9-25bfba01076e', 'Polarised4930', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youcontext-sensitive', 'http://dummyimage.com/212x100.png/cc0000/ffffff', null, null, 476, 'Shayla', 'Archibold', null, '2022-09-08 23:22:45', '2022-06-24 21:16:46');
----insert into topics (id, title, editor_state, editor_text_content, user_uploaded_image_url, user_uploaded_vod_url, evidence_links, likes, created_by_username, created_by_user_id, topic_words, created_at, updated_at) values ('de9b77a7-3a0d-49d8-87e0-c9ef2fb41b74', 'human-resource1987', '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text: ","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}', 'You should go eat a giant dick. you are very cool. I love you. But Check this out. Wow crazy. You should go eat a giant dick. you are very cool. I love youlocal area network', 'http://dummyimage.com/173x100.png/cc0000/ffffff', null, null, 500, 'Ramon', 'Horatio', null, '2022-11-03 00:44:44', '2022-06-26 04:31:39');
--
--DO $$
--DECLARE
--    random_user_id VARCHAR(50);
--    random_user_username VARCHAR(50);
--    topic_row RECORD;
--BEGIN
--    -- Loop over every entry in the topics table
--    FOR topic_row IN SELECT * FROM topics
--    LOOP
--        -- Select a random user from the users table
--        SELECT user_id, username
--        FROM users
--        ORDER BY random()
--        LIMIT 1
--        INTO random_user_id, random_user_username;
--
--        -- Update the created_by_username and created_by_user_id columns in the topics table
--        UPDATE topics SET created_by_username = random_user_username, created_by_user_id = random_user_id WHERE id = topic_row.id;
--    END LOOP;
--END $$;
--
--
--
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('32d79753-9bff-4868-97c0-33ee7805083e', null, 'YouTube', 'https://www.youtube.com/watch?v=HFCEWKX0vkV', 'UpZ700Nzfqs', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('9c41e4a6-ce7d-49b1-8bd2-34fa8aa34962', null, 'YouTube', 'https://www.youtube.com/watch?v=ocPJMSbB8Y5', 'Xz07nEBT7FQ', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('defc0151-a5da-4793-b62b-89f9e567cc57', null, 'YouTube', 'https://www.youtube.com/watch?v=7PAgGExL8OX', 'tr0qvSIhQJU', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('23f6bfa0-2c87-4a38-95fe-521dc4c5d7a8', null, 'YouTube', 'https://www.youtube.com/watch?v=2tdTaHcznEo', 'nHEwTKwj8e8', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('b79ad4a9-6e65-4b74-af8a-94d11610334f', null, 'YouTube', 'https://www.youtube.com/watch?v=Wv9KJ0F2QiR', 'RbPJD4Wh-l8', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('85caf8e2-79e0-4e9d-83c8-a49cd2c6d140', null, 'YouTube', 'https://www.youtube.com/watch?v=WhsviVj2w79', 'GM9eazSNHDg', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('cc8913e3-0c48-41cd-a9b1-d9f6656137b5', null, 'YouTube', 'https://www.youtube.com/watch?v=Io8eYXQDLg3', 'q-KgiICaytQ', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('5d6785b7-538e-4340-b17b-e53fb6426882', null, 'YouTube', 'https://www.youtube.com/watch?v=8ZHC4hdRgQt', 'U2EvbNcgdS0', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('f7f2751e-b82a-41f0-ab85-db19ceef56fd', null, 'YouTube', 'https://www.youtube.com/watch?v=vBeNMuwJgQK', 'PBLlII4lu0g', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('770a5c78-4873-4540-af05-4b09825fe540', null, 'YouTube', 'https://www.youtube.com/watch?v=VNOdm4RYuwC', 'qqa5tpDCggA', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('96e93d27-0c58-4fd9-8164-285178827502', null, 'YouTube', 'https://www.youtube.com/watch?v=zUKEgWLkm1j', 'tHpZ2vqJbMA', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('f7776fdc-e2eb-4f00-b86a-24d3bf0ef659', null, 'YouTube', 'https://www.youtube.com/watch?v=dXpoNQG190R', '1k37OcjH7BM', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('d6e2b1e5-815a-46ec-b437-712f047e282a', null, 'YouTube', 'https://www.youtube.com/watch?v=zjQdwLslOZE', '0jspaMLxBig', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('9b54cb5c-eb01-47b2-a779-20052ce089b7', null, 'YouTube', 'https://www.youtube.com/watch?v=TyUBO57Zd1n', 'Pl3x4GINtBQ', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('1f7be27b-5879-4dac-a562-e848dd0587d4', null, 'YouTube', 'https://www.youtube.com/watch?v=vnBgH8y40kO', 'ABmkVWRYv4I', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('dc79a64a-8ffa-4f8e-a817-88202ba76b83', null, 'YouTube', 'https://www.youtube.com/watch?v=eI1OLnJ0o6Y', 'C4D7Er76KGM', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('ae80b79d-ea77-4bf1-a4ed-29083db1f087', null, 'YouTube', 'https://www.youtube.com/watch?v=oQeqETGyVPb', 'ZlbB6h1d7FE', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('f61ee1dc-010f-429e-9123-4a4f31a005dc', null, 'YouTube', 'https://www.youtube.com/watch?v=X6LZObY5PEx', 'L56BdeQgd3g', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('8f206a60-bed3-40cc-a663-dd421e71eb80', null, 'YouTube', 'https://www.youtube.com/watch?v=Of21Msk3KXx', 'yiahHSzYDLU', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('972d7fe1-3e7b-454f-a65c-f4bce1f70db0', null, 'YouTube', 'https://www.youtube.com/watch?v=NaZB6mLWTFe', '2LyxoR26EFo', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('b149b5b6-9b81-4eea-9d4a-b82ec585ea64', null, 'YouTube', 'https://www.youtube.com/watch?v=S1RI0FXg6To', 'BnZ6x3J0v40', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('29330a26-6d9b-4941-8713-65234e864646', null, 'YouTube', 'https://www.youtube.com/watch?v=QrqJiWuXxv7', 'a_Tbds8_Idc', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('45078646-9765-4690-b9c0-0f24c89975ec', null, 'YouTube', 'https://www.youtube.com/watch?v=eNibFOl6qHU', 'GlVtWxvNp4s', null);
--
--
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('80db7dbe-9aa7-446f-9aae-fd17fc5bcc1a', null, 'Twitter', 'https://www.youtube.com/watch?v=phsy68ZiMrg', '1633105233885773829', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('f1e88a36-fafa-44ea-b0d2-d5335ee8021e', null, 'Twitter', 'https://www.youtube.com/watch?v=qWJlbrxvEAo', '1633141629673455617', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('7ca469f9-1beb-469b-8801-8e6dc06f355d', null, 'Twitter', 'https://www.youtube.com/watch?v=SDontlL8bV2', '1633197760462684164', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('30802c28-252c-4da8-ab79-162c20da8b57', null, 'Twitter', 'https://www.youtube.com/watch?v=hc8txfMaH7D', '1633216860027011076', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('bc534bbc-ea75-415a-abb4-716d4e9366be', null, 'Twitter', 'https://www.youtube.com/watch?v=WqUAilCu9K5', '1633194727322288128', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('1f278457-8875-4836-bfba-ea3520125b8d', null, 'Twitter', 'https://www.youtube.com/watch?v=R4MnKx0Xv7W', '1633255186859737093', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('5f621bea-7d68-4a2a-9c77-55c067453822', null, 'Twitter', 'https://www.youtube.com/watch?v=UEQLpGR1lSB', '1633095762681683973', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('2384b2cb-027a-4302-9032-05e5e48cadd9', null, 'Twitter', 'https://www.youtube.com/watch?v=Jvkpm64TGVc', '1633223250657562625', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('78434f7b-848f-4daa-9533-59afee79b18d', null, 'Twitter', 'https://www.youtube.com/watch?v=GscWifM98SX', '1633271531374534657', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('653adaac-8724-4c0a-afc6-43ae179693ae', null, 'Twitter', 'https://www.youtube.com/watch?v=JKqDXnQWNC7', '1633087168309608449', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('4faa70ff-3afa-435e-9e27-11a397998663', null, 'Twitter', 'https://www.youtube.com/watch?v=AKQjbGLhRz5', '1633088063265026048', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('f0371c12-b109-43fa-95d4-b1b7144f4fef', null, 'Twitter', 'https://www.youtube.com/watch?v=Nv9XSBUWjHD', '1633275442328207361', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('05e3fd99-aee1-4b49-840b-8e6978f54f45', null, 'Twitter', 'https://www.youtube.com/watch?v=z1nIKD9ZY67', '1633231562962780160', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('8afcef82-c782-4c4c-9042-58cecef4c98b', null, 'Twitter', 'https://www.youtube.com/watch?v=EarFxT9Chcw', '1632914531587354625', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('85aa6bee-4c7e-4bd0-9db1-7858dc70a7fc', null, 'Twitter', 'https://www.youtube.com/watch?v=dHcEVBIjoxf', '1632907799549325313', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('597aae00-2296-4c63-9854-3e094867b4f1', null, 'Twitter', 'https://www.youtube.com/watch?v=Cmuj9XaHotK', '1632908340601692163', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('b6d982de-3ac8-4758-8de4-55119dfe2eeb', null, 'Twitter', 'https://www.youtube.com/watch?v=iuZX18MPShw', '1633227556689264641', null);
--
--
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('77c4ac52-1284-4eef-a63c-27a227a9a420', null, 'Web', 'https://gop.com/rapid-response/haaland-reveals-pro-ccp-climate-policy/', null, null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('77b4ac52-1284-4eef-a63c-27a227a9a420', null, 'Twitter', 'https://twitter.com/ElastosInfo/status/1640702006317686786', '1640702006317686786', null);
--insert into external_links (id, topic_id, link_type, external_content_url, embed_id, thumb_img_url) values ('7774ac52-1284-4eef-a63c-27a227a9a420', null, 'Twitter', 'https://twitter.com/GlideFinance/status/1640933940075659265', '1640933940075659265', null);
--
--
--
--
-- DO $$
-- DECLARE
--     selected_topic_id uuid;
--     external_links_row RECORD;
-- BEGIN
--     -- Loop over every entry in the topics table
--     FOR external_links_row IN SELECT * FROM external_links
--     LOOP
--         -- Select a random user from the users table
--         SELECT id
--         FROM topics as e2
--          WHERE NOT EXISTS (
--             SELECT 1 FROM external_links AS e3
--             WHERE e3.topic_id = e2.id
--           )
--         ORDER BY random()
--         LIMIT 1
--         INTO selected_topic_id;
--
--         UPDATE external_links SET topic_id = selected_topic_id WHERE id = external_links_row.id;
--     END LOOP;
-- END $$;
--
--
--
--
--DO $$
--DECLARE
--    topic_row RECORD;
--    discussion_id UUID;
--BEGIN
--    -- Loop over each topic in the topics table
--    FOR topic_row IN SELECT * FROM topics
--    LOOP
--        -- Generate a random UUID for the discussion ID
--        discussion_id := uuid_generate_v4();
--
--        -- Insert a new row into the discussions table with the specified values
--        INSERT INTO discussions(id, created_at, created_by_username, created_by_user_id, title, editor_state, editor_text_content, evidence_links, likes, user_uploaded_image_url, user_uploaded_vod_url, discussion_key_words, topic_id, discussion_id)
--        VALUES (discussion_id, topic_row.created_at, topic_row.created_by_username, topic_row.created_by_user_id, 'General', 'General Discussion', 'General, Discussion', '{}', 0, NULL, NULL, '{}', topic_row.id, NULL);
--    END LOOP;
--END $$;
--
--
--DO $$
--DECLARE
--    user_row RECORD;
--    other_user_row RECORD;
--BEGIN
--    -- Loop over each row in the users table
--    FOR user_row IN SELECT * FROM users
--    LOOP
--        -- Loop over each other row in the users table
--        FOR other_user_row IN SELECT * FROM users WHERE id != user_row.id
--        LOOP
--            -- Insert a new row into the follows table with the user_id and followed_user_id values
--            INSERT INTO follows(user_id, followed_user_id) VALUES (user_row.user_id, other_user_row.user_id);
--        END LOOP;
--    END LOOP;
--END $$;
--
--
--DO $$
--DECLARE
--  topic_row RECORD;
--  user_row RECORD;
--  name_row RECORD;
--  count_result INT;
--
--BEGIN
--  FOR topic_row IN SELECT * FROM topics LOOP
--    FOR i IN 1..5 LOOP
--      SELECT * FROM names ORDER BY random() LIMIT 1 INTO name_row;
--      SELECT COUNT(*) FROM discussions WHERE topic_id = topic_row.id AND title = name_row.name INTO count_result;
--      IF count_result = 0 THEN
--          SELECT * FROM users ORDER BY random() LIMIT 1 INTO user_row;
--          INSERT INTO discussions (id, created_by_username, created_by_user_id, title, editor_state, editor_text_content, evidence_links, likes, user_uploaded_image_url, user_uploaded_vod_url, discussion_key_words, topic_id, discussion_id)
--          VALUES (uuid_generate_v4(), user_row.username, user_row.user_id, name_row.name, topic_row.editor_state, topic_row.editor_text_content, '{}', 0, null, 'https://civil-dev.s3.us-west-1.amazonaws.com/topic_video/93e2715d-89bb-4fd7-a4ab-25613f79602a.mp4', '{}', topic_row.id, null);
--      END IF;
--    END LOOP;
--  END LOOP;
--END $$;
--
--
--
--CREATE TABLE random_tweets (
--    id SERIAL,
--    conversation TEXT
--);
--
--
--COPY random_tweets(conversation)
--FROM 'C:\Users\coomb\Desktop\civil_windows_dev\civil_backend_latest\civil\src\main\resources\db\mock_data\convos_reddit.csv'
--WITH (FORMAT csv, HEADER true);
--
---- UPDATE random_tweets
---- SET conversation = substring(conversation from 1 for length(conversation) - 1);
--
--DO $$ DECLARE
--  discussion_row RECORD;
--  user_row RECORD;
--  tweet RECORD;
--BEGIN
--  FOR discussion_row IN SELECT * FROM discussions LOOP
--    FOR i IN 1..40 LOOP
--      SELECT * FROM users ORDER BY random() LIMIT 1 INTO user_row;
--      SELECT * FROM random_tweets ORDER BY random() LIMIT 1 INTO tweet;
--      -- SELECT format('"{\"root\":{\"children\":[{\"children\":[{\"detail\":0,\"format\":0,\"mode\":\"normal\",\"style\":\"\",\"text\":\"%s",\"type\":\"text\",\"version\":1}],\"direction\":\"ltr\",\"format\":\"\",\"indent\":0,\"type\":\"paragraph\",\"version\":1}],\"direction\":\"ltr\",\"format\":\"\",\"indent\":0,\"type\":\"root\",\"version\":1}}"', tweet.text) into formatted;
--      INSERT INTO comments (id, created_by_username, created_by_user_id, sentiment, editor_state, editor_text_content, likes, root_id, parent_id, source, report_status, toxicity_status, topic_id, discussion_id)
--      VALUES (uuid_generate_v4(), user_row.username, user_row.user_id,
--        CASE floor(random() * 3) + 1
--          WHEN 1 THEN 'POSITIVE'
--          WHEN 2 THEN 'NEUTRAL'
--          ELSE 'NEGATIVE'
--        END,
--       '{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"Dummy Text:","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1},{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"replaceMe","type":"text","version":1}],"direction":"ltr","format":"","indent":0,"type":"paragraph","version":1}],"direction":"ltr","format":"","indent":0,"type":"root","version":1}}',
--       substring(tweet.conversation from 1 for 400),
--        0,
--        null,
--        null,
--        null,
--        'Clean',
--        'Clean',
--        discussion_row.topic_id,
--        discussion_row.id
--      );
--    END LOOP;
--  END LOOP;
--END $$;
--
--
--DO $$
--DECLARE
--  comment_row RECORD;
--  random_tweet text;
--BEGIN
--  FOR comment_row IN SELECT * FROM comments
--  LOOP
--    SELECT conversation INTO random_tweet FROM random_tweets ORDER BY random() LIMIT 1;
--    UPDATE comments SET editor_state = REPLACE(editor_state, 'replaceMe', random_tweet) WHERE id = comment_row.id;
--  END LOOP;
--END $$;
--
--DO $$
--DECLARE
--  topic_row RECORD;
--  random_tweet text;
--BEGIN
--  FOR topic_row IN SELECT * FROM topics
--  LOOP
--    SELECT conversation INTO random_tweet FROM random_tweets ORDER BY random() LIMIT 1;
--    UPDATE topics SET editor_state = REPLACE(editor_state, 'replaceMe', random_tweet), editor_text_content = random_tweet WHERE id = topic_row.id;
--  END LOOP;
--END $$;



-- DO $$
-- DECLARE
--   i INT;
--   random_comment RECORD;
--   tweet RECORD;

-- BEGIN
--   FOR i IN 1..19000 LOOP
--     -- Select a random comment from the comments table
--     SELECT * FROM comments ORDER BY RANDOM() LIMIT 1 INTO random_comment;
--     SELECT * FROM random_tweets ORDER BY random() LIMIT 1 INTO tweet;

--     -- Insert a new comment with the parent_id and root_id set to the values from the random comment
--     INSERT INTO comments (editor_state, editor_text_content, created_by_username, created_by_user_id, discussion_id, topic_id, sentiment, likes, root_id, parent_id, source, report_status, toxicity_status)
--     VALUES (random_comment.editor_state,
--     random_comment.editor_text_content,
--     random_comment.created_by_username,
--     random_comment.created_by_user_id,
--     random_comment.discussion_id,
--     random_comment.topic_id,
--     random_comment.sentiment,
--     0,
--     CASE
--       WHEN random_comment.root_id IS NULL THEN random_comment.id
--       ELSE random_comment.root_id
--      END,
--     random_comment.id,
--     random_comment.source,
--     'Clean',
--     'Clean'
--     );
--   END LOOP;
-- END $$;



ALTER TABLE external_links
ADD CONSTRAINT fk_spaces
  FOREIGN KEY(space_id)
  REFERENCES spaces(id);

