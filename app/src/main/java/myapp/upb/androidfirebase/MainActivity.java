package myapp.upb.androidfirebase;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

import myapp.upb.androidfirebase.models.UploadInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_FILE_REQUEST = 1;

    private Button btnSync, btnUpload;
    private ImageButton btnDownload;
    //private ImageView imgView;
    private ListView listView;
    private Uri filePath;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private DialogInterface.OnClickListener uploadDialogListener;
    private ArrayList<UploadInfo> listOfItems = new ArrayList<>();
    private ListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        //imgView = (ImageView) findViewById(R.id.imgView);
        listView = (ListView) findViewById(R.id.listView);
        btnSync = (Button) findViewById(R.id.btnSync);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnDownload = (ImageButton) findViewById(R.id.downloadButton);

        uploadDialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        uploadFile();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        filePath = null;
                        break;
                }
            }
        };

        adapter = new ListViewAdapter(this, listOfItems);

        listView.setAdapter(adapter);

        btnUpload.setOnClickListener(this);
        btnSync.setOnClickListener(this);

        databaseReference.child("docs").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                UploadInfo info = dataSnapshot.getValue(UploadInfo.class);
                listOfItems.add(info);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                UploadInfo info = dataSnapshot.getValue(UploadInfo.class);
                for (UploadInfo i : listOfItems) {
                    if (i.getId().equals(info.getId())) {
                        listOfItems.remove(i);
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select file"), PICK_FILE_REQUEST);
    }

    private void refreshListView() {
        adapter.notifyDataSetChanged();
    }

    private void uploadFile() {

        if (filePath != null) {

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading file...");
            progressDialog.show();

            final StorageReference fileRef = storageReference.child("docs").child(getFileName(filePath));

            fileRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();

                            while (!urlTask.isSuccessful());

                            String name = taskSnapshot.getMetadata().getName();
                            String url = urlTask.getResult().toString();

                            writeNewFileInfoToDB(name, url);

                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "File uploaded", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Error while uploading", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage((int) progress + "% uploaded");
                        }
                    });
        }
        else {
            Toast.makeText(getApplicationContext(), "Files not yet selected", Toast.LENGTH_LONG).show();
        }
    }

    public void writeNewFileInfoToDB(final String name, final String url) {
        UploadInfo info = new UploadInfo(name, url);

        if (name != null)
            databaseReference.child("docs").child(info.getId()).setValue(info);
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void displayUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Upload " + getFileName(filePath) + " to Firebase?").setPositiveButton("Yes", uploadDialogListener)
                .setNegativeButton("No", uploadDialogListener).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();

//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
//                imgView.setImageBitmap(bitmap);
            displayUploadDialog();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }

        }
    }

    @Override
    public void onClick(View view) {
        if (view == btnUpload) {
            showFileChooser();
        }
        else if (view == btnSync) {
            refreshListView();
        }
        else if (view == btnDownload) {
            System.out.println(view.getParent());
        }
    }
}
