package net.huaxin.umdatacollection;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import net.huaxin.umdatacollection.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class AddLocationsActivity extends AppCompatActivity {
    private ListView listView;
    Button AddLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_locations);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        listView=(ListView)findViewById(R.id.list);


        SQLiteDatabase mSQLiteDatabase = this.openOrCreateDatabase("location.db", MODE_PRIVATE, null);
        //mSQLiteDatabase.execSQL("DROP TABLE Locations");
        try{
            mSQLiteDatabase.execSQL("create table Locations (" +
                    "Lat REAL, " +
                    "Lon REAL, " +
                    "Label Text)");
        }catch(Exception e){}

        Cursor cur = mSQLiteDatabase.rawQuery("SELECT * FROM Locations", null);
        ArrayList<HashMap<String,String>> loc = new ArrayList<HashMap<String,String>>();
        if(cur != null){
            if(cur.moveToFirst()){
                do{
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("ItemTitle",cur.getFloat(cur.getColumnIndex("Lat"))+" , "+cur.getFloat(cur.getColumnIndex("Lon"))+" "+cur.getString(cur.getColumnIndex("Label")));
                    loc.add(map);
                   }while(cur.moveToNext());
            }
        }
        mSQLiteDatabase.close();

        //生成适配器的Item和动态数组对应的元素
        SimpleAdapter listItemAdapter = new SimpleAdapter(this,loc,
                R.layout.list_items,//ListItem的XML实现
                //动态数组与ImageItem对应的子项
                new String [] {"ItemTitle"},
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID
                new int [] {R.id.item_text}
        );

        //添加并且显示
        listView.setAdapter(listItemAdapter);
        AddLocation = (Button)findViewById(R.id.add_locations);
        AddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(AddLocationsActivity.this);
                View promptsView = li.inflate(R.layout.prompts, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        AddLocationsActivity.this);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText userInputLat = (EditText) promptsView
                        .findViewById(R.id.editTextLatitude);
                final EditText userInputLon = (EditText) promptsView
                        .findViewById(R.id.editTextLongitude);
                final EditText userInputLabel = (EditText) promptsView
                        .findViewById(R.id.editTextLabel);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // get user input and set it to result
                                        // edit text
                                        SQLiteDatabase mSQLiteDatabase = openOrCreateDatabase("location.db", MODE_PRIVATE, null);
                                        //mSQLiteDatabase.execSQL("DROP TABLE Locations");
                                        try{
                                            mSQLiteDatabase.execSQL("create table Locations (" +
                                                    "Lat REAL, " +
                                                    "Lon REAL" +
                                                    "Label TEXT)");
                                        }catch(Exception e){}
                                        if (isDouble(userInputLat.getText().toString()) &&isDouble(userInputLon.getText().toString())){
                                            ContentValues  cv  =  new ContentValues();
                                            cv.put("Lat", userInputLat.getText().toString());
                                            cv.put("Lon", userInputLon.getText().toString());
                                            cv.put("Label", userInputLabel.getText().toString());
                                            mSQLiteDatabase.insert("Locations", null, cv);
                                            mSQLiteDatabase.close();
                                        } else {
                                            Toast.makeText(AddLocationsActivity.this,"Format is not correct!",0).show();
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
            }
        });
    }

    public static boolean isDouble(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
        return pattern.matcher(str).matches();
    }
}
