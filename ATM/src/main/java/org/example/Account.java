package org.example;

public class Account {
    String Username;
    private String cardId;
    private char sex;
    private String passWord;
    private double money;
    private double limit; // 限额

    public String getUsername() {
        return Username + (sex == '男' ? "先生" : "女士");
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public char getSex() {
        return sex;
    }

    public void setSex(char sex) {
        this.sex = sex;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public double getMoney() {
        return this.money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }
}
