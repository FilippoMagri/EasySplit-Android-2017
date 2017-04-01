package it.polito.mad.easysplit.models;

import java.util.Currency;

public class Money {
    private Currency currency;
    private long cents;

    public Money(Currency currency, long cents) {
        this.currency = currency;
        this.cents = cents;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long getCents() {
        return cents;
    }
}
