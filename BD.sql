create table Usuarios(
    id int auto_increment not null,
    nombreUsuario varchar(20) not null,
    clave varchar(60) not null,
    primary key (id)
);

create table Amigos(
    id1 int not null,
    id2 int not null,
    primary key (id1,id2),
    foreign key (id1) references Usuarios(id)
        on update cascade
        on delete cascade,
    foreign key (id2) references Usuarios(id)
        on update cascade
        on delete cascade
);

create table Solicitudes(
    id_solicitud int auto_increment not null,
    id1 int not null,
    id2 int not null,
    estado ENUM('pendiente', 'aceptada', 'rechazada') DEFAULT 'pendiente',
    fecha_solitud date default current_date,
    primary key (id_solicitud,id1,id2),
    foreign key (id1) references Usuarios(id) 
        ON delete cascade,
        on update cascade
    foreign key (id2) references Usuarios(id)
        on update cascade
        on delete cascade
)

