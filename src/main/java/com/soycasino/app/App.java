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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

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
import com.soycasino.json.CreateCustomer;
import com.soycasino.json.CreateCustomerResult;

import spark.Request;
import spark.Response;
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
        staticFileLocation("res");

        get("/hello", (req, res) -> "Hello World");
        post("/login", App::login);
        post("/signup", App::signup);
        
//        serveStatic("/lobby.html");
//        serveStatic("/login.html");
//        serveStatic("/blackjack.html");
//        serveStatic("/poker.html");
//        serveStatic("/signup.html");
    }

    static String readResource(String resourceName) throws IOException {
        InputStream stream = App.class.getResourceAsStream(resourceName);
        System.out.println(resourceName);
        return IOUtils.toString(stream);
    }
    
    public static void serveStatic(String servPath) {
        get(servPath, (res, req) -> {
            System.out.println("trying");
            try {
                String result = readResource(servPath);
                System.out.println("res: " + result);
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
    
    public static String login(Request req, Response res) {
        return req.queryParams("username");
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
    
    public static String signup(Request req, Response res) {
        List<String> required = Arrays.asList("first_name", "last_name", "email", "password");
        for(String s : required) {
            if(!req.queryParams().contains(s)) {
                res.status(400);
                return s + " is required.";
            }
        }
        String db_path = jarPathOf("users.db");
        File try_this_file = new File(db_path);
        if(!try_this_file.exists()) {
            try {
                db_path = exportResource("/users.db");
            } catch(Exception e) {
                serverError(res, e);
            }
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
