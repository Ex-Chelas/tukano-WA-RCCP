create table user(
    userId varchar primary key,
    pwd varchar,
    email varchar,
    displayName varchar
);

create table short(
    shortId varchar primary key,
    ownerId varchar,
    blobUrl varchar,
    timestamp bigint,
    totalLikes int
);

create table likes(
    userId varchar,
    shortId varchar,
    primary key (userId, shortId)
);

create table following(
    follower varchar,
    followee varchar,
    primary key (follower, followee)
);
