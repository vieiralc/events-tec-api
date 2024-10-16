create extension if not exists "pgcrypto";

create table event (
    id UUID default gen_random_uuid() primary key,
    title varchar(100) not null,
    description varchar(250) not null,
    img_url varchar(100) not null,
    event_url varchar(100) not null,
    date timestamp not null,
    remote boolean not null
);