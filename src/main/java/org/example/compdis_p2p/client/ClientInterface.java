package org.example.compdis_p2p.client;

import java.rmi.Remote;

public interface ClientInterface extends Remote {
    void sendMsg(ClientInterface client);
    void receiveMsg(ClientInterface client);

    ClientInterface receiveConnectNotification(ClientInterface other);
}
