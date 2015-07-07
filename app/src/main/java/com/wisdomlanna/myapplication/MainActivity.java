package com.wisdomlanna.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import me.iwf.photopicker.PhotoPickerActivity;
import me.iwf.photopicker.utils.PhotoPickerIntent;


public class MainActivity extends Activity {
    AlertDialog alertDialog = new AlertDialog();

    private int counter;
    private static final int REQUEST_CODE = 1;

    private ProgressDialog progressDialog;

    private Context context = this;
    private ArrayList<String> photos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // btnUpload
        Button btnUpload = (Button) findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PhotoPickerIntent intent = new PhotoPickerIntent(MainActivity.this);
                intent.setPhotoCount(5);
                intent.setShowCamera(true);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            if (data != null) {
                photos =
                        data.getStringArrayListExtra(PhotoPickerActivity.KEY_SELECTED_PHOTOS);

                counter = photos.size();
                UploadFileAsync uploadFileAsync = new UploadFileAsync();
                uploadFileAsync.execute();
            }
        }
    }

    public class UploadFileAsync extends AsyncTask<String, Void, Void> {

        String resServer;

        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "", "Please wait...");
        }

        @Override
        protected Void doInBackground(String... params) {
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            int resCode = 0;
            String resMessage = "";

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            final String strUrlServer = "http://cm-smarthome.com/android/uploadImage.php";

            String strSDPath;

            for (int counterRound = 0; counterRound < counter; counterRound++) {
                strSDPath = photos.get(counterRound);
                try {
                    FileInputStream fileInputStream = new FileInputStream(new File(strSDPath));

                    URL url = new URL(strUrlServer);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");

                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type",
                            "multipart/form-data;boundary=" + boundary);

                    DataOutputStream outputStream = new DataOutputStream(conn
                            .getOutputStream());
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream
                            .writeBytes("Content-Disposition: form-data; name=\"filUpload\";filename=\""
                                    + strSDPath + "\"" + lineEnd);
                    outputStream.writeBytes(lineEnd);

                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // Read file
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {
                        outputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    // Response Code and  Message
                    resCode = conn.getResponseCode();
                    if (resCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();

                        int read = 0;
                        while ((read = is.read()) != -1) {
                            bos.write(read);
                        }
                        byte[] result = bos.toByteArray();
                        bos.close();

                        resMessage = new String(result);

                    }

                    Log.d("resCode=", Integer.toString(resCode));
                    Log.d("resMessage=", resMessage.toString());

                    fileInputStream.close();
                    outputStream.flush();
                    outputStream.close();

                    resServer = resMessage.toString();
                } catch (Exception ex) {
                    // Exception handling
                    alertDialog.alertDialogError("Sorry...", "Internet fail!", context);
                    return null;
                }
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            progressDialog.dismiss();
            new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Upload your Image")
                    .setContentText("Successfully")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismiss();
                        }
                    })
                    .show();
        }
    }
}