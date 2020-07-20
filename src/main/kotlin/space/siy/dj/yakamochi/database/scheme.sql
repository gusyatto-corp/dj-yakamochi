create table guilds
(
    id text,
    name     text,
    icon     text,
    primary key (id)
);

create table users
(
    id text,
    name    text,
    icon    text,
    primary key (id)
);

create table track_histories
(
    id         serial,
    title      text,
    url        text,
    thumbnail  text,
    duration   integer,
    author     text,
    guild      text,
    done       boolean,
    created_at timestamptz default now(),
    primary key (id),
    foreign key (author) references users (id),
    foreign key (guild) references guilds (id)
);

create table auth (
    id serial,
    user_id text,
    provider text,
    access_token text,
    refresh_token text,
    primary key(id),
    foreign key (user_id) references users(id)
)