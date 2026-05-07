package de.zinformatik.galleryvideo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GetVideoActivity extends AppCompatActivity {

    private static final String TAG = "GalleryVideo";
    private ActivityResultLauncher<Intent> videoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    handleVideoSelected(result);
                }
            }
        );

        openVideoGallery();
    }

    private void openVideoGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");

        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("video/*");
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooserIntent = Intent.createChooser(pickIntent, getString(R.string.select_video));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{getContentIntent});

        try {
            videoPickerLauncher.launch(chooserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Tidak bisa membuka galeri", e);
            Toast.makeText(this, R.string.error_no_gallery, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void handleVideoSelected(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Uri selectedVideoUri = result.getData().getData();
        if (selectedVideoUri == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Uri outputUri = getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);

        if (outputUri != null) {
            boolean success = copyVideo(selectedVideoUri, outputUri);
            if (success) {
                Intent resultIntent = new Intent();
                resultIntent.setData(outputUri);
                setResult(RESULT_OK, resultIntent);
            } else {
                Toast.makeText(this, R.string.error_copy_failed, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
            }
        } else {
            Intent resultIntent = new Intent();
            resultIntent.setData(selectedVideoUri);
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setResult(RESULT_OK, resultIntent);
        }

        finish();
    }

    private boolean copyVideo(Uri source, Uri output) {
        try {
            InputStream in = getContentResolver().openInputStream(source);
            OutputStream out = getContentResolver().openOutputStream(output);
            if (in == null || out == null) return false;
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Gagal copy video", e);
            return false;
        }
    }
}
