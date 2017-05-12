package it.polito.mad.easysplit.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Currency;

public class Money {
    private static Currency defaultCurrency = Currency.getInstance("EUR");

    public static Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public static void setDefaultCurrency(Currency defaultCurrency) {
        Money.defaultCurrency = defaultCurrency;
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

    public Money add(Money rhs) {
        /// TODO Handle different currencies
        if (! rhs.getCurrency().equals(this.getCurrency()))
            throw new AssertionError("Multiple currencies aren't yet implemented");

        return new Money(this.getCurrency(), this.mAmount.add(rhs.mAmount));
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

    public Money neg() {
        return new Money(this.getCurrency(), this.mAmount.negate());
    }

    public Money mul(BigDecimal factor) {
        return new Money(this.getCurrency(), this.mAmount.multiply(factor));
    }

    private static DecimalFormat sStdFormat = new DecimalFormat("#.00");
    private static DecimalFormat sLocaleFormat = new DecimalFormat("####,###,###.00");
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
