package it.polito.mad.easysplit.models;

import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by fil on 07/05/17.
 */

public class CreditorDebtorModel {
    static String TAG="CreditorDebtorModel";
    HashMap<String,GroupBalanceModel.MemberRepresentation> creditors = new HashMap<>();
    HashMap<String,GroupBalanceModel.MemberRepresentation> debtors = new HashMap<>();

    ArrayList<CatchUpGroup> listOfCatchUpGroup = new ArrayList<>();

    public CreditorDebtorModel (HashMap<String,GroupBalanceModel.MemberRepresentation> membersBalanceInvolved) {
        for (Map.Entry<String,GroupBalanceModel.MemberRepresentation> entry:membersBalanceInvolved.entrySet()) {
            GroupBalanceModel.MemberRepresentation member = entry.getValue();
            String idMember = entry.getKey();
            String nameMember = member.getName();
            Money residue = member.getResidue();
            if (residue.getAmount().compareTo(BigDecimal.ZERO)>0) {
                //Is a creditor , then i add it to the global creditors map
                creditors.put(idMember,member);
            } else
            if (residue.getAmount().compareTo(BigDecimal.ZERO)<0) {
                //Is a debtor , then i add it to the global debtors map
                debtors.put(idMember,member);
            }
        }
        elaborateZeroGroups();
    }

    private void printDebugCreditors(HashMap<String, GroupBalanceModel.MemberRepresentation> creditors) {
        for (Map.Entry<String,GroupBalanceModel.MemberRepresentation> entry : creditors.entrySet()) {
            GroupBalanceModel.MemberRepresentation member = entry.getValue();
            Log.d(TAG,"CreditorName: "+member.getName());
            Log.d(TAG,"CreditorResidue: "+member.getResidue().getAmount().toString());
        }
    }

    private void printDebugDebtors(Map<String,GroupBalanceModel.MemberRepresentation> debtors) {
        for (Map.Entry<String,GroupBalanceModel.MemberRepresentation> entry : debtors.entrySet()) {
            GroupBalanceModel.MemberRepresentation member = entry.getValue();
            Log.d(TAG,"DebtorName: "+member.getName());
            Log.d(TAG,"DebtorResidue: "+member.getResidue().getAmount().toString());
        }
    }

    private void printDebugCatchUpGroups() {
        Iterator<CatchUpGroup> i = listOfCatchUpGroup.iterator();
        int index =0;
        while (i.hasNext()) {
            Log.d(TAG,"CatchUp Group Number:"+(++index));
            CatchUpGroup catchUpGroup = i.next();
            Log.d(TAG,"Creditor: "+ catchUpGroup.creditor.getName()+"With: "+ catchUpGroup.creditor.getResidue().toString());
            for (int in = 0; in< catchUpGroup.listOfDebtors.size(); in++) {
                Log.d(TAG,"Debtor n°"+(in)+" "+ catchUpGroup.listOfDebtors.get(in).getName()+ "With: "+ catchUpGroup.listOfDebtors.get(in).getResidue());
            }
        }
    }

    private void elaborateZeroGroups() {
        //Filling each single catchUpGroup with a Creditor and then with a list of debtors
        for (Map.Entry<String,GroupBalanceModel.MemberRepresentation> entry:creditors.entrySet()) {
            CatchUpGroup catchUpGroup = new CatchUpGroup(entry.getValue(),entry.getKey());
            while (!catchUpGroup.isCompleted()) {
                catchUpGroup.addDebtor();
            }
            listOfCatchUpGroup.add(catchUpGroup);
        }
    }

    public class CatchUpGroup {
        GroupBalanceModel.MemberRepresentation creditor;
        //The catchUpAmount represent something that at the end of the process
        //will be zero in every CatchUpGroup. That means we have finally
        //founded all single creditors associated to a list of debtors
        Money catchUpAmount;
        ArrayList<GroupBalanceModel.MemberRepresentation> listOfDebtors = new ArrayList<>();
        public CatchUpGroup(GroupBalanceModel.MemberRepresentation creditor,String idCreditor) {
            this.creditor = new GroupBalanceModel.MemberRepresentation(creditor.getName(),creditor.getResidue(),idCreditor);
            catchUpAmount = creditor.getResidue();
        }

        public void addDebtor() {
            GroupBalanceModel.MemberRepresentation debtor =
                                            new GroupBalanceModel.MemberRepresentation("",new Money(new BigDecimal("0.00")));
            String debtorKey="";
            for (Map.Entry<String,GroupBalanceModel.MemberRepresentation> entry:debtors.entrySet()) {
                GroupBalanceModel.MemberRepresentation memberRepresentation = entry.getValue();
                debtorKey = entry.getKey();
                debtor = new GroupBalanceModel.MemberRepresentation(memberRepresentation.getName(),
                                                                    memberRepresentation.getResidue(),debtorKey);
                break;
            }
            if (debtor.getResidue().getAmount().abs().compareTo(catchUpAmount.getAmount())>0) {
                //I've to split the residue of the debtor
                Money residue = catchUpAmount.add(debtor.getResidue());
                GroupBalanceModel.MemberRepresentation newDebtor =
                                                new GroupBalanceModel.MemberRepresentation(debtor.getName(), catchUpAmount.neg());
                this.listOfDebtors.add(newDebtor);
                catchUpAmount = new Money(new BigDecimal("0.00"));
                debtor.setResidue(residue);
            }
            if (debtor.getResidue().getAmount().abs().compareTo(catchUpAmount.getAmount())<=0) {
                //I don't have to split the residue of the debtor but just add to the listOfDebtors
                catchUpAmount = catchUpAmount.add(debtor.getResidue());
                this.listOfDebtors.add(debtor);
                debtors.remove(debtorKey);
            }
        }

        public boolean isCompleted () {
            //This is the method used to check if a catchUpGroup is completed
            if (catchUpAmount.getAmount().compareTo(BigDecimal.ZERO)==0) return true;
            else return false;
        }

        public ArrayList<GroupBalanceModel.MemberRepresentation> getListOfDebtors() {
            return listOfDebtors;
        }

        public void setListOfDebtors(ArrayList<GroupBalanceModel.MemberRepresentation> listOfDebtors) {
            this.listOfDebtors = listOfDebtors;
        }

        public GroupBalanceModel.MemberRepresentation getCreditor() {
            return creditor;
        }

        public void setCreditor(GroupBalanceModel.MemberRepresentation creditor) {
            this.creditor = creditor;
        }

        public Money getCatchUpAmount() {
            return catchUpAmount;
        }

        public void setCatchUpAmount(Money catchUpAmount) {
            this.catchUpAmount = catchUpAmount;
        }
    }

    public ArrayList<CatchUpGroup> getListOfCatchUpGroup() {
        return listOfCatchUpGroup;
    }

    public void setListOfCatchUpGroup(ArrayList<CatchUpGroup> listOfCatchUpGroup) {
        this.listOfCatchUpGroup = listOfCatchUpGroup;
    }
}
