package com.example.googledrivecamera;


import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DriveServiceHelper {
    private final String TAG = "MyTag";
    private final String GOOGLE_DRIVE_CAMERA_FOLDER = "Google Drive Camera";

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<String> createFile(Bitmap bitmap, String content, String photoDate) {
        return Tasks.call(mExecutor, () -> {

            //create/get folder
            String folderId = getCameraFolder();
            if(folderId == null){
                folderId = createFolder(GOOGLE_DRIVE_CAMERA_FOLDER);
            }

            //get file name
            FileList files = mDriveService.files().list().execute();
            List<File> list = files.getFiles();

            //
            for(File item: list){
                Log.d(TAG, item.getName());
            }
            //

            //create txt file
            File fileMetadata = new File();
            fileMetadata.setName("photo_"+ list.size()/2 + ".geo");
            fileMetadata.setParents(Collections.singletonList(folderId));
            fileMetadata.setMimeType("text/plain");
            ByteArrayContent mediaContent = ByteArrayContent.fromString("text/plain", content);
            mDriveService.files().create(fileMetadata, mediaContent).execute();

            //create image file
            byte[] imageData = getBytesFromBitmap(bitmap);
            File photoFile = new File();
            photoFile.setName(photoDate);
            photoFile.setParents(Collections.singletonList(folderId));
            photoFile.setMimeType("image/jpeg");
            ByteArrayContent image = new ByteArrayContent("image/jpeg", imageData);
            mDriveService.files().create(photoFile, image).execute();
            return "createdFile.getId()";
        });
    }

    private String getCameraFolder(){
        List<File> files = getFilesByName(GOOGLE_DRIVE_CAMERA_FOLDER);
        if(files.size() != 0)
            return files.get(0).getId();
        else
            return null;
    }

    public boolean isFileExist(String fileName) throws IOException {
        List<File> files = getFilesByName(fileName);
        return files.size() != 0;
    }

    private List<File> getFilesByName(String fileName){
        try {
            FileList files = mDriveService.files().list().setQ("name='" + fileName + "'").execute();

            return files.getFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageInByte = baos.toByteArray();
        return imageInByte;
    }

    public String createFolder(String folderName){
        try {
            File folderMetadata = new File();
            folderMetadata.setName(folderName);
            isFileExist(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            File folder = mDriveService.files().create(folderMetadata).execute();
            return folder.getId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
