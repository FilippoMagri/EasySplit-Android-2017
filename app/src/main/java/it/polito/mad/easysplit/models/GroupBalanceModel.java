package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import it.polito.mad.easysplit.ConversionRateProvider;
import it.polito.mad.easysplit.Utils;

public class GroupBalanceModel {
    private static final String TAG = "GroupBalanceModel";

    public LinkedHashMap<String, MemberRepresentation> getmBalance() {
        return mBalance;
    }

    public interface Listener {
        void onBalanceChanged(Map<String, MemberRepresentation> balance);
    }

    // There's a small leak of Uri objects... If this gets critical, it might be a good idea to
    // switch to using group IDs as String keys
    private static final HashMap<Uri, WeakReference<GroupBalanceModel>> sInstances = new HashMap<>();
    private static final ConcurrentHashMap<Uri, WeakReference<GroupBalanceModel>> sInstancesWithSpecificCurrencyCode = new ConcurrentHashMap<>();

    public static GroupBalanceModel forGroup(Uri groupUri) {
        WeakReference<GroupBalanceModel> instance = sInstances.get(groupUri);

        if (instance == null || instance.get() == null) {
            GroupBalanceModel newInstance = new GroupBalanceModel(groupUri);
            sInstances.put(groupUri, new WeakReference<>(newInstance));
            return newInstance;
        }

        return instance.get();
    }

    public static GroupBalanceModel forGroup(Uri groupUri,String currencyCode) {
        WeakReference<GroupBalanceModel> instance = sInstancesWithSpecificCurrencyCode.get(groupUri);

        if (instance == null || instance.get() == null || ( (instance!=null) && !instance.get().getmGroupCurrency().getCurrencyCode().equals(currencyCode) )) {
            GroupBalanceModel newInstance = new GroupBalanceModel(groupUri, currencyCode);
            sInstancesWithSpecificCurrencyCode.put(groupUri, new WeakReference<>(newInstance));
            return newInstance;
        }

        return instance.get();
    }

    private final LinkedHashMap<String, MemberRepresentation> mBalance = new LinkedHashMap<>();
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private Currency mGroupCurrency = ConversionRateProvider.getBaseCurrency();
    private String mGroupId="";

    private GroupBalanceModel(Uri groupUri) {
        DatabaseReference groupRef = Utils.findByUri(groupUri);
        mGroupId = Utils.getIdFor(Utils.UriType.GROUP,groupUri);

        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                String currencyCode = groupSnap.child("currency").getValue(String.class);
                if (currencyCode != null)
                    mGroupCurrency = Currency.getInstance(currencyCode);
                else
                    mGroupCurrency = ConversionRateProvider.getBaseCurrency();

                updateBalance(groupSnap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });
    }

    private GroupBalanceModel(Uri groupUri,final String currencyCode) {
        DatabaseReference groupRef = Utils.findByUri(groupUri);
        mGroupId = Utils.getIdFor(Utils.UriType.GROUP,groupUri);

        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                if (currencyCode != null)
                    mGroupCurrency = Currency.getInstance(currencyCode);
                else
                    mGroupCurrency = ConversionRateProvider.getBaseCurrency();

                updateBalance(groupSnap);
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

    private void addMember(String memberId, String memberName) {
        if (mBalance.containsKey(memberId))
            return;
        mBalance.put(memberId, new MemberRepresentation(memberId, memberName, mGroupId));
    }

    private synchronized void resetBalance(@NonNull DataSnapshot groupSnap) {
        mBalance.clear();

        // There is a slight difference between getting the members of the group
        // and the union of the members of all expenses: it's possible that a
        // person is part of the group but not a member of any expense (i.e. hasn't
        // participated in any expense yet), and viceversa it's possible that a person
        // is part of one or more expenses but not of the group (e.g. has left the group
        // at some point)

        for (DataSnapshot member : groupSnap.child("members_ids").getChildren())
            addMember(member.getKey(), member.getValue(String.class));

        for (DataSnapshot expense : groupSnap.child("expenses").getChildren()) {
            for (DataSnapshot member : expense.child("members_ids").getChildren())
                addMember(member.getKey(), member.getValue(String.class));

            // Also add the payer, as he/she may be a "financier" (they have credit but no debit)
            String payerId = expense.child("payer_id").getValue(String.class);
            String payerName = expense.child("payer_name").getValue(String.class);
            addMember(payerId, payerName);
        }
    }

    private synchronized void updateBalance(DataSnapshot groupSnap) {
        resetBalance(groupSnap);

        for (DataSnapshot expense : groupSnap.child("expenses").getChildren()) {
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

        consideringPayments(groupSnap);

        decideWhoHasToGiveBackTo();
        convertToGroupCurrency().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                notifyListeners();
            }
        });
    }

    private void consideringPayments(DataSnapshot groupSnap) {
        if (groupSnap.hasChild("payments")) {
            for (DataSnapshot expense : groupSnap.child("payments").getChildren()) {
                String payerId = expense.child("payer_id").getValue(String.class);
                String amountStr = expense.child("amount").getValue(String.class);
                Money amount = Money.parseOrFail(amountStr);

                DataSnapshot membersIds = expense.child("members_ids");
                long numMembers = membersIds.getChildrenCount();
                if (numMembers == 0)
                    continue;
                // Actually this cycle is performed only once
                // Because of the logical structure of a payment , with only one member inside
                // members_ids
                for (DataSnapshot member : membersIds.getChildren()) {
                    String memberId = member.getKey();
                    // Update the balance of the receiver with a negative amount
                    MemberRepresentation memberReprReceiver = mBalance.get(memberId);
                    memberReprReceiver.residue = memberReprReceiver.residue.add(amount);
                    // Update the balance of the payer with a positive amount
                    MemberRepresentation memberReprPayer = mBalance.get(payerId);
                    memberReprPayer.residue = memberReprPayer.residue.add(amount.neg());
                }
            }
        }
    }

    /// TODO Deduplicate with ExpenseDetailsActivity.distributeRest or clarify difference
    private void distributeRest(Money amount, long numMembers, String payerId) {
        Money quote = amount.div(numMembers);
        Money totalAmountCalculated = quote.mul(numMembers);
        if (totalAmountCalculated.compareTo(amount) == 0)
            return;

        Money rest = amount.sub(totalAmountCalculated);
        BigDecimal d = rest.getAmount();
        int numberOfIteration = d.subtract(d.setScale(0, RoundingMode.HALF_UP))
                .movePointRight(d.scale())
                .abs().intValue();

        for (Entry<String, MemberRepresentation> entry : mBalance.entrySet()) {
            String memberId = entry.getKey();
            MemberRepresentation member = entry.getValue();
            if (memberId.equals(payerId)) {
                continue;
            }

            int cmp = rest.getAmount().compareTo(BigDecimal.ZERO);
            if (member.getResidue().cmpZero()!=0) {
                if (cmp > 0)
                    member.residue = member.getResidue().add(new Money(new BigDecimal("-0.01")));
                else if (cmp < 0)
                    member.residue = member.getResidue().add(new Money(new BigDecimal("+0.01")));
            } else continue;
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
        ConcurrentHashMap<MemberRepresentation, Money> availableDebit = new ConcurrentHashMap<>();
        List<MemberRepresentation> creditors = new ArrayList<>();
        //Available Debit contains all the list of debtors
        for (MemberRepresentation member : mBalance.values()) {
            if (member.getResidue().cmpZero() > 0)
                creditors.add(member);
            else if (member.getResidue().cmpZero() < 0)
                availableDebit.put(member, member.getResidue());
        }

        for (MemberRepresentation creditor : creditors) {
            // unassignedDebit is always negative
            Money unassignedDebit = creditor.getResidue().neg();
            while (!unassignedDebit.isZero()) {
                for (MemberRepresentation debtor : availableDebit.keySet()) {
                    // debtorResidue is always negative
                    Money debtorResidue = availableDebit.get(debtor);

                    if (debtorResidue.compareTo(unassignedDebit) < 0) {
                        // Case in which the debtor has to own more money (In the global Balance) respect the money that the this single creditor has to receive
                        // I have to split the residue of the debtor
                        // Assign to the creditor , this debtor with a positive number
                        creditor.assign(debtor, unassignedDebit.neg());
                        // Assign to the debtor , this creditor with a negative number
                        debtor.assign(creditor, unassignedDebit);
                        availableDebit.put(debtor, debtorResidue.sub(unassignedDebit));
                        unassignedDebit = Money.zeroLike(unassignedDebit);
                        break;
                    } else if (debtorResidue.compareTo(unassignedDebit) >= 0) {
                        // Case in which the debtor cover totally the amount that the creditor has to receive
                        creditor.assign(debtor, debtorResidue.neg());
                        debtor.assign(creditor, debtorResidue);
                        unassignedDebit = unassignedDebit.sub(debtorResidue);
                        // Here i directly delete the debtor from availableDebit by using ConcurrentHashMap
                        // Other wise we could encounter into a Concurrent exception.

                        // P.s. : If for some reason we don't wanna use ConcurrentHashMap.remove() we can always use the instruction written before
                        // i.e. "availableDebit.put(debtor, Money.zeroLike(debtorResidue));" , but in that case we had some rows with 0.00
                        // on the SubElements into GroupBalance Visualization. And by the way , i fixed also that problem because i've used a filter
                        // before the visualization into GroupBalance Screen.
                        // So right now , if we wanna change only this instruction we can.
                        availableDebit.remove(debtor);
                    }
                }
            }
        }
    }

    /** Do all of the necessary conversions to the group currency */
    private Task<Void> convertToGroupCurrency() {
        final ConversionRateProvider converter = ConversionRateProvider.getInstance();
        final TaskCompletionSource completion = new TaskCompletionSource();

        new Thread() {
            @Override
            public void run() {
                synchronized (GroupBalanceModel.this) {
                    try {
                        for (final MemberRepresentation member : mBalance.values()) {
                            member.convertedResidue = Tasks.await(converter.convertFromBase(member.residue, mGroupCurrency));
                            member.convertedAssignments.clear();
                            for (final MemberRepresentation otherMember : member.assignments.keySet()) {
                                Money amount = member.assignments.get(otherMember);
                                Money convertedAmount = Tasks.await(converter.convertFromBase(amount, mGroupCurrency));
                                member.convertedAssignments.put(otherMember, convertedAmount);
                            }
                        }
                        completion.setResult(null);
                    } catch (InterruptedException | ExecutionException exc) {
                        completion.setException(exc);
                    }
                }
            }
        }.start();

        return completion.getTask();
    }

    public Currency getmGroupCurrency() {
        return mGroupCurrency;
    }

    public class MemberRepresentation {
        private String id;
        private String name;
        private Money residue, convertedResidue;
        private Map<MemberRepresentation, Money> assignments = new HashMap<>();
        private Map<MemberRepresentation, Money> convertedAssignments = new HashMap<>();
        private String groupId;

        MemberRepresentation(String id, String name, String groupId) {
            this(id, name, Money.zero(),groupId);
        }

        MemberRepresentation(String id, String name, Money residue, String groupId) {
            this.id = id;
            this.name = name;
            this.residue = residue;
            this.groupId = groupId;
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
        public void setResidue(Money residue) {
            this.residue = residue;
        }

        public Money getConvertedResidue() {
            return convertedResidue;
        }
        public void setConvertedResidue(Money convertedResidue) {
            this.convertedResidue = convertedResidue;
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

        public Map<MemberRepresentation, Money> getConvertedAssignments() {
            return convertedAssignments;
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

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }
}
