create table if not exists Usuarios (
    nombreUsuario varchar(20) not null,
    clave varchar(60) not null,
    primary key (nombreUsuario)
);

create table if not exists Amigos (
    nombreUsuario1 int not null,
    nombreUsuario2 int not null,
    primary key (nombreUsuario1,nombreUsuario2),
    foreign key (nombreUsuario1) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    foreign key (nombreUsuario2) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    constraint check_usuarios_diferentes check (nombreUsuario1 != nombreUsuario2)
);

create table if not exists Solicitudes(
    -- TODO: PK fecha + user1 + user2
    -- Preferiblemente no mezclar snake_case con camelCase
    -- (como a SQL le dan igual las mayusculas, tecnicamente es mejor snake_case)
    id_solicitud int auto_increment,
    nombreUsuario1 int not null,
    nombreUsuario2 int not null,
    estado text not null default 'pendiente',
    --fecha_solitud date default current_date,
    primary key (id_solicitud, nombreUsuario1, nombreUsuario2),
    foreign key (nombreUsuario1) references Usuarios(nombreUsuario)
        on delete cascade
        on update cascade,
    foreign key (nombreUsuario2) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    constraint check_estado check (estado in ('pendiente', 'aceptada', 'rechazada'))
);
