package com.migrations;

import java.util.Map;

import org.flywaydb.core.Flyway;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Main.wait(5000);
        JsonElement root = new JsonParser().parse(System.getenv("BACKENDMONOCLUSTER_SECRET"));
        String password = root.getAsJsonObject().get("password").toString();

        Flyway flyway = Flyway.configure().dataSource("jdbc:postgresql://civil-test-backend-mono-a-backendmonoclusterdbclus-c2eih4ntn4fn.cluster-clzyqw0pwv43.us-west-1.rds.amazonaws.com:5432/civil_main", "postgres", "2bqFGWPKYQXT3deP")
                .load();
        flyway.migrate();
    }
}
