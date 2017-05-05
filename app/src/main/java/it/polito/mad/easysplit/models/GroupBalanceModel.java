package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

    // membersBalanceInvolved
    // is the HashMap created specially to compute the balance among a single group
    private final HashMap<String,MemberRepresentation> membersBalanceInvolved = new HashMap<String,MemberRepresentation>();

    // The parameter groupBalanceAdapter in the constructor in necessary because we use it at the end of
    // computation in order to update the listItems inside the ListView
    // (And of course the listView is relative to the Fragment: "MemberListFragment")

    public GroupBalanceModel(final Uri mGroupUri, final GroupBalanceAdapter groupBalanceAdapter) {
        thisGroupId = mGroupUri.toString().replace("content://it.polito.mad.easysplit/groups/","");
        Log.d(TAG,mGroupUri.toString());
        DatabaseReference groupRef = mRoot.child("groups").child(thisGroupId).getRef();
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String,Object> entireGroupDB = new HashMap<String, Object>();
                entireGroupDB = (Map<String, Object>)dataSnapshot.getValue();
                retrieveUsersOfThisGroup((Map<String,Object>)entireGroupDB.get("members_ids"));
                retrieveExpensesOfThisGroup((Map<String,Object>)entireGroupDB.get("expenses"));
                computeBalance();
                printDebugBalances();
                updateListItemsOnFragment(groupBalanceAdapter);
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
            GroupBalanceAdapter.ListItem listItem = new GroupBalanceAdapter.ListItem(idMember,name,residue);
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
    }
}
