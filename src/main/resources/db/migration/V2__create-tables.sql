create table coupon (
    id UUID default gen_random_uuid() primary key,
    code varchar(100) not null,
    discount integer not null,
    valid timestamp not null,
    event_id UUID,
    foreign key (event_id) references event(id) on delete cascade
);

create table address (
    id UUID default gen_random_uuid() primary key,
    city varchar(100) not null,
    uf varchar(100) not null,
    event_id UUID,
    foreign key (event_id) references event(id) on delete cascade
);