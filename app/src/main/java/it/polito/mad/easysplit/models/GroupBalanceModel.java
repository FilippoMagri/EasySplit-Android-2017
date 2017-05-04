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
    private Map<String,Object> expensesOfThisGroup_Firebase = new HashMap<String,Object>();
    private Map<String,Object> usersOfThisGroup_Firebase = new HashMap<String,Object>();

    // membersBalanceInvolved
    // is the HashMap created specially to compute the balance among a single group
    private final HashMap<String,MemberRepresentation> membersBalanceInvolved = new HashMap<String,MemberRepresentation>();

    // The parameter groupBalanceAdapter in the constructor in necessary because we use it at the end of
    // computation in order to update the listItems inside the ListView
    // (And of course the listView is relative to the Fragment: "MemberListFragment")

    public GroupBalanceModel(final Uri mGroupUri, final GroupBalanceAdapter groupBalanceAdapter) {
        thisGroupId = mGroupUri.toString().replace("content://it.polito.mad.easysplit/groups/","");
        Log.d(TAG,mGroupUri.toString());
        mRoot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String,Object> entireDatabase = new HashMap<String, Object>();
                entireDatabase = (Map<String, Object>)dataSnapshot.getValue();
                for (Map.Entry<String,Object> entry : entireDatabase.entrySet()) {
                    HashMap<String,Object> singleTableDb = (HashMap<String, Object>) entry.getValue();
                    if (entry.getKey().equals("users")) {
                        retrieveUsersRelatedThisGroup(singleTableDb);
                    }
                    if(entry.getKey().equals("expenses")) {
                        retrieveExpensesRelatedThisGroup(singleTableDb);
                    }
                }
                computeBalance();
                printDebugBalances();
                updateListItemsOnFragment(groupBalanceAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateListItemsOnFragment(GroupBalanceAdapter groupBalanceAdapter) {
        groupBalanceAdapter.clear();
        for (Map.Entry<String,MemberRepresentation> entry : membersBalanceInvolved.entrySet()) {
            MemberRepresentation member = (MemberRepresentation) entry.getValue();
            Money residue = member.getResidue();
            String name = member.getName();
            String idMember = entry.getKey();
            GroupBalanceAdapter.ListItem listItem = new GroupBalanceAdapter.ListItem(idMember,name,residue);
            Log.d(TAG,"CIAO");
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
        for (Map.Entry<String,Object> entry : expensesOfThisGroup_Firebase.entrySet()) {
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

    private void retrieveUsersRelatedThisGroup(Map<String, Object> mapOfAllUsers) {
        for (Map.Entry<String,Object> entry: mapOfAllUsers.entrySet()) {
            Map singleUser = (Map) entry.getValue();
            String name = (String) singleUser.get("name");
            String id = (String) singleUser.get("id");
            Map groupIds = (Map) singleUser.get("groups_ids");
            if (groupIds.containsKey(thisGroupId)) {
                Log.d(TAG, "User:" + name);
                usersOfThisGroup_Firebase.put(entry.getKey(),entry.getValue());
                Money money = new Money(new BigDecimal("0.00"));
                membersBalanceInvolved.put(entry.getKey(),new MemberRepresentation(name,money));
            }
        }
        Log.d(TAG,"FINAL MAP USERS OF THIS GROUP:"+ usersOfThisGroup_Firebase.size());
    }

    public void retrieveExpensesRelatedThisGroup (Map<String, Object> mapOfAllExpenses) {
        for (Map.Entry<String, Object> entry : mapOfAllExpenses.entrySet()) {
            Map singleExpense = (Map) entry.getValue();
            String group_id = (String) singleExpense.get("group_id");
            if(group_id.equals(thisGroupId)) {
                expensesOfThisGroup_Firebase.put(entry.getKey(),entry.getValue());
            }
            Log.d(TAG,singleExpense.toString());
            Log.d(TAG,group_id);
        }
        Log.d(TAG,"FINAL MAP EXPENSES OF THIS GROUP:"+ expensesOfThisGroup_Firebase.size());
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
