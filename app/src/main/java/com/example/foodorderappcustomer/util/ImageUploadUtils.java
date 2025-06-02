package com.example.foodorderappcustomer.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class ImageUploadUtils {
    private static final String TAG = "ImageUploadUtils";

    public interface ImageUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
        void onProgress(double progress);
    }

    public static void uploadImage(Uri imageUri, String folder, ImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new IllegalArgumentException("Image URI cannot be null"));
            return;
        }

        // Create a unique filename
        String filename = UUID.randomUUID().toString() + ".jpg";
        
        // Get reference to the storage location
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child(folder).child(filename);

        // Upload the file
        UploadTask uploadTask = imageRef.putFile(imageUri);

        // Monitor upload progress
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        });

        // Handle upload success
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Image uploaded successfully. URL: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL: " + e.getMessage());
                callback.onFailure(e);
            });
        });

        // Handle upload failure
        uploadTask.addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading image: " + e.getMessage());
            callback.onFailure(e);
        });
    }

    /**
     * Deletes an image from Firebase Storage
     * 
     * @param imageUrl The URL of the image to delete
     * @param callback Callback to handle deletion result
     */
    public static void deleteImage(String imageUrl, ImageUploadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Image URL cannot be null or empty"));
            return;
        }

        try {
            // Get reference to the file
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            
            // Delete the file
            imageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Image deleted successfully");
                callback.onSuccess(null);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting image: " + e.getMessage());
                callback.onFailure(e);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage reference: " + e.getMessage());
            callback.onFailure(e);
        }
    }
}