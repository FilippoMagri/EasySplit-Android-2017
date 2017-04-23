package it.polito.mad.easysplit;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;

public class AddExpenses_checkBox extends AppCompatActivity {

    private ImageView checkImgView;
    GroupModel dummyGroupModel_received;
    private List<PersonModel> listPersonModel_received;
    private ArrayList<String> payerGroup_ToSend;
    private ArrayList<String> payerGroup_FromTemporaryInfoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTitle("Select partecipants");
        setContentView(R.layout.activity_add_expenses_check_box);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        populateWithInformationReceived();
        addActionToButtonTopBar();
    }

    public void populateWithInformationReceived() {
        Intent received_intent = getIntent();
        //Otherwise: take the bundle, take elements sent by AddExpenses (managing JsonFormat)
        Bundle b = received_intent.getExtras();
        String info_received_json_format= (String) b.get("members_info");
        dummyGroupModel_received = DummyGroupModel.fromJSONstatic(info_received_json_format);
        try {
            readFromJsonFileTemporaryInformation(getExternalCacheDir(),"temporary_info.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        listPersonModel_received = dummyGroupModel_received.getMembers();
        setTitle(dummyGroupModel_received.getName());

        //add checkboxes Dynamically To The Screen
        LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayoutCheckBox);
        for(int i = 0; i < listPersonModel_received.size(); i++) {
            if (i< listPersonModel_received.size()) {
                CheckBox cb = new CheckBox(this);
                cb.setText(listPersonModel_received.get(i).getIdentifier());
                cb.setId(i);
                if(payerGroup_FromTemporaryInfoFile.size()>0) {
                    if (payerGroup_FromTemporaryInfoFile.contains(listPersonModel_received.get(i).getIdentifier())) {
                        cb.setChecked(true);
                    } else {
                        cb.setChecked(false);
                    }
                } else {
                    cb.setChecked(true);
                }
                cb.setWidth(LinearLayoutCompat.LayoutParams.MATCH_PARENT);
                cb.setGravity(Gravity.END);
                cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                cb.setTag(i);
                Resources r = getApplicationContext().getResources();
                int px = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16,
                        r.getDisplayMetrics()
                );

                LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
                checkParams.setMargins(px, px, px, px);
                ll.addView(cb, checkParams);
            }
        }
    }

    public void addActionToButtonTopBar (){
        checkImgView = (ImageView) findViewById(R.id.img_confirm_checkbox);
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),AddExpenses.class);
                Bundle b = new Bundle();
                //Add to the bundle all information about witch people are engaged into the cost
                payerGroup_ToSend = new ArrayList<String>();
                for (int i = 0; i< listPersonModel_received.size(); i++) {
                    CheckBox cb = (CheckBox) findViewById(i);
                    if (cb.isChecked()) {
                        payerGroup_ToSend.add(listPersonModel_received.get(i).getIdentifier());
                    }
                }
                b.putSerializable("payerMembersSelected", payerGroup_ToSend);
                //Custom Message if we want
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                intent.putExtras(b);
                intent.putExtra("Uniqid","Activity_AddExpenses_checkBox");
                startActivity(intent);
            }
        });
    }

    public void readFromJsonFileTemporaryInformation(File fileDir, String nameFile) throws IOException {
        //The information read here are inside a temporary file named temporary_info.json
        //created ad-hoc from the Main Activity AddExpenses
        JsonReader reader = new JsonReader(new FileReader(fileDir.toString()+"/"+nameFile));
        reader.beginObject();
        payerGroup_FromTemporaryInfoFile = new ArrayList<String>();
        while (reader.hasNext()) {
            String name = reader.nextName();
            String value="";
            if (name.equals("payer_members_selected")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    value = reader.nextString();
                    payerGroup_FromTemporaryInfoFile.add(value);
                }
                reader.endArray();
            }else {
                reader.skipValue(); //avoid some unhandle events
            }
        }

        reader.endObject();
        reader.close();

    }
}
