create table track_histories (
    id serial,
    title text,
    url text,
    thumbnail text,
    duration integer,
    author text,
    done boolean,
    primary key (id)
);