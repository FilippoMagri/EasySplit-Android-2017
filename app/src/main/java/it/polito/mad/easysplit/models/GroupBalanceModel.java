package it.polito.mad.easysplit.models;

import android.database.DataSetObserver;

import java.util.HashMap;

public class GroupBalanceModel extends ObservableBase {
    private GroupModel group;

    public GroupModel getGroup() {
        return group;
    }

    private static class Balance {
        public Money credit = new Money(0);
        public Money debit = new Money(0);

        public void add(Money amount) {
            if (amount.getCents() > 0)
                credit = credit.add(amount);
            if (amount.getCents() < 0)
                debit = debit.add(amount.neg());
        }

        public Money getResidue() {
            return credit.sub(debit);
        }
    }

    private HashMap<PersonModel, Balance> balance = new HashMap<>();

    public GroupBalanceModel(GroupModel group) {
        this.group = group;
        this.group.registerDataSetObserver(new Observer());
        recompute();
    }

    private class Observer extends DataSetObserver {
        @Override
        public void onChanged() {
            recompute();
        }

        @Override
        public void onInvalidated() {
            notifyInvalidated();
        }
    }

    private void recompute() {
        int numPeople = group.getMembers().size();
        if (numPeople == 0)
            return;

        for (PersonModel person : this.group.getMembers())
            balance.put(person, new Balance());

        if (numPeople == 1)
            return;

        for (ExpenseModel exp : this.group.getExpenses()) {
            PersonModel creditor = exp.getPayer();
            Money quota = exp.getAmount().div(numPeople);

            for (PersonModel person : this.group.getMembers()) {
                if (person == creditor)
                    balance.get(person).add(quota.mul(numPeople - 1));
                else
                    balance.get(person).add(quota.neg());
            }
        }

        notifyChanged();
    }

    public Money getCreditFor(PersonModel person) {
        return balance.get(person).credit;
    }

    public Money getDebitFor(PersonModel person) {
        return balance.get(person).debit;
    }

    public Money getResidueFor(PersonModel person) {
        return balance.get(person).getResidue();
    }

}