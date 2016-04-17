package com.soycasino.app;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hc.client5.http.impl.sync.HttpClients;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.client5.http.sync.HttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.entity.StringEntity;
import org.apache.http.Consts;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

import com.google.gson.Gson;
import com.soycasino.json.Address;
import com.soycasino.json.CreateAccountResult;
import com.soycasino.json.CreateCustomer;
import com.soycasino.json.CreateCustomerResult;

import spark.Request;
import spark.Response;
import spark.Spark;
import spark.utils.IOUtils;

/**
 * Hello world!
 *
 */
public class App 
{
    static String API_KEY = "c473b1a4c534ec45fd7dadb8ad4c5672";
    static String jarFolder;

    static {
        try {
            jarFolder = new File(App.class.getProtectionDomain().getCodeSource().
                    getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main( String[] args )
    {
        StdErrLog log = new StdErrLog();
        Log.setLog(log);
        log.setLevel(StdErrLog.LEVEL_WARN);
        Spark.staticFileLocation("static");

        get("/hello", (req, res) -> "Hello World");
        post("/login", App::login);
        post("/signup", App::signup);
        get("/signout", (req, res) -> {
          if(!verifyLoggedIn(req, res)) {
              return "Redirect to login.";
          }
          res.removeCookie("_id");
          res.redirect("/");
          return "Signed out";
        });
        post("/addAccount", App::addAccount);
        get("/getAccounts", App::getAccounts);
        post("/updateAccounts", App::updateAccounts);
        
        //                     NEEDLOGIN
        serveStatic("/lobby.html", true);
        serveStatic("/login.html", false);
        serveStatic("/blackjack.html", true);
        serveStatic("/poker.html", true);
        serveStatic("/signup.html", false);
        serveStatic("/accounts.html", true);
        
        serveStatic("/", "/login.html", false);
        

        /*get("/*", (q, a) -> {
            throw new NotFoundException();
        });

        exception(NotFoundException.class, (e, request, response) -> {
            response.status(404);
            response.body("Not found :(");
        });  -- Bug?!  Doesn't work, and there is no workaround. */
    }
    
    private static String updateAccounts(Request req, Response res) {
        if(!verifyLoggedIn(req, res)) {
            return "not logged in.";
        }
        if(!validateParams(res, req.queryParams(), "acc_id")) {
            return "Nothing updated.";
        }
        res.cookie("_acc", req.queryParams("acc_id"));
        return "Updated account number.";
    }
    
    static ApiCall api = new ApiCall(API_KEY);
    
    private static String addAccount(Request req, Response res) {
        if(!verifyLoggedIn(req, res)) {
            return "Not logged in.";
        }
        if(!validateParams(res, req.queryParams(), "type", "nickname", "deposit")) {
            return "required param missing";
        }
        try {
            CreateAccountResult car = api.createAccount(req.cookie("_id"),
                req.queryParams("type"),
                req.queryParams("nickname"), 0, 
                Integer.parseInt(req.queryParams("deposit")));
            if(car == null) {
                res.status(404);
                return "Not found: null result.";
            } else if(car.code != 201) {
                return errorPage + car.message; 
            } else {
                res.redirect("/accounts.html");
                res.status(200);
                return "Done!";
            }
        } catch (Exception e) {
            return errorPage + e.getMessage();
        }
    }
    
    static final String errorPage = "<!doctype html><title>Error</title> <a href=\"/lobby.html\">Back to lobby</a><br><p>";
    
    static Map<String, LocalTime> throttleGetAccounts = new ConcurrentHashMap<>();
    private final static long SECONDS_THROTTLE = 10L; // how many seconds to force wait.
    
    private static String getAccounts(Request req, Response res) {
        if(!verifyLoggedIn(req, res)) {
            return "Not logged in.";
        }
        String cust_id = req.cookie("_id");
        LocalTime lt;
        if((lt = throttleGetAccounts.getOrDefault(cust_id, null)) != null) {
            if(ChronoUnit.SECONDS.between(lt, LocalTime.now()) < SECONDS_THROTTLE) {
                res.status(403);
                return "";
            }
        }
        try {
            String rs = api.getAccountsJson(cust_id);
            throttleGetAccounts.put(cust_id, LocalTime.now());
            return rs;
        } catch (IOException e) {
            return serverError(res, e);
        }
    }

    private static boolean validateParams(Response res, Set<String> queryParams, String... items) {
        for(String s : items) {
            if(!queryParams.contains(s)) {
                res.status(400);
                return false;
            }
        }
        return true;
        
    }

    private static boolean verifyLoggedIn(Request req, Response res) {
        // VERY INSECURE
        String cust_id = req.cookie("_id");
        if(cust_id == null) {
            res.redirect("/login.html");
            return false;
        }
        return true;
    }

    private static void serveStatic(String routePath, String servPath, boolean needLogin) {
        get(routePath, (res, req) -> {
            String cust_id = res.cookie("_id"); // customer id
            
            if(needLogin) {
                if(!verifyLoggedIn(res, req)) {
                    return "Need to login...";
                }
            } else if (cust_id != null && !needLogin) {
                // TODO: validate it.
                req.redirect("/lobby.html");
                return "Redirecting to lobby...";
            }
            
            System.out.println("trying" + servPath);
            try {
                String resPath = Paths.get("/res", servPath).toString();
                String result = readResource(resPath);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                return "noo";
            }
        }); 
    }

    static String readResource(String resourceName) throws IOException {
        InputStream stream = App.class.getResourceAsStream(resourceName);
        System.out.println(resourceName);
        return IOUtils.toString(stream);
    }
    
    /**
     * If needLogin is true: you need to be logged in to access this page.
     * @param servPath
     * @param needLogin
     */
    public static void serveStatic(String servPath, boolean needLogin) {
        serveStatic(servPath, servPath, needLogin);
    }
    
    public static String login(Request req, Response res) {
        Connection connection;
        try {
            System.out.println("DBPATH: " + db_path);
            connection = DriverManager.getConnection("jdbc:sqlite:" + db_path);
            PreparedStatement stmt = connection.prepareStatement("SELECT password,account FROM users WHERE email = ?");
            stmt.setString(1, req.queryParams("username"));
            ResultSet set = stmt.executeQuery();
            String hashPass = hash(req.queryParams("password"));
            while(set.next()) {
                if(set.getString(1).equals(hashPass)) {
                    res.cookie("_id", set.getString(2));
                    connection.close();
                    res.redirect("/lobby.html");
                    return "Success!";
                }
            }
            connection.close();
            return "Login failed.";
        } catch (Exception e1) {
            return serverError(res, e1);
        }
    }
    
    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     * @throws Exception
     */
    static public String exportResource(String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        try {
            stream = App.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            resStreamOut = new FileOutputStream(jarPathOf(resourceName));
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            try {
                stream.close();
                resStreamOut.close();
            } catch(Exception ex) {
                 
            }
        }

        return jarPathOf(resourceName);
    }

    private static String jarPathOf(String resourceName) {
        return Paths.get(jarFolder,resourceName).toString();
    }
    
    static String db_path = jarPathOf("users.db");
    
    static {
        File try_this_file = new File(db_path);
        if(!try_this_file.exists()) {
            try {
                db_path = exportResource("/users.db");
            } catch(Exception e) {
                System.out.println("A serious startup exception occured.");
                e.printStackTrace();
            }
        }
    }

    public static String signup(Request req, Response res) {
        if(!App.validateParams(res, req.queryParams(), "first_name", "last_name", "email", "password")) {
            return "Required param missing.";
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + db_path);
            PreparedStatement stmt = connection.prepareStatement("SELECT count(*) FROM users WHERE email = ?");
            stmt.setString(1, req.queryParams("email"));
            ResultSet set = stmt.executeQuery();
            set.next();
            int count = set.getInt(1);
            if(count != 0) {
                return "Error: The email is already registered.";
            }
        } catch (SQLException e1) {
            return serverError(res, e1);
        }

        HttpClient cli = HttpClients.createDefault();
        String url = "http://api.reimaginebanking.com/customers?key=" + API_KEY;
        HttpPost post = new HttpPost(url);
        CreateCustomer cust = new CreateCustomer(req.queryParams("first_name"),
                                                 req.queryParams("last_name"), 
                                                 new Address());
        Gson gson = new Gson();
        String body_json = gson.toJson(cust);
        System.out.println("Sending: " + body_json);
        StringEntity stringEntity = new StringEntity(body_json, Consts.UTF_8);
        stringEntity.setContentType("application/json");
        post.setEntity(stringEntity);
        HttpResponse response;
        
        CreateCustomerResult ccres = null;
        try {
            System.out.println(IOUtils.toString(post.getEntity().getContent()));
            response = cli.execute(post);
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                InputStream instream = entity.getContent();
                ccres = gson.fromJson(new InputStreamReader(instream), CreateCustomerResult.class);
            } else {
                return serverError(res, new Exception("Null entity: from API call in signup."));
            }
        } catch (IOException e) {
            return serverError(res, e);
        }
        
        if(ccres == null) {
            return serverError(res, new Exception("Null CreateCustomerResult"));
        } else if (ccres.code != 201) {
            return serverError(res, new Exception(ccres.message));
        }
        
        PreparedStatement stmt2;
        try {
            String pass_hash = hash(req.queryParams("password"));
            stmt2 = connection.prepareStatement("INSERT INTO users (email, password, account) values (?, ?, ?)");
            stmt2.setString(1, req.queryParams("email"));
            stmt2.setString(2, pass_hash);
            stmt2.setString(3, ccres.objectCreated._id);
            stmt2.execute();
            if(!connection.getAutoCommit()) {
               connection.commit();
            }
            connection.close();
        } catch (Exception e) {
            return serverError(res, e);
        }
        res.cookie("_id", ccres.objectCreated._id);
        res.redirect("/lobby.html");
        
        return "Successfully created account!";
    }

    private static String hash(String queryParams) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(queryParams.getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }

    private static String serverError(Response res, Exception e) {
        res.status(500);
        e.printStackTrace();
        return "An error has occurred";
    }
}
