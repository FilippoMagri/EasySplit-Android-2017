package it.polito.mad.easysplit.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Currency;

public class Money {
    private static Currency defaultCurrency = Currency.getInstance("EUR");

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
    public static Money zero(Currency currency) {
        return new Money(currency, BigDecimal.ZERO);
    }
    public static Money zeroLike(Money other) {
        return new Money(other.getCurrency(), BigDecimal.ZERO);
    }

    private Currency mCurrency;
    private BigDecimal mAmount;

    public Money(BigDecimal amount) {
        this(defaultCurrency, amount);
    }

    public Money(Currency currency, BigDecimal amount) {
        this.mCurrency = currency;
        this.mAmount = amount;
    }

    public Currency getCurrency() {
        return mCurrency;
    }
    public BigDecimal getAmount() {
        return mAmount;
    }

    public boolean isZero() {
        return cmpZero() == 0;
    }
    public int cmpZero() {
        return getAmount().compareTo(BigDecimal.ZERO);
    }

    public int compareTo(Money other) {
        if (! other.getCurrency().equals(other.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return mAmount.compareTo(other.mAmount);
    }

    public Money abs() {
        return new Money(getCurrency(), mAmount.abs());
    }

    public Money add(BigDecimal amount) {
        return new Money(getCurrency(), mAmount.add(amount));
    }

    public Money add(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.mAmount.add(rhs.mAmount));
    }

    public Money sub(BigDecimal amount) {
        return new Money(getCurrency(), mAmount.subtract(amount));
    }

    public Money sub(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.mAmount.subtract(rhs.mAmount));
    }

    public Money div(BigDecimal denom) {
        return new Money(this.getCurrency(), this.mAmount.divide(denom, 2, RoundingMode.HALF_UP));
    }

    public Money div(long denom) {
        return div(BigDecimal.valueOf(denom));
    }

    public Money neg() {
        return new Money(this.getCurrency(), this.mAmount.negate());
    }

    public Money mul(BigDecimal factor) {
        return new Money(this.getCurrency(), this.mAmount.multiply(factor));
    }

    public Money mul(long factor) {
        return mul(BigDecimal.valueOf(factor));
    }

    private static DecimalFormat sStdFormat = new DecimalFormat("0.00");
    private static DecimalFormat sLocaleFormat = new DecimalFormat("####,###,##0.00");
    static {
        sStdFormat.setParseBigDecimal(true);
        sLocaleFormat.setParseBigDecimal(true);
    }

    @Override
    public String toString() {
        return sLocaleFormat.format(mAmount) + " " + mCurrency.getSymbol();
    }

    public String toStandardFormat() {
        return sStdFormat.format(mAmount) + " " + mCurrency.getCurrencyCode();
    }

    public static Money parseOrFail(String text) {
        try {
            return parse(text);
        } catch (ParseException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static Money parse(String text) throws ParseException {
        String[] toks = text.split("\\s+");
        BigDecimal amount = (BigDecimal) sStdFormat.parseObject(toks[0]);
        String currencyCode = toks[1];
        return new Money(Currency.getInstance(currencyCode), amount);
    }
}
