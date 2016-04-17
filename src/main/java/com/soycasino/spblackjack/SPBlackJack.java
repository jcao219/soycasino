package com.soycasino.spblackjack;

import static spark.Spark.get;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.soycasino.app.App;

import spark.Request;
import spark.Response;

public class SPBlackJack {
    public static final String[] cards = {
            "ace-of-clubs","two-of-clubs","three-of-clubs","four-of-clubs","five-of-clubs",
            "six-of-clubs","seven-of-clubs","eight-of-clubs","nine-of-clubs","ten-of-clubs",
            "jack-of-clubs","queen-of-clubs","king-of-clubs","ace-of-spades","two-of-spades",
            "three-of-spades","four-of-spades","five-of-spades","six-of-spades","seven-of-spades",
            "eight-of-spades","nine-of-spades","ten-of-spades","jack-of-spades","queen-of-spades",
            "king-of-spades","ace-of-hearts","two-of-hearts","three-of-hearts","four-of-hearts",
            "five-of-hearts","six-of-hearts","seven-of-hearts","eight-of-hearts","nine-of-hearts",
            "ten-of-hearts","jack-of-hearts","queen-of-hearts","king-of-hearts","ace-of-diamonds",
            "two-of-diamonds","three-of-diamonds","four-of-diamonds","five-of-diamonds","six-of-diamonds",
            "seven-of-diamonds","eight-of-diamonds","nine-of-diamonds","ten-of-diamonds","jack-of-diamonds",
            "queen-of-diamonds","king-of-diamonds"
    };
    
    public static final Integer[] values = {
            11,2,3,4,5,6,7,8,9,10,10,10,10,11,2,3,4,5,6,7,8,9,10,10,10,10,11,
            2,3,4,5,6,7,8,9,10,10,10,10,11,2,3,4,5,6,7,8,9,10,10,10,10
    };

    public static void registerRoutes() {
        get("/gameInfo/bj4real", SPBlackJack::getInfo);
    }
    
    // Implementing  shuffle
    static void shuffleArrays()
    {
      Random rnd = ThreadLocalRandom.current();
      for (int i = cards.length - 1; i > 0; i--)
      {
        int index = rnd.nextInt(i + 1);
        // Simple swap
        int a = values[index];
        String b = cards[index];
        values[index] = values[i];
        cards[index] = cards[i];
        values[i] = a;
        cards[i] = b;
      }
    }
    
    public static String getInfo(Request req, Response res) {
        if(!App.verifyLoggedIn(req, res)) {
            return "not logged in";
        }
        if(!App.verifyHasAcc(req, res)) { // TODO
            return App.errorPage + "Need to select primary acc. in accounts page!";
        }
        if(!(App.allGameStates.get(req.cookie("_id")) instanceof GameInfo)) {
            App.allGameStates.remove(req.cookie("_id"));
            return App.errorPage + "invalid session.";
        }
        GameInfo prev = (GameInfo) App.allGameStates.get(req.cookie("_id"));
        GameInfo result = new GameInfo();
        if(prev.gameState.equals("init")) {
            shuffleArrays();
            int i = prev.dealt;
            result.gameState = "deal";
            result.dealer1 = cards[i];
            result.dealer = values[i++];

            result.player1 = cards[i];
            result.player = values[i++];
            result.player2 = cards[i];
            result.player += values[i++];
            result.dealt = i;
        } else {
            result.gameState = "afterDeal";
        }
        try {
            result.balance = App.getFunds(req.cookie("_acc"));
        } catch (Exception e) {
            return App.errorPage + e.getMessage();
        }
        App.allGameStates.put(req.cookie("_id"), result);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

}
