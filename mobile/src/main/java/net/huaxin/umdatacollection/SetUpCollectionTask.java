package net.huaxin.umdatacollection;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class SetUpCollectionTask extends AsyncTask <String, String, String>{

    String TAG = "SetUpCollectionTask Class";
    String path;
    String userName, collectFreq;
    Context context;
    boolean connection = false;
    ProgressDialog progressDialog;
    int idx;

    SetUpCollectionTask(String userName, String collectFreq, Context context, String path)
    {
        this.userName = userName;
        this.collectFreq = collectFreq;
        this.context = context;
        this.path = path;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(path);
        try {

            JSONObject jsonobj = new JSONObject();

            jsonobj.put("userName", userName);
            //jsonobj.put("tripNumber", tripNumber);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("req", jsonobj.toString()));

            Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());

            // Use UrlEncodedFormEntity to send in proper format which we need
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            try{
                HttpResponse response = httpclient.execute(httppost);
                InputStream inputStream = response.getEntity().getContent();
                InputStreamToStringExample str = new InputStreamToStringExample();
                String serverResponse = str.getStringFromInputStream(inputStream);
                Log.v(TAG, "response -----" + serverResponse);
                idx = Integer.parseInt(serverResponse);
                Log.v(TAG, "idx -----" + idx);
                connection = true;

            }catch(Exception e){

                e.printStackTrace();
                connection = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        LoginActivity loginActivity = null;
        progressDialog.dismiss();
        /*
        Intent intent = new Intent(context, DataCollectionActivity_v2.class);
        String str = userName+idx;
        intent.putExtra("user information",str);
        intent.putExtra("frequency", collectFreq);
        intent.putExtra("connection",connection);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        */


        Intent intent = new Intent(context, DataCollectionActivity_v2.class);

        String str = userName+idx;
        intent.putExtra("user information",str);
        intent.putExtra("frequency", collectFreq);
        intent.putExtra("connection",connection);
        intent.addCategory(Intent.CATEGORY_HOME);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(!connection)
        {
            Toast.makeText(context, "Network issue. Store data locally", 0).show();
        }
        context.startActivity(intent);
        super.onPostExecute(s);
    }
}
