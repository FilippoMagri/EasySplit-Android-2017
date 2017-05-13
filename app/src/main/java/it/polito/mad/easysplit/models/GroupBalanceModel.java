package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.polito.mad.easysplit.Utils;

public class GroupBalanceModel {

    public interface Listener {
        void onBalanceChanged(Map<String, MemberRepresentation> balance);
    }

    // There's a small leak of Uri objects... If this gets critical, it might be a good idea to
    // switch to using group IDs as String keys
    private static final HashMap<Uri, WeakReference<GroupBalanceModel>> sInstances = new HashMap<>();
    public static GroupBalanceModel forGroup(Uri groupUri) {
        WeakReference<GroupBalanceModel> instance = sInstances.get(groupUri);

        if (instance == null || instance.get() == null) {
            GroupBalanceModel newInstance = new GroupBalanceModel(groupUri);
            sInstances.put(groupUri, new WeakReference<>(newInstance));
            return newInstance;
        }

        return instance.get();
    }


    private final Map<String, MemberRepresentation> mBalance = new HashMap<>();
    private final ArrayList<Listener> mListeners = new ArrayList<>();

    private GroupBalanceModel(Uri groupUri) {
        DatabaseReference groupRef = Utils.findByUri(groupUri);

        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                updateBalance(groupSnap.child("expenses"));
                decideWhoHasToGiveBackTo();
                notifyListeners();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void notifyListeners() {
        for (Listener listener : new ArrayList<>(mListeners))
            listener.onBalanceChanged(mBalance);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
        listener.onBalanceChanged(mBalance);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public synchronized Map<String, MemberRepresentation> getBalanceSnapshot() {
        return new HashMap<>(mBalance);
    }

    private synchronized void resetBalance(@NonNull DataSnapshot expensesSnap) {
        mBalance.clear();

        for (DataSnapshot expense : expensesSnap.getChildren()) {
            expense.child("members_ids").getChildren();
            for (DataSnapshot member : expense.child("members_ids").getChildren()) {
                String memberId = member.getKey();
                if (! mBalance.containsKey(memberId)) {
                    String memberName = member.getValue(String.class);
                    mBalance.put(memberId, new MemberRepresentation(memberId, memberName));
                }
            }

            // Also add the payers, as they may be "financiers" (they have credit but no debit)
            String payerId = expense.child("payer_id").getValue(String.class);
            if (! mBalance.containsKey(payerId)) {
                String payerName = expense.child("payer_name").getValue(String.class);
                mBalance.put(payerId, new MemberRepresentation(payerId, payerName));
            }
        }
    }

    private synchronized void updateBalance(DataSnapshot expensesSnap) {
        resetBalance(expensesSnap);

        for (DataSnapshot expense : expensesSnap.getChildren()) {
            String payerId = expense.child("payer_id").getValue(String.class);
            String amountStr = expense.child("amount").getValue(String.class);
            Money amount = Money.parseOrFail(amountStr);

            DataSnapshot membersIds = expense.child("members_ids");
            long numMembers = membersIds.getChildrenCount();
            if (numMembers == 0)
                continue;

            Money quote = amount.div(numMembers).neg();

            for (DataSnapshot member : membersIds.getChildren()) {
                String memberId = member.getKey();
                MemberRepresentation memberRepr = mBalance.get(memberId);

                memberRepr.residue = memberRepr.residue.add(quote);
            }

            MemberRepresentation payer = mBalance.get(payerId);
            payer.residue = payer.residue.add(amount);

            distributeRest(amount, numMembers, payerId);
        }

        decideWhoHasToGiveBackTo();
    }

    /// TODO Deduplicate with ExpenseDetailsActivity.distributeRest or clarify difference
    private void distributeRest(Money amount, long numMembers, String payerId) {
        Money quote = amount.div(numMembers);
        Money totalAmountCalculated = quote.mul(numMembers);

        if (totalAmountCalculated.compareTo(amount) == 0)
            return;

        Money rest = amount.sub(totalAmountCalculated);

        // TODO Clarify following comment ("expense" used to be "singleExpense")
        // TODO Make the distribution onSingleExpense

        BigDecimal d = rest.getAmount();
        int numberOfIteration = d.subtract(d.setScale(0, RoundingMode.HALF_UP))
                .movePointRight(d.scale())
                .abs().intValue();

        for (Entry<String, MemberRepresentation> entry : mBalance.entrySet()) {
            String memberId = entry.getKey();
            MemberRepresentation member = entry.getValue();
            if (memberId.equals(payerId))
                continue;

            int cmp = rest.getAmount().compareTo(BigDecimal.ZERO);
            if (cmp > 0)
                member.residue = member.getResidue().add(new Money(new BigDecimal("-0.01")));
            else if (cmp < 0)
                member.residue = member.getResidue().add(new Money(new BigDecimal("+0.01")));

            numberOfIteration--;
            if (numberOfIteration == 0)
                break;
        }
    }

    private void resetAssignments() {
        for (MemberRepresentation member : mBalance.values())
            member.resetAssignments();
    }

    private void decideWhoHasToGiveBackTo() {
        resetAssignments();

        // Keeps track of how much debit is left for each debtor
        Map<MemberRepresentation, Money> availableDebit = new HashMap<>();
        List<MemberRepresentation> creditors = new ArrayList<>();

        for (MemberRepresentation member : mBalance.values()) {
            if (member.getResidue().cmpZero() > 0)
                creditors.add(member);
            else
                availableDebit.put(member, member.getResidue());
        }

        for (MemberRepresentation creditor : creditors) {
            // unassignedDebit is always negative
            Money unassignedDebit = creditor.getResidue().neg();

            for (MemberRepresentation debtor : availableDebit.keySet()) {
                // debtorResidue is always negative
                Money debtorResidue = availableDebit.get(debtor);

                if (debtorResidue.compareTo(unassignedDebit) < 0) {
                    // I have to split the residue of the debtor
                    creditor.assign(debtor, unassignedDebit.neg());
                    debtor.assign(creditor, unassignedDebit);

                    unassignedDebit = Money.zeroLike(unassignedDebit);
                    availableDebit.put(debtor, debtorResidue.sub(unassignedDebit));
                } else {
                    creditor.assign(debtor, debtorResidue.neg());
                    debtor.assign(creditor, debtorResidue);

                    unassignedDebit = unassignedDebit.sub(debtorResidue);
                    availableDebit.put(debtor, Money.zeroLike(debtorResidue));
                }
            }
        }
    }

    public class MemberRepresentation {
        private String id;
        private String name;
        private Money residue;
        private Map<MemberRepresentation, Money> assignments = new HashMap<>();

        MemberRepresentation(String id, String name) {
            this(id, name, Money.zero());
        }

        MemberRepresentation(String id, String name, Money residue) {
            this.id = id;
            this.name = name;
            this.residue = residue;
        }

        public String getName() {
            return name;
        }
        void setName(String name) {
            this.name = name;
        }

        public Money getResidue() {
            return residue;
        }

        public String getId() {
            return id;
        }
        void setId(String id) {
            this.id = id;
        }

        public Map<MemberRepresentation, Money> getAssignments() {
            return Collections.unmodifiableMap(assignments);
        }

        void resetAssignments() {
            assignments.clear();
        }

        void assign(MemberRepresentation to, Money amount) {
            Money current = assignments.get(to);
            if (current == null)
                current = Money.zeroLike(amount);
            current = current.add(amount);
            assignments.put(to, current);
        }
    }
}
