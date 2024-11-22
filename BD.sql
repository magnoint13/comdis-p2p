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

-- TODO: esto daba error cerca de 'pendiente'
--create table Solicitudes(
    --id_solicitud int auto_increment not null,
    --nombreUsuario1 int not null,
    --nombreUsuario2 int not null,
    --estado ENUM('pendiente', 'aceptada', 'rechazada') DEFAULT 'pendiente',
    --fecha_solitud date default current_date,
    --primary key (id_solicitud,nombreUsuario1,nombreUsuario2),
    --foreign key (nombreUsuario1) references Usuarios(nombreUsuario)
        --ON delete cascade,
        --on update cascade
    --foreign key (nombreUsuario2) references Usuarios(nombreUsuario)
        --on update cascade
        --on delete cascade
--)

