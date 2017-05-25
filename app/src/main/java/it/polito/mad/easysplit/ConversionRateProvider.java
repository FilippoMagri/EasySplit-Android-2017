package it.polito.mad.easysplit;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.polito.mad.easysplit.models.Money;

public class ConversionRateProvider {
    private static ConversionRateProvider sInstance = null;
    public static synchronized void setupInstance(Context ctx) {
        sInstance = new ConversionRateProvider(ctx);
    }
    public static synchronized ConversionRateProvider getInstance() {
        return sInstance;
    }

    private static final Currency mBaseCurrency = Currency.getInstance("EUR");
    public static Currency getBaseCurrency() {
        return mBaseCurrency;
    }

    private RequestQueue mQueue;
    private Cache mCache = new Cache();

    private ConversionRateProvider(Context ctx) {
        mQueue = Volley.newRequestQueue(ctx);
        mQueue.start();
    }

    public Task<Money> convertToBase(final Money money) {
        if (money.getCurrency().equals(mBaseCurrency))
            return Tasks.forResult(money);

        String currencyCode = money.getCurrency().getCurrencyCode();
        return mCache.fetchOrUpdate(currencyCode).continueWith(new Continuation<BigDecimal, Money>() {
            @Override
            public Money then(@NonNull Task<BigDecimal> task) throws Exception {
                if (task.getException() != null)
                    throw task.getException();
                BigDecimal rate = task.getResult();
                BigDecimal amount = money.getAmount().divide(rate, RoundingMode.HALF_UP);
                return new Money(mBaseCurrency, amount);
            }
        });
    }

    public Task<Money> convertFromBase(final Money money, final Currency currency) {
        if (! money.getCurrency().equals(mBaseCurrency))
            throw new IllegalArgumentException(
                    "Argument `money` must be in the base currency ("
                    +mBaseCurrency.getCurrencyCode()+")");

        if (currency.equals(mBaseCurrency))
            return Tasks.forResult(money);

        String currencyCode = currency.getCurrencyCode();

        return mCache.fetchOrUpdate(currencyCode).continueWith(new Continuation<BigDecimal, Money>() {
            @Override
            public Money then(@NonNull Task<BigDecimal> task) throws Exception {
                if (task.getException() != null)
                    throw task.getException();
                BigDecimal rate = task.getResult();
                BigDecimal amount = money.getAmount().multiply(rate);
                return new Money(currency, amount);
            }
        });
    }

    private static final long CACHE_DURATION_MSECS = TimeUnit.HOURS.convert(12, TimeUnit.MILLISECONDS);

    private class Cache {
        private Map<String, BigDecimal> mConversionRates = new HashMap<>();
        private long mLastUpdateTime = 0;

        Task<BigDecimal> fetchOrUpdate(final String key) {
            BigDecimal rate = mConversionRates.get(key);

            long now = System.currentTimeMillis();
            if (rate != null && now - mLastUpdateTime < CACHE_DURATION_MSECS)
                return Tasks.forResult(rate);

            return update().continueWith(new Continuation<Void, BigDecimal>() {
                @Override
                public BigDecimal then(@NonNull Task<Void> task) throws Exception {
                    if (task.getException() != null)
                        throw task.getException();

                    // may be null if the service does not know the particular currency!
                    return mConversionRates.get(key);
                }
            });
        }

        private String getApiUrl() {
            return "https://api.fixer.io/latest?base=" + mBaseCurrency.getCurrencyCode();
        }

        Task<Void> update() {
            final TaskCompletionSource<Void> completion = new TaskCompletionSource<>();

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONObject rates = response.getJSONObject("rates");
                        Iterator<String> keys = rates.keys();
                        while (keys.hasNext()) {
                            String currencyCode = keys.next();
                            BigDecimal rate = BigDecimal.valueOf(rates.getDouble(currencyCode));
                            mConversionRates.put(currencyCode, rate);
                        }

                        mLastUpdateTime = System.currentTimeMillis();
                        completion.setResult(null);
                    } catch(Exception exc) {
                        completion.setException(exc);
                    }
                }
            };


            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    completion.setException(error);
                }
            };

            JsonObjectRequest request =
                    new JsonObjectRequest(Request.Method.GET, getApiUrl(), null, listener, errorListener);
            mQueue.add(request);

            return completion.getTask();
        }
    }
}