package com.migrations;

import org.flywaydb.core.Flyway;


public class Runner {
  public static void wait(int ms) {
    try{
        Thread.sleep(ms);
    } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}

  public static void main(String[] args) {
    Runner.wait(5000);
    Flyway flyway = Flyway.configure().dataSource("jdbc:postgresql://db:54323/civil", "postgres", "postgres")
        .load();
    flyway.migrate();
  }
}
