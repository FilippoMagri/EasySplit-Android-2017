package it.polito.mad.easysplit;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.polito.mad.easysplit.models.ConstraintException;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.dummy.DummyExpenseModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;
import it.polito.mad.easysplit.models.dummy.DummyPersonModel;

public class AddExpenses extends AppCompatActivity {
    Toolbar toolbar;
    ImageView checkImgView;
    private EditText dateEditText;
    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;
    private Spinner spinner;
    private String payer;
    // TODO Fix phone_owner with the right reference to the unique id from Firebase
    private String phone_owner = "Camille";
    DummyGroupModel dummyGroupModel;
    List<PersonModel> membersDummyGroup;
    ArrayList<String> payerMembersSelected;
    int sizeDummyGroupModel;
    EditText editText1,editText2,editText3;
    ImageView dateImgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expenses);
        setDateEditText();
        try {
            populateWithInformationReceived ();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setActionOnButtons();
        addItemsOnSpinner();
        setEditText3();
    }

    public void populateWithInformationReceived () throws IOException {
        Intent received_intent = getIntent();
        if (received_intent.getExtras() == null ){
            dummyGroupModel = new DummyGroupModel("Gruppo MAD");
            try {
                dummyGroupModel.addMember(new DummyPersonModel("Filippo", dummyGroupModel));
                dummyGroupModel.addMember(new DummyPersonModel("Camille", dummyGroupModel));
                dummyGroupModel.addMember(new DummyPersonModel("Sebastiano", dummyGroupModel));
                dummyGroupModel.addMember(new DummyPersonModel("Flavio", dummyGroupModel));
            } catch (ConstraintException e) {
                e.printStackTrace();
            }
            dummyGroupModel.writeIntoJsonFile(getExternalCacheDir(),"group_members.json");
            membersDummyGroup = dummyGroupModel.getMembers();
            sizeDummyGroupModel = dummyGroupModel.getMembers().size();
            setTitle(dummyGroupModel.getName());
            payer = phone_owner;
        } else {
            if (received_intent.getExtras().get("Uniqid").equals("Activity_AddExpenses_checkBox")) {
                //Populate again Screen By Reading file group_members.json
                dummyGroupModel = new DummyGroupModel("Gruppo MAD");
                dummyGroupModel.readFromJsonFile(getExternalCacheDir(),"group_members.json");
                membersDummyGroup = dummyGroupModel.getMembers();
                sizeDummyGroupModel = dummyGroupModel.getMembers().size();
                setTitle(dummyGroupModel.getName());
                //Retrieve Element Engaged into the payment
                payerMembersSelected = (ArrayList<String>) received_intent.getExtras().getSerializable("payerMembersSelected");
                if (payerMembersSelected.size()< membersDummyGroup.size()) {
                    editText3 = (EditText) findViewById(R.id.EditText3);
                    editText3.setText("Custom Members");
                }
                readFromJsonFileTemporaryInformation(getExternalCacheDir() , "temporary_info.json");
            }
        }
    }

    private void setDateEditText() {
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

    public void setActionOnButtons() {

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
        //Setting the Listener on the Image Of The Calendar
        dateImgView = (ImageView) findViewById(R.id.dateImgView);
        dateImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });
        //Setting the Listener On the Check Image View On Top Right
        checkImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                String date = dateEditText.getText().toString();
                String dateStr = date;
                SimpleDateFormat curFormatter = new SimpleDateFormat("dd-MM-yyyy");
                Date dateObj=null;
                try {
                    dateObj = curFormatter.parse(dateStr);
                } catch(ParseException e) { e.printStackTrace();}
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateObj);
                editText1 = (EditText) findViewById(R.id.EditText1);
                String title = editText1.getText().toString();
                editText2 = (EditText) findViewById(R.id.EditText2);
                String amount = editText2.getText().toString();
                Money money = new Money(Money.getDefaultCurrency(),(long)(Float.parseFloat(amount)*100));

                DummyPersonModel payer_AsDummyPersonModel  = new DummyPersonModel(payer, dummyGroupModel);
                List<PersonModel> payerMemberSelected_asListPersonModel;

                if (payerMembersSelected != null) {
                    payerMemberSelected_asListPersonModel = new ArrayList<>();
                    for (int i = 0; i < payerMembersSelected.size(); i++) {
                        payerMemberSelected_asListPersonModel.add(dummyGroupModel.getMember(payerMembersSelected.get(i)));
                    }
                }
                else {
                    payerMemberSelected_asListPersonModel = dummyGroupModel.getMembers();
                }

                DummyExpenseModel expenseModel = new DummyExpenseModel(title,calendar,money,payer_AsDummyPersonModel, dummyGroupModel, payerMemberSelected_asListPersonModel);
                try {
                    dummyGroupModel.addExpense(expenseModel);
                } catch (ConstraintException e) {
                    e.printStackTrace();
                }
                MyApplication app = (MyApplication) getApplicationContext();
                if (app.getGroupModel()==null) {
                    app.setGroupModel(dummyGroupModel);
                } else {
                    app.getGroupModel().getExpenses().add(expenseModel);
                }
                //Something like app.setpayersEngaged
                Intent i = new Intent(getApplicationContext(),ExpensesListActivity.class);
                startActivity(i);
            }
        });
    }

    public void addItemsOnSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner);
        List<String> list = new ArrayList<String>();
        int dim = membersDummyGroup.size();
        for (int i=0;i<dim;i++) {
            if (membersDummyGroup.get(i).getIdentifier().equals(payer)) {
                list.add(membersDummyGroup.get(i).getIdentifier());
            }
        }
        for (int i=0;i<dim;i++) {
            if (!membersDummyGroup.get(i).getIdentifier().equals(payer)) {
                list.add(membersDummyGroup.get(i).getIdentifier());
            }
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                payer = adapterView.getItemAtPosition(i).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void setEditText3() {
        editText3 = (EditText)findViewById(R.id.EditText3);
        if ((payerMembersSelected ==null)||(payerMembersSelected.size()== membersDummyGroup.size())) editText3.setText("All Members");
        editText3.setInputType(InputType.TYPE_NULL);
        editText3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    writeIntoJsonFileTemporaryInformation(getExternalCacheDir(),"temporary_info.json");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(getApplicationContext(),AddExpenses_checkBox.class);
                Bundle b = new Bundle();
                intent.putExtra("members_info", dummyGroupModel.toJSON());
                startActivity(intent);
            }
        });
    }

    public void writeIntoJsonFileTemporaryInformation(File fileDir,String nameFile) throws IOException{
        dateEditText = (EditText)findViewById(R.id.dateEditText);
        editText1 = (EditText) findViewById(R.id.EditText1);
        editText2 = (EditText) findViewById(R.id.EditText2);
        JsonWriter writer;
        File file = new File(fileDir , nameFile);
        writer = new JsonWriter(new FileWriter(file.getPath()));
        writer.beginObject();
        writer.name("expense_date").value(dateEditText.getText().toString());
        if(editText1 == null) {
            writer.name("expense_title").value("");
        } else {
            writer.name("expense_title").value(editText1.getText().toString());
        }
        if (editText2 == null ) {
            writer.name("expense_amount").value("");
        } else {
            writer.name("expense_amount").value(editText2.getText().toString());
        }
        writer.name("expense_payer").value(payer);
        if(payerMembersSelected!=null) {
            writer.name("payer_members_selected");
            if (payerMembersSelected.size() > 0) {
                writer.beginArray();
                for (int i = 0; i < payerMembersSelected.size(); i++) {
                    writer.value(payerMembersSelected.get(i));
                }
                writer.endArray();
            } else {
                writer.value("");
            }
        }
        writer.endObject();
        writer.close();

    }

    public void readFromJsonFileTemporaryInformation(File fileDir,String nameFile) throws IOException {
        JsonReader reader = new JsonReader(new FileReader(fileDir.toString()+"/"+nameFile));
        reader.beginObject();
        while (reader.hasNext()) {
            dateEditText = (EditText)findViewById(R.id.dateEditText);
            editText1 = (EditText) findViewById(R.id.EditText1);
            editText2 = (EditText) findViewById(R.id.EditText2);
            String name = reader.nextName();
            String value="";
            if (name.equals("expense_date")) {
                value = reader.nextString();
                dateEditText.setText(value);
            } else if (name.equals("expense_title")) {
                value = reader.nextString();
                editText1.setText(value);
            } else if (name.equals("expense_amount")) {
                value = reader.nextString();
                editText2.setText(value);
            } else if (name.equals("expense_payer")) {
                value = reader.nextString();
                payer = value;
            }else {
                reader.skipValue(); //avoid some unhandle events
            }
        }

        reader.endObject();
        reader.close();

    }


}
