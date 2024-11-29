create table if not exists Usuarios (
    nombreUsuario text not null,
    clave text not null,
    primary key (nombreUsuario)
);

create table if not exists Amigos (
    nombreUsuario1 text not null,
    nombreUsuario2 text not null,
    primary key (nombreUsuario1, nombreUsuario2),
    foreign key (nombreUsuario1) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    foreign key (nombreUsuario2) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    constraint check_usuarios_diferentes check (nombreUsuario1 != nombreUsuario2)
);

create table if not exists Solicitudes (
    fechaSolicitud timestamp not null default current_timestamp,
    emisor text not null,
    receptor text not null,
    estado text not null default 'pendiente',
    primary key (emisor, receptor,fechaSolicitud),
    --primary key (fechaSolicitud, emisor, receptor),
    foreign key (emisor) references Usuarios(nombreUsuario)
        on delete cascade
        on update cascade,
    foreign key (receptor) references Usuarios(nombreUsuario)
        on update cascade
        on delete cascade,
    -- Asegurarse que son los mismos estados que en server.FriendStatus
    constraint check_estado check (estado in ('pendiente', 'aceptada', 'rechazada'))
    constraint check_usuarios_diferentes check (emisor != receptor),
);
