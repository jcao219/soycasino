package com.soycasino.json;

public class CreateAccount {

    private String type;
    private String nickname;
    private String acc_num;
    private int rewards;
    private int balance;

    public CreateAccount(String type, String nickname, int rewards, int balance, String acc_num) {
        this.type = type;
        this.nickname = nickname;
        this.rewards = rewards;
        this.balance = balance;
        this.acc_num = acc_num;
    }
}
