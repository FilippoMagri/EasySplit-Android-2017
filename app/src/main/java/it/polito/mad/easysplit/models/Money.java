package it.polito.mad.easysplit.models;

import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class Money {
    private static Currency defaultCurrency = Currency.getInstance("EUR");

    private static List<Currency> currencies = null;

    @NonNull
    public static List<Currency> getCurrencies() {
        if (currencies == null) {
            final String[] codes = {
                    "EUR", "USD", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
                    "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR",
            };

            currencies = new ArrayList<>();
            for (String code : codes)
                currencies.add(Currency.getInstance(code));
        }
        return currencies;
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
    public static Money zero(Currency currency) {
        return new Money(currency, BigDecimal.ZERO);
    }
    public static Money zeroLike(Money other) {
        return new Money(other.getCurrency(), BigDecimal.ZERO);
    }

    private static DecimalFormat sStdFormat = new DecimalFormat("0.00");
    private static DecimalFormat sLocaleFormat = new DecimalFormat("####,###,##0.00");
    static {
        sStdFormat.setParseBigDecimal(true);
        sLocaleFormat.setParseBigDecimal(true);
    }

    public static void setLocale(Locale locale) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        sLocaleFormat.setDecimalFormatSymbols(symbols);
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
