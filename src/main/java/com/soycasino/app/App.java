package com.soycasino.app;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.stop;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

import spark.Request;
import spark.Response;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        StdErrLog log = new StdErrLog();
        Log.setLog(log);
        log.setLevel(StdErrLog.LEVEL_WARN);
        staticFileLocation("res");

        get("/hello", (req, res) -> "Hello World");
        post("/login", App::login);
        get("/exit", (req, res) -> {
            stop();
            return "";
        });
    }
    
    public static String login(Request req, Response res) {
        return req.queryParams("username");
    }
}
