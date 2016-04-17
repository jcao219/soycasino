package com.soycasino.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.hc.client5.http.impl.sync.HttpClients;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.client5.http.methods.HttpGet;
import org.apache.hc.client5.http.sync.HttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.entity.StringEntity;
import org.apache.http.Consts;

import com.google.gson.Gson;
import com.soycasino.json.AccountResult;
import com.soycasino.json.CreateAccount;
import com.soycasino.json.CreateAccountResult;
import com.soycasino.json.CreateCustomerResult;

import spark.utils.IOUtils;

public class ApiCall {
    private String apikey;

    public ApiCall(String apikey) { this.apikey = apikey; }
    
    public CreateAccountResult createAccount(String cust_id, String type, String nickname, int rewards, int balance) throws Exception {
        String URL = "http://api.reimaginebanking.com/customers/%s/accounts?key=%s";
        String req_url = String.format(URL, cust_id, apikey);

        CreateAccount cacc = new CreateAccount(type, nickname, rewards, balance, null);
        CreateAccountResult result = doPost(cacc, req_url, CreateAccountResult.class);
        return result;
    }
    
    private <T, U> U doPost(T data, String req_url, Class<U> resultType) throws Exception {
        HttpClient cli = HttpClients.createDefault();
        HttpPost post = new HttpPost(req_url);
        Gson gson = new Gson();
        String body_json = gson.toJson(data);
        System.out.println("To: " +resultType.toString() + "Sending: " + body_json);
        StringEntity stringEntity = new StringEntity(body_json, Consts.UTF_8);
        stringEntity.setContentType("application/json");
        post.setEntity(stringEntity);
        HttpResponse response;
        
        Object ccres = null;
        try {
            System.out.println(IOUtils.toString(post.getEntity().getContent()));
            response = cli.execute(post);
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                InputStream instream = entity.getContent();
                ccres = gson.fromJson(new InputStreamReader(instream), resultType);
            } else {
                throw new Exception("Null entity.");
            }
        } catch (Exception e) {
            throw e;
        }
        
        return ccres == null ? null : (U)ccres;
    }

    public String getAccountsJson(String cust_id) throws IOException {
        String URL = "http://api.reimaginebanking.com/customers/%s/accounts?key=%s";
        String req_url = String.format(URL, cust_id, apikey);
        
        HttpClient cli = HttpClients.createDefault();
        HttpGet mtd = new HttpGet(req_url);
        HttpResponse hresp = cli.execute(mtd);
        HttpEntity ent = hresp.getEntity();
        if(ent != null) {
            return IOUtils.toString(hresp.getEntity().getContent());
        } else {
            throw new IOException("Null entity.");
        }
    }
    
    public <U> U doGet(String url, Class<U> result) throws IOException {
        Gson gson = new Gson();
        HttpClient cli = HttpClients.createDefault();
        HttpGet mtd = new HttpGet(url);
        HttpResponse hresp = cli.execute(mtd);
        HttpEntity ent = hresp.getEntity();
        Object ccres = null;
        if(ent != null) {
            ccres = gson.fromJson(new InputStreamReader(hresp.getEntity().getContent()), result);
        } else {
            throw new IOException("Null entity.");
        }
        
        return ccres == null ? null : (U)ccres;
    }
    
    public String getAccounts(String cust_id) throws Exception {
        throw new UnsupportedOperationException("Unimplemented.");
    }
    
    public AccountResult getAccountById(String acc_id) throws Exception {
        String urlForm = "http://api.reimaginebanking.com/accounts/%s?key=%s";
        String url = String.format(urlForm, acc_id, apikey);
        AccountResult result = doGet(url, AccountResult.class);
        return result;
    }
}
