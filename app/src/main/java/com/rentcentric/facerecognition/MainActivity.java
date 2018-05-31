package com.rentcentric.facerecognition;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    ImageView image0, image1;
    ListView listView0, listView1;
    Button selectImage0, selectImage1, verify;
    FaceListAdapter faceListAdapter0, faceListAdapter1;
    FaceServiceClient faceServiceClient;
    Bitmap bitmap0, bitmap1;
    UUID faceID0, faceID1;
    ProgressDialog progressDialog;
    int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image0 = findViewById(R.id.image_0);
        image1 = findViewById(R.id.image_1);
        listView0 = findViewById(R.id.list_faces_0);
        listView0.setOnItemClickListener(this);
        listView1 = findViewById(R.id.list_faces_1);
        listView1.setOnItemClickListener(this);
        selectImage0 = findViewById(R.id.select_image_0);
        selectImage0.setOnClickListener(this);
        selectImage1 = findViewById(R.id.select_image_1);
        selectImage1.setOnClickListener(this);
        verify = findViewById(R.id.Verify);
        verify.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        faceServiceClient = new FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_image_0:
                Intent intent0 = new Intent(Intent.ACTION_GET_CONTENT);
                intent0.setType("image/*");
                startActivityForResult(intent0, 0);
                index = 0;
                break;
            case R.id.select_image_1:
                Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
                intent1.setType("image/*");
                startActivityForResult(intent1, 1);
                index = 1;
                break;
            case R.id.Verify:
                new FaceVerificationTask().execute();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (index) {
            case 0:
                FaceListAdapter adapter0 = faceListAdapter0;
                faceID0 = adapter0.faces.get(i).faceId;
                image0.setImageBitmap(adapter0.faceThumbnails.get(i));
                listView0.setAdapter(adapter0);
                break;
            case 1:
                FaceListAdapter adapter1 = faceListAdapter1;
                faceID1 = adapter1.faces.get(i).faceId;
                image1.setImageBitmap(adapter1.faceThumbnails.get(i));
                listView1.setAdapter(adapter1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                Bitmap bitmap0 = FaceRecognitionImageHelper.loadSizeLimitedBitmapFromUri(data.getData(), getContentResolver());
                this.bitmap0 = bitmap0;
                ByteArrayOutputStream output0 = new ByteArrayOutputStream();
                bitmap0.compress(Bitmap.CompressFormat.JPEG, 100, output0);
                ByteArrayInputStream inputStream0 = new ByteArrayInputStream(output0.toByteArray());
                new FaceDetectionTask().execute(inputStream0);
                faceID0 = null;
                return;
            case 1:
                Bitmap bitmap1 = FaceRecognitionImageHelper.loadSizeLimitedBitmapFromUri(data.getData(), getContentResolver());
                this.bitmap1 = bitmap1;
                ByteArrayOutputStream output1 = new ByteArrayOutputStream();
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, output1);
                ByteArrayInputStream inputStream1 = new ByteArrayInputStream(output1.toByteArray());
                new FaceDetectionTask().execute(inputStream1);
                faceID1 = null;
                return;
        }
    }

    private class FaceListAdapter extends BaseAdapter {
        List<Face> faces = new ArrayList<>();
        List<Bitmap> faceThumbnails = new ArrayList<>();

        FaceListAdapter(Face[] detectionResult) {
            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        faceThumbnails.add(FaceRecognitionImageHelper.generateFaceThumbnail(index == 0 ? bitmap0 : bitmap1, face.faceRectangle));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face, parent, false);
            }
            convertView.setId(position);

            Bitmap thumbnailToShow = faceThumbnails.get(position);
            if (index == 0 && faces.get(position).faceId.equals(faceID0)) {
                thumbnailToShow = FaceRecognitionImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            } else if (index == 1 && faces.get(position).faceId.equals(faceID1)) {
                thumbnailToShow = FaceRecognitionImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            }
            ((ImageView) convertView.findViewById(R.id.image_face)).setImageBitmap(thumbnailToShow);
            return convertView;
        }
    }

    private class FaceDetectionTask extends AsyncTask<InputStream, String, Face[]> {

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Detecting...");
            progressDialog.show();
        }

        @Override
        protected Face[] doInBackground(InputStream... params) {
            try {
                return faceServiceClient.detect(params[0], true, false, null);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] result) {
            try {
                progressDialog.dismiss();
                if (result.length > 0) {
                    FaceListAdapter faceListAdapter = new FaceListAdapter(result);
                    switch (index) {
                        case 0:
                            faceID0 = faceListAdapter.faces.get(0).faceId;
                            image0.setImageBitmap(faceListAdapter.faceThumbnails.get(0));
                            listView0.setAdapter(faceListAdapter);
                            faceListAdapter0 = faceListAdapter;
                            bitmap0 = null;
                            break;
                        case 1:
                            faceID1 = faceListAdapter.faces.get(0).faceId;
                            image1.setImageBitmap(faceListAdapter.faceThumbnails.get(0));
                            listView1.setAdapter(faceListAdapter);
                            faceListAdapter1 = faceListAdapter;
                            bitmap1 = null;
                            break;
                    }
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setMessage("No face detected!");
                    alert.setCancelable(false);
                    alert.setPositiveButton("OK", null);
                    alert.show();
                }
            }catch (Exception e){
                Toast.makeText(MainActivity.this, "License is expired", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class FaceVerificationTask extends AsyncTask<Void, String, VerifyResult> {

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Verifying...");
            progressDialog.show();
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            try {
                return faceServiceClient.verify(faceID0, faceID1);
            } catch (Exception e) {
                progressDialog.setMessage(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            progressDialog.dismiss();
            DecimalFormat formatter = new DecimalFormat("#0.00");
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setMessage((result.isIdentical ? "The same person" : "Different persons") + ". The confidence is " + formatter.format(result.confidence));
            alert.setCancelable(false);
            alert.setPositiveButton("OK", null);
            alert.show();
        }
    }
}