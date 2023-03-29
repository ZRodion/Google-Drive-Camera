package com.example.googledrivecamera;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<String> createFile(Bitmap bitmap, String content, String fileName) {
        return Tasks.call(mExecutor, () -> {
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setMimeType("text/plain");

            ByteArrayContent mediaContent = ByteArrayContent.fromString("text/plain", content);

            File createdFile = mDriveService.files().create(fileMetadata, mediaContent).execute();

            byte[] imageData = getBytesFromUri(bitmap);

            File photoFile = new File();
            photoFile.setName(fileName);
            photoFile.setMimeType("image/jpeg");

            ByteArrayContent image = new ByteArrayContent("image/jpeg", imageData);
            File create = mDriveService.files().create(photoFile, image).execute();

            return createdFile.getId();
        });
    }

    //найти замену Tasks.call
    public Task<String> createImageFile(Bitmap bitmap, String fileName) {
        try {
            byte[] imageData = getBytesFromUri(bitmap);

            return Tasks.call(mExecutor, () -> {
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setMimeType("image/jpeg");

                ByteArrayContent content = new ByteArrayContent("image/jpeg", imageData);
                File create = mDriveService.files().create(fileMetadata, content).execute();

                return create.getId();
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBytesFromUri(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageInByte = baos.toByteArray();
        return imageInByte;
    }

    //проверить работоспособность
    public byte[] getBytesFromUri(Uri uri) throws IOException {
        //Only decode image size. Not whole image
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getPath(), option);

        // Minimum width and height are > NEW_SIZE (e.g. 380 * 720)
        final int NEW_SIZE = 480;

        //Now we have image width and height. We should find the correct scale value. (power of 2)
        int width = option.outWidth;
        int height = option.outHeight;
        int scale = 1;
        while (width / 2 > NEW_SIZE || height / 2 > NEW_SIZE) {
            width /= 2;//  ww w . j  a  va  2  s.co  m
            height /= 2;
            scale++;
        }
        //Decode again with inSampleSize
        option = new BitmapFactory.Options();
        option.inSampleSize = scale;

        Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath(), option);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        bitmap.recycle();

        return stream.toByteArray();
    }


}
