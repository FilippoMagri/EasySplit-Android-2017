package it.polito.mad.easysplit;

import android.app.Application;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import it.polito.mad.easysplit.models.ConstraintException;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.dummy.DummyExpenseModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;
import it.polito.mad.easysplit.models.dummy.DummyPersonModel;

import static android.os.Environment.getExternalStorageDirectory;

public class AddExpenses extends AppCompatActivity {
    Toolbar toolbar;
    ImageView checkImgView;
    private EditText dateEditText;
    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;
    private Spinner spinner1;
    private String payer;
    DummyGroupModel gm;
    List<PersonModel> lpm;
    ArrayList<String> payerGroup;
    ArrayList<PersonModel> payersEngaged = new ArrayList<PersonModel>();;
    int size_group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expenses);
        populateWithInformationReceived ();
        setEditText_n1();
        setActionButtons();
        addItemsOnSpinner();
        setEditText_n5();
    }

    public void populateWithInformationReceived () {
        Intent received_intent = getIntent();
        if (received_intent.getExtras() == null ){
            gm = DummyGroupModel.getInstance();
            writeIntoJsonFile(gm);
            gm = readFromJsonFile("user.json");
            lpm = gm.getMembers();
            size_group = gm.getMembers().size();
            setTitle(gm.getName());
        } else {
            if (received_intent.getExtras().get("Uniqid").equals("From_Activity_AddExpenses_checkBox")) {
                //Populate again Screen By Reading file user.json
                gm = readFromJsonFile("user.json");
                lpm = gm.getMembers();
                size_group = gm.getMembers().size();
                setTitle(gm.getName());
                //Retrieve Element Engaged into the payment
                payerGroup = (ArrayList<String>) received_intent.getExtras().getSerializable("payerGroup");
                if (payerGroup.size()<lpm.size()) {
                    EditText editText5 = (EditText) findViewById(R.id.dateEditText5);
                    editText5.setText("Custom Members");
                }

            }
            //gm = inforeceivedFromVisualizeActivity_group
            //lpm  = inforeceivedFromVisualizeActivity_group
            //setTitle(gm.getName());
        }
    }

    private void setEditText_n1 () {
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ITALY);
        dateEditText = (EditText) findViewById(R.id.dateEditText);
        dateEditText.setInputType(InputType.TYPE_NULL);
        dateEditText.requestFocus();
        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view == dateEditText) {
                    datePickerDialog.show();
                }
            }
        });
        Calendar newCalendar = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                dateEditText.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        // Disable future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        Calendar newDate = Calendar.getInstance();
        dateEditText.setText(dateFormatter.format(newDate.getTime()));
    }

    public void setActionButtons() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        checkImgView = (ImageView) findViewById(R.id.img_confirm_add_expenses);

        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left_black_36dp);
        setSupportActionBar(toolbar);
        //Setting the Listener On The Button To Come Back To the Previous Activity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        //Setting the Listener On the Check Image View On Top Right
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                EditText dateEditText1 = (EditText) findViewById(R.id.dateEditText);
                String date = dateEditText1.getText().toString();

                String dateStr = date;
                SimpleDateFormat curFormater = new SimpleDateFormat("dd-MM-yyyy");
                Date dateObj=null;
                try {
                    dateObj = curFormater.parse(dateStr);
                } catch(ParseException e) { e.printStackTrace();}
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateObj);
                EditText dateEditText2 = (EditText) findViewById(R.id.dateEditText3);
                String title = dateEditText2.getText().toString();
                EditText dateEditText3 = (EditText) findViewById(R.id.dateEditText4);
                String amount = dateEditText3.getText().toString();
                Money money = new Money(Money.getDefaultCurrency(),(long)(Float.parseFloat(amount)*100));

                String payer_selected = payer;
                DummyPersonModel dummyPersonModel_ofpayer  = new DummyPersonModel(payer,gm);
                List<PersonModel> participants;

                if (payerGroup != null) {
                    participants = new ArrayList<>();
                    for (int i = 0; i < payerGroup.size(); i++) {
                        participants.add(gm.getMember(payerGroup.get(i)));
                    }
                }
                else {
                    participants = gm.getMembers();
                }

                DummyExpenseModel expenseModel = new DummyExpenseModel(title,calendar,money,dummyPersonModel_ofpayer,gm, participants);
                try {
                    gm.addExpense(expenseModel);
                } catch (ConstraintException e) {
                    e.printStackTrace();
                }
                MyApplication app = (MyApplication) getApplicationContext();
                app.setGroupModel(gm);


                //Something like app.setpayersEngaged
                Intent i = new Intent(getApplicationContext(),ExpensesListActivity.class);
                startActivity(i);
            }
        });
    }

    public void addItemsOnSpinner() {
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        List<String> list = new ArrayList<String>();
        int dim = lpm.size();
        for (int i=0;i<dim;i++) {
            list.add(lpm.get(i).getIdentifier());
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(dataAdapter);

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                payer = adapterView.getItemAtPosition(i).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void setEditText_n5 () {
        EditText editText5 = (EditText)findViewById(R.id.dateEditText5);
        if ((payerGroup==null)||(payerGroup.size()==lpm.size())) editText5.setText("All Members");
        editText5.setInputType(InputType.TYPE_NULL);
        editText5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),AddExpenses_checkBox.class);
                Bundle b = new Bundle();
                intent.putExtra("group_info",gm.toJSON());
                startActivity(intent);
            }
        });
    }

    public void writeIntoJsonFile(GroupModel groupModel) {
        JsonWriter writer;
        try {
            File file = new File(getCacheDir() , "user.json");
            writer = new JsonWriter(new FileWriter(file.getPath()));
            writer.beginObject();
            writer.name("group_name").value(groupModel.getName());
            writer.name("number_of_members").value(groupModel.getMembers().size());
            writer.name("members");
            writer.beginArray();
            for (int i=0;i<groupModel.getMembers().size();i++) {
                writer.value(groupModel.getMembers().get(i).getIdentifier());
            }
            writer.endArray();
            writer.endObject();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DummyGroupModel readFromJsonFile (String nameFile) {
        DummyGroupModel groupModel = null;
        try {
            JsonReader reader = new JsonReader(new FileReader(getCacheDir().toString()+"/"+nameFile));

            reader.beginObject();

            while (reader.hasNext()) {

                String name = reader.nextName();

                if (name.equals("group_name")) {
                    String test = reader.nextString();
                    groupModel = new DummyGroupModel(test);
                    System.out.println(test);
                } else if (name.equals("number_of_members")) {
                    System.out.println(reader.nextInt());
                } else if (name.equals("members")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        groupModel.getMembers().add(new DummyPersonModel(reader.nextString(),groupModel));
                    }
                    reader.endArray();
                } else {
                    reader.skipValue(); //avoid some unhandle events
                }
            }

            reader.endObject();
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return groupModel;
    }

}
