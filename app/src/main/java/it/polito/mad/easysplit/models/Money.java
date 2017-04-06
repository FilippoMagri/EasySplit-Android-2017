package it.polito.mad.easysplit.models;

import java.util.Currency;
import java.util.Locale;

public class Money {
    private Currency currency;
    private long cents;

    private static Currency defaultCurrency = Currency.getInstance("EUR");

    public static Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public static void setDefaultCurrency(Currency defaultCurrency) {
        Money.defaultCurrency = defaultCurrency;
    }

    public Money(long cents) {
        this(defaultCurrency, cents);
    }

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

    public Money add(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.cents + rhs.cents);
    }

    public Money sub(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.cents - rhs.cents);
    }

    public Money div(int denom) {
        return new Money(this.getCurrency(), this.cents / denom);
    }

    public Money neg() {
        return new Money(this.getCurrency(), - this.cents);
    }

    public Money mul(int factor) {
        return new Money(this.getCurrency(), this.cents * factor);
    }

    @Override
    public String toString() {
        long fractionalDenom = (long) Math.pow(10, currency.getDefaultFractionDigits());
        long integ = cents / fractionalDenom;
        // `integ` already has the right sign
        long frac = Math.abs(cents % fractionalDenom);
        return String.format(Locale.getDefault(), "%+d.%02d %s", integ, frac, currency.getSymbol());
    }
}
