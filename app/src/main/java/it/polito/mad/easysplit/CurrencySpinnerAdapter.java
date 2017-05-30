package it.polito.mad.easysplit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Currency;

import it.polito.mad.easysplit.models.Money;

public class CurrencySpinnerAdapter extends ArrayAdapter<Currency> {
    public CurrencySpinnerAdapter(@NonNull Context context) {
        super(context, R.layout.spinner_item_currency);
        addAll(Money.getCurrencies());
    }

    public int findCurrencyByCode(String code) {
        final int numCurrencies = getCount();
        for (int i=0; i < numCurrencies; i++) {
            Currency curr = getItem(i);
            if (curr.getCurrencyCode().equals(code))
                return i;
        }

        return -1;
    }

    public void setOnlyGroupCurrency (String groupCurrency) {
        this.clear();
        Currency currency = Currency.getInstance(groupCurrency);
        add(currency);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.spinner_item_currency, parent, false);

        Currency curr = getItem(position);
        if (curr != null) {
            TextView textView = (TextView) convertView;
            textView.setText(curr.getSymbol());
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dropdown_item_currency, parent, false);

        Currency curr = getItem(position);

        if (curr != null) {
            TextView symbolText = (TextView) convertView.findViewById(R.id.currencySymbolText);
            TextView codeText = (TextView) convertView.findViewById(R.id.currencyCodeText);

            symbolText.setText(curr.getSymbol());
            codeText.setText(curr.getCurrencyCode());
        }

        return convertView;
    }
}
