package com.uacam.minidbeaver.mini_dbeaver.model;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SSHTunnelServicio {
    private static final SSHTunnelServicio INSTANCE =
            new SSHTunnelServicio();

    private Session session;
    private int localPort = 3307;

    private SSHTunnelServicio() {
    }
    public static SSHTunnelServicio getInstance() {
        return INSTANCE;
    }
    public synchronized int connect(
            String host,
            String sshUser,
            String sshPassword) throws Exception {

        disconnect();
        JSch jsch = new JSch();
        session = jsch.getSession(sshUser, host, 22);
        session.setPassword(sshPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(15000);
        localPort = findAvailablePort();
        session.setPortForwardingL(localPort, "127.0.0.1", 3306);
        return localPort;
    }
    private int findAvailablePort() throws Exception {
        for (int port = 3307; port <= 3399; port++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                return port;
            } catch (java.io.IOException e) {
            }
        }
        throw new IllegalStateException("No hay un puerto local disponible.");
    }
    public synchronized void disconnect() {
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
            session = null;
        }
    }
}