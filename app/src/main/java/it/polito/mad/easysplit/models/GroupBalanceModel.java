package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.GroupHandler;
import it.polito.mad.easysplit.layout.GroupBalanceAdapter;

/**
 * Created by fil on 03/05/17.
 */

public class GroupBalanceModel {
    static String TAG="GroupBalanceModel";
    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    String thisGroupId = "";
    private Map<String,Object> expensesOfThisGroup = new HashMap<String,Object>();
    private Map<String,Object> usersOfThisGroup = new HashMap<String,Object>();
    Uri groupUri;

    // membersBalanceInvolved
    // is the HashMap created specially to compute the balance among a single group
    private final HashMap<String,MemberRepresentation> membersBalanceInvolved = new HashMap<String,MemberRepresentation>();

    // The parameter groupBalanceAdapter in the constructor in necessary because we use it at the end of
    // computation in order to update the listItems inside the ListView
    // (And of course the listView is relative to the Fragment: "MemberListFragment")

    public GroupBalanceModel(final Uri mGroupUri, final GroupBalanceAdapter groupBalanceAdapter) {
        thisGroupId = mGroupUri.toString().replace("content://it.polito.mad.easysplit/groups/","");
        Log.d(TAG,mGroupUri.toString());
        if (groupBalanceAdapter != null) {
            DatabaseReference groupRef = mRoot.child("groups").child(thisGroupId).getRef();
            groupRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Map<String, Object> entireGroupDB = new HashMap<String, Object>();
                    entireGroupDB = (Map<String, Object>) dataSnapshot.getValue();
                    retrieveUsersOfThisGroup((Map<String, Object>) entireGroupDB.get("members_ids"));
                    retrieveExpensesOfThisGroup((Map<String, Object>) entireGroupDB.get("expenses"));
                    computeBalance();
                    printDebugBalances();
                    updateListItemsOnFragment(groupBalanceAdapter);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public interface UserBalanceListener {
        void onBalanceAvailable(Money money);
    }

    public void getUserBalance (final String user, final UserBalanceListener listener) {
        DatabaseReference groupRef = mRoot.child("groups").child(thisGroupId).getRef();
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> entireGroupDB = new HashMap<String, Object>();
                entireGroupDB = (Map<String, Object>) dataSnapshot.getValue();
                retrieveUsersOfThisGroup((Map<String, Object>) entireGroupDB.get("members_ids"));
                retrieveExpensesOfThisGroup((Map<String, Object>) entireGroupDB.get("expenses"));
                computeBalance();
                if (membersBalanceInvolved.containsKey(user)) {
                    listener.onBalanceAvailable(membersBalanceInvolved.get(user).getResidue());
                }
                else {
                    listener.onBalanceAvailable(new Money(new BigDecimal("0.00")));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void retrieveExpensesOfThisGroup(Map<String, Object> mapOfAllExpenses) {
        if (mapOfAllExpenses!=null) {
            for (Map.Entry<String, Object> entry : mapOfAllExpenses.entrySet()) {
                Map singleExpense = (Map) entry.getValue();
                Map<String, Object> involved_balance_members_ids = (Map<String, Object>) singleExpense.get("members_ids");
                for (Map.Entry<String, Object> entry_nested : involved_balance_members_ids.entrySet()) {
                    String involved_balance_member_key = entry_nested.getKey();
                    String involved_balance_member_name = entry_nested.getValue().toString();
                    Money money = new Money(new BigDecimal("0.00"));
                    membersBalanceInvolved.put(involved_balance_member_key, new MemberRepresentation(involved_balance_member_name, money));
                }
            }
            expensesOfThisGroup = mapOfAllExpenses;
        }
    }

    private void retrieveUsersOfThisGroup(Map<String, Object> mapOfAllUsers) {
        usersOfThisGroup = mapOfAllUsers;
    }

    private void updateListItemsOnFragment(GroupBalanceAdapter groupBalanceAdapter) {
        groupBalanceAdapter.clear();
        for (Map.Entry<String,MemberRepresentation> entry : membersBalanceInvolved.entrySet()) {
            MemberRepresentation member = (MemberRepresentation) entry.getValue();
            Money residue = member.getResidue();
            String name = member.getName();
            String idMember = entry.getKey();
            String memberToGiveBack = member.getMemberToGiveBack();
            GroupBalanceAdapter.ListItem listItem = new GroupBalanceAdapter.ListItem(idMember,name,residue,memberToGiveBack);
            groupBalanceAdapter.add(listItem);
        }
    }

    private void printDebugBalances() {
        for (Map.Entry<String,MemberRepresentation> entry :membersBalanceInvolved.entrySet()) {
            MemberRepresentation member = entry.getValue();
            Log.d(TAG,"Name: "+member.getName().toString()+"Residue: "+member.getResidue().toString());
        }
    }

    private void computeBalance() {
        //Log.d(TAG,"MembersInvolved:"+membersBalanceInvolved.toString());
        for (Map.Entry<String,Object> entry : expensesOfThisGroup.entrySet()) {
            Map singleExpense = (Map) entry.getValue();

            String amountTemp = (String) singleExpense.get("amount");
            Money amount= Money.parse(amountTemp);
            String id = entry.getKey();
            String payerId = (String) singleExpense.get("payer_id");

            Log.d(TAG,"Expenses id:"+id+"amount: "+amount.toString()+"payer_id: "+ payerId);

            HashMap<String,Object> members_ids = (HashMap <String,Object>) singleExpense.get("members_ids");
            int numberOfMembersInvolved = members_ids.size();
            for (Map.Entry<String,Object> entry_nested :members_ids.entrySet()) {
                String idMember = entry_nested.getKey();
                Log.d(TAG,"Member Nested"+idMember);
                boolean isPayer=false;
                if(idMember.equals(payerId)) {
                    isPayer=true;
                }
                updateBalanceSingleMember(idMember,numberOfMembersInvolved,isPayer,amount);
            }
            distributeTheRestAmongParticipants(amount,numberOfMembersInvolved,payerId);
            checkIfThePayerIsFinancierOfTheExpense(members_ids,payerId,amount);
        }

        decideWhoHasToGiveBackTo();
    }

    private void distributeTheRestAmongParticipants(Money amount,int numberOfMembersInvolved,String payerId) {
        Money quote = amount.div(new BigDecimal(numberOfMembersInvolved));
        Money totalAmountCalculated = (quote.mul(new BigDecimal(numberOfMembersInvolved)));
        Money singleExpenseRest = new Money(new BigDecimal("0.00"));
        if (totalAmountCalculated.getAmount().compareTo(amount.getAmount())!=0) {
            singleExpenseRest = amount.sub(totalAmountCalculated);
            Log.d(TAG,"SingleRest: "+singleExpenseRest.toString());
            //TODO Make the distribution onSingleExpense
            BigDecimal d = BigDecimal.valueOf(singleExpenseRest.getAmount().doubleValue());
            BigDecimal result = d.subtract(d.setScale(0, RoundingMode.HALF_UP)).movePointRight(d.scale());
            Integer numberOfIteration = result.abs().intValue();
            Log.d(TAG,"Number Of Iteration: "+numberOfIteration);
            for (Map.Entry<String,MemberRepresentation> entry: membersBalanceInvolved.entrySet()) {
                if (numberOfIteration!=0) {
                    Log.d(TAG,"ITERATO");
                    MemberRepresentation member =entry.getValue();
                    if (entry.getKey().equals(payerId)) {continue;}
                    if (singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)>0) {
                        Money memberResidue = member.getResidue();
                        memberResidue = memberResidue.add(new Money(new BigDecimal("-0.01")));
                        member.setResidue(memberResidue);
                    }
                    if (singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)<0) {
                        Money memberResidue = member.getResidue();
                        memberResidue = memberResidue.add(new Money(new BigDecimal("+0.01")));
                        member.setResidue(memberResidue);
                    }
                    numberOfIteration--;
                }
            }
        }
    }

    private void decideWhoHasToGiveBackTo() {
        HashMap<String,MemberRepresentation> temporaryMapForComputation = (HashMap<String,MemberRepresentation>) membersBalanceInvolved.clone();

        for (Map.Entry<String,MemberRepresentation> entry:membersBalanceInvolved.entrySet()) {
            MemberRepresentation singleMember = entry.getValue();

            Money residue = singleMember.getResidue();
            if (residue.getAmount().compareTo(BigDecimal.ZERO)<0) {
                //Log.d(TAG,"Value <0");
                // TODO create a double map with payers and who has to pay
                singleMember.setMemberToGiveBack("Value < 0 Has To GiveBack");
            }
            if (residue.getAmount().compareTo(BigDecimal.ZERO)>0) {
                //Log.d(TAG,"Value >0");
                // TODO create a double map with payers and who has to pay
                singleMember.setMemberToGiveBack("Value > 0 Has To Receive");
            }
        }

    }

    private void checkIfThePayerIsFinancierOfTheExpense(HashMap<String,Object> members_ids,String payerId,Money amount) {
        if (!members_ids.containsKey(payerId)) {
            //The payer of this Expense is only a financier;
            //and Because we don't find him inside the members_ids of the single expense
            //we have to add him to the membersBalanceInvolved,in order to be visualized, but
            //only as a financier that means without computing a quote
            //but just adding him with a positive residue, equivalent to the entire amount
            String financierName = usersOfThisGroup.get(payerId).toString();
            Money  financierResidue = amount;
            if (membersBalanceInvolved.containsKey(payerId)) {
                //This is the case in which the Financier has been already considered and visualized in
                //others expenses made before , but as a normal member of the single expense
                //So we have to consider the update of the residue
                Money residue = membersBalanceInvolved.get(payerId).getResidue();
                financierResidue = residue.add(financierResidue);
            }
            MemberRepresentation memberUpdated = new MemberRepresentation(financierName,financierResidue);
            membersBalanceInvolved.put(payerId,memberUpdated);
        }
    }

    private void updateBalanceSingleMember(String idMember,int numberOfMembersInvolved,boolean isPayer,Money amount) {
        MemberRepresentation member = membersBalanceInvolved.get(idMember);
        Money residue = member.getResidue();
        Money quote = amount.div(new BigDecimal(numberOfMembersInvolved)).neg();
        Log.d(TAG,"Quote:"+quote.toString());
        Log.d(TAG,"IdMember: "+idMember);
        if(isPayer) {
            Log.d(TAG,"isPayer: "+isPayer);
            Money positiveQuote = quote.neg();
            Money income = amount.sub(positiveQuote);
            residue = residue.add(income);
            Log.d(TAG,"ResiduePAYER: "+residue);
        } else {
            residue = residue.add(quote);
            Log.d(TAG,"Residue: "+residue);
        }
        MemberRepresentation memberUpdated = new MemberRepresentation(member.getName(),residue);
        membersBalanceInvolved.put(idMember,memberUpdated);
    }

    public class MemberRepresentation {
        String name;
        Money residue;
        String memberToGiveBack="";

        public MemberRepresentation(String name,Money residue) {
            this.name = name;
            this.residue = residue;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Money getResidue() {
            return residue;
        }

        public void setResidue(Money residue) {
            this.residue = residue;
        }

        public String getMemberToGiveBack() {
            return memberToGiveBack;
        }

        public void setMemberToGiveBack(String memberToGiveBack) {
            this.memberToGiveBack = memberToGiveBack;
        }
    }
}
