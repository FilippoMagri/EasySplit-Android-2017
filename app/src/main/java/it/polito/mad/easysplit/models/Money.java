package it.polito.mad.easysplit.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Scanner;

public class Money {
    private Currency currency;
    private BigDecimal amount;

    private static Currency defaultCurrency = Currency.getInstance("EUR");

    public static Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public static void setDefaultCurrency(Currency defaultCurrency) {
        Money.defaultCurrency = defaultCurrency;
    }

    public Money(BigDecimal amount) {
        this(defaultCurrency, amount);
    }

    public Money(Currency currency, BigDecimal amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money add(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.amount.add(rhs.amount));
    }

    public Money sub(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.amount.subtract(rhs.amount));
    }

    public Money div(BigDecimal denom) {
        return new Money(this.getCurrency(), this.amount.divide(denom, 2, RoundingMode.HALF_UP));
    }

    public Money neg() {
        return new Money(this.getCurrency(), this.amount.negate());
    }

    public Money mul(BigDecimal factor) {
        return new Money(this.getCurrency(), this.amount.multiply(factor));
    }

    @Override
    public String toString() {
        return amount.toString() + " " + currency.getCurrencyCode();
    }

    public static Money parse(String text) {
        Scanner scanner = new Scanner(text);
        BigDecimal cents = scanner.nextBigDecimal();
        if (scanner.hasNext()) {
            String currencyCode = scanner.next();
            return new Money(Currency.getInstance(currencyCode), cents);
        } else {
            return new Money(cents);
        }
    }
}
