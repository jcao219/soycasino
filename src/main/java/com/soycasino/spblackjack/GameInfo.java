package com.soycasino.spblackjack;

public class GameInfo {
    public Double balance;
    public String gameState = "init";
    public int dealt;
    public String dealer1;
    public String player1;
    public String player2;
    public Integer player;
    public Integer dealer;

    GameInfo(double balance, String gameState) { 
        this.balance = balance;
    }

    public GameInfo() {
        balance = null;
    }
}
