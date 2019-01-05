package myapp.upb.androidfirebase;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import myapp.upb.androidfirebase.models.UploadInfo;
import myapp.upb.androidfirebase.viewholder.ViewHolder;

public class ListViewAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    private List<UploadInfo> fileList = null;

    public ListViewAdapter(Context context, List<UploadInfo> fileList) {
        this.context = context;
        this.fileList = fileList;
        inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.listview_item, null);
            // Locate the TextViews in listview_item.xml
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.btn = (ImageButton) view.findViewById(R.id.downloadButton);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        // Set the results into TextViews
        holder.name.setText(fileList.get(position).getName());
        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    final ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setTitle("Downloading file...");
                    progressDialog.show();

                    File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "tmp2131.jpg");
                    localFile.createNewFile();

                    StorageReference ref = FirebaseStorage.getInstance().getReference().child("docs").child(fileList.get(position).getName());

                    ref.getFile(localFile)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Download finished", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    exception.printStackTrace();
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                    progressDialog.setMessage((int) progress + "% downloaded");
                                }
                            });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Listen for ListView Item Click
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
//                Intent intent = new Intent(context, SingleItemView.class);
//                intent.putExtra("name",(fileList.get(position).getName()));
//                context.startActivity(intent);
            }
        });

        return view;
    }
}
