package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.layout.GroupBalanceAdapter.ListItem;
import it.polito.mad.easysplit.models.CreditorDebtorModel.CatchUpGroup;
import it.polito.mad.easysplit.models.GroupBalanceModel.MemberRepresentation;
import it.polito.mad.easysplit.models.Money;

/**
 * Created by fil on 03/05/17.
 */

public class GroupBalanceAdapter extends ArrayAdapter<ListItem> {

    static String TAG="GroupBalanceAdapter";

    public static final class ListItem {
        public String id;
        public String name;
        public Money residue;
        public String typeOfMember;
        ArrayList<CatchUpGroup> listOfCatchUpGroup= new ArrayList<>();

        public ListItem(String id, String name, Money residue,String typeOfMember,ArrayList<CatchUpGroup> listOfCatchUpGroup) {
            this.id = id;
            this.name = name;
            this.residue = residue;
            this.typeOfMember = typeOfMember;
            this.listOfCatchUpGroup = listOfCatchUpGroup;
        }
    }

    public GroupBalanceAdapter(Context ctx, ArrayList<ListItem> members) {
        super(ctx, 0, members);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout.list_item_member, parent, false);
        }

        ListItem item = getItem(position);
        TextView name = (TextView) convertView.findViewById(id.name);
        TextView residue = (TextView) convertView.findViewById(id.residue);
        TextView typeOfMember = (TextView) convertView.findViewById(id.typeOfMember);
        TextView keyItemMember = (TextView) convertView.findViewById(id.key_item_member);

        // access to nested linear layout, in order to attach the list of creditors or debtors
        LinearLayout layout = (LinearLayout)convertView.findViewById(id.linearLayout_catch_up_list);
        if(layout.getChildCount() > 0)
            layout.removeAllViews();

        //Retrieve all listOfCatchUpGroups
        ArrayList<CatchUpGroup> listOfCatchUpGroup= item.listOfCatchUpGroup;

        //Retrieve each single catchUpGroup And create a Map like this <idOfTheCreditor,CatchUpGroup_n0> and so on
        //Because basically every single creditor will be part of only one CatchUpGroup
        HashMap<String,CatchUpGroup> mapCreditors_CatchUpGroup = new HashMap<>();
        Iterator<CatchUpGroup> iterator = listOfCatchUpGroup.iterator();
        while (iterator.hasNext()) {
            CatchUpGroup element = iterator.next();
            mapCreditors_CatchUpGroup.put(element.getCreditor().getId(),element);
        }

        //TODO implement general Version For All Currencies
        name.setText(item.name);
        keyItemMember.setText(item.id);

        if (!item.residue.toStandardFormat().equals("0.00 EUR")) {
            residue.setText(item.residue.toString());
            if(item.residue.getAmount().compareTo(new BigDecimal("0.00"))>0) {
                //Case in which this member is a creditor
                residue.setTextColor(Color.GREEN);
                typeOfMember.setText(item.typeOfMember);
                //Retrieve its catch_up_group
                CatchUpGroup catchUpGroup = mapCreditors_CatchUpGroup.get(item.id);
                //Retrieve its list of debtors
                ArrayList<MemberRepresentation> listOfDebtors = catchUpGroup.getListOfDebtors();
                int number_of_Debtors = listOfDebtors.size();
                for (int i=0;i<number_of_Debtors;i++) {
                    //Retrieve Info about single Debtor
                    String nameOfDebtor = listOfDebtors.get(i).getName();
                    String idOfDebtor = listOfDebtors.get(i).getId();
                    Money residueOfDebtor = listOfDebtors.get(i).getResidue();
                    //Populate the child with the correct information about debtor
                    View child = LayoutInflater.from(getContext()).inflate(layout.catch_up_group_item,parent,false);
                    TextView message = (TextView)  child.findViewById(id.text_view_catch_up_item);
                    TextView residue_catch_up_item = (TextView) child.findViewById(id.residue_catch_up_item);
                    TextView keyOfDebtor = (TextView) child.findViewById(id.key_catch_up_item);
                    message.setText("This member has to receive from "+nameOfDebtor+": ");
                    residue_catch_up_item.setText(residueOfDebtor.toString());
                    keyOfDebtor.setText(idOfDebtor);
                    layout.addView(child);
                }
             } else {
                //Case in which this member is a debtor
                residue.setTextColor(Color.RED);
                typeOfMember.setText(item.typeOfMember);
                //Retrieve its list of Creditors
                for (Entry<String,CatchUpGroup> entry: mapCreditors_CatchUpGroup.entrySet()) {
                    CatchUpGroup catchUpGroup = entry.getValue();
                    String creditorName = catchUpGroup.getCreditor().getName();
                    String keyOfCreditor = catchUpGroup.getCreditor().getId();
                    ArrayList<MemberRepresentation> listOfDebtors = catchUpGroup.getListOfDebtors();
                    for (int i=0;i<listOfDebtors.size();i++) {
                        if (listOfDebtors.get(i).getName().equals(item.name)) {
                            //Element Present , retrieve the creditor Information and populate the child
                            View child = LayoutInflater.from(getContext()).inflate(layout.catch_up_group_item,parent,false);
                            TextView message = (TextView)  child.findViewById(id.text_view_catch_up_item);
                            TextView residue_catch_up_item = (TextView) child.findViewById(id.residue_catch_up_item);
                            TextView keyOfCreditor_tv = (TextView) child.findViewById(id.key_catch_up_item);
                            Money residueOfDebtorInsideCatchUpGroup = listOfDebtors.get(i).getResidue();
                            message.setText("This member has to give back to "+creditorName+":");
                            residue_catch_up_item.setText(residueOfDebtorInsideCatchUpGroup.toString());
                            keyOfCreditor_tv.setText(keyOfCreditor);
                            layout.addView(child);
                        }
                    }
                }
            }
        } else {
            //Case in which this memeber is catch-up
            residue.setText("Catch up");
            residue.setTextColor(Color.GREEN);
        }
        return convertView;
    }
}
