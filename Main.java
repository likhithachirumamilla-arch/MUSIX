package com.musix;

import com.musix.server.MusixHttpServer;

public class Main {
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        MusixHttpServer server = null;
        int[] candidatePorts = (port == DEFAULT_PORT)
                ? new int[]{8080, 8081, 8082, 8088, 3000}
                : new int[]{port, 8080, 8081, 8082};

        for (int p : candidatePorts) {
            try {
                server = new MusixHttpServer(p);
                server.start();
                break;
            } catch (Exception e) {
                System.err.println("Port " + p + " is in use or blocked (" + e.getMessage() + "), attempting alternate port...");
            }
        }

        if (server != null) {
            final MusixHttpServer activeServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down MUSIX Server...");
                activeServer.stop();
            }));
        } else {
            System.err.println("CRITICAL: Failed to bind MUSIX Server to any available port.");
        }
    }
}
