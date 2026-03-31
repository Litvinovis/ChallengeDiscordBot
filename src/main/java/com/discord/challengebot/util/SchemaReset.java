package com.discord.challengebot.util;

import org.apache.ignite.client.IgniteClient;

public class SchemaReset {
    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder().addresses("127.0.0.1:10300").build()) {
            System.out.println("Connected to Ignite 3");
            client.sql().execute(null, "DROP TABLE IF EXISTS challenges");
            System.out.println("Table challenges DROPPED");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
