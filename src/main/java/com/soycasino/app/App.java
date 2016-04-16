package com.soycasino.app;

import static spark.Spark.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        staticFileLocation("com/soycasino/res");

        get("/hello", (req, res) -> "Hello World");
        get("/exit", (req, res) -> {
            stop();
            return "";
        });
    }
}
