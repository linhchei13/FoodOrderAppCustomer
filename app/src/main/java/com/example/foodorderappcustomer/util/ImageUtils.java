package com.example.foodorderappcustomer.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for loading images from Firebase Storage using Picasso
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";

    public interface ImageListCallback {
        void onSuccess(List<String> imageUrls);

        void onFailure(Exception e);
    }

    public interface ImageUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
        void onProgress(double progress);
    }

    /**
     * Deletes an image from Firebase Storage
     *
     * @param imageUrl The URL of the image to delete
     * @param callback Callback to handle the result
     */
    public static void deleteImage(String imageUrl, ImageListCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Image URL cannot be null or empty"));
            return;
        }

        try {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Image deleted successfully");
                        callback.onSuccess(new ArrayList<>());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting image: " + e.getMessage());
                        callback.onFailure(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage reference: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    /**
     * Loads an image from Firebase Storage or a URL into an ImageView

     * @param imageUrl         The image URL or Firebase Storage path
     * @param imageView        The ImageView to load into
     * @param placeholderResId Resource ID for the placeholder image
     * @param errorResId       Resource ID for the error image
     */
    public static void loadImage(String imageUrl, ImageView imageView, int placeholderResId, int errorResId) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(errorResId);
            return;
        }

        // If the URL is a Firebase Storage reference (starts with "gs://")
        if (imageUrl.startsWith("gs://")) {
            // Convert gs:// URL to download URL
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Load image with Picasso
                Picasso.get()
                        .load(uri.toString())
                        .error(errorResId)
                        .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Image loaded successfully from gs:// URL");
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error loading image from gs:// URL: " + e.getMessage());
                            }
                        });
            }).addOnFailureListener(e -> {
                // On failure, load error image
                imageView.setImageResource(errorResId);
                Log.e(TAG, "Error getting download URL: " + e.getMessage());
            });
        }
        // If it's already a HTTP URL
        else if (imageUrl.startsWith("http")) {
            // Load directly with Picasso
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(placeholderResId)
                    .error(errorResId)
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded successfully from HTTP URL");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading image from HTTP URL: " + e.getMessage());
                        }
                    });
        }
    }

    public static void uploadImage(Uri imageUri, String folder, ImageUploadUtils.ImageUploadCallback callback) {
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
    public static void deleteImage(String imageUrl, ImageUploadUtils.ImageUploadCallback callback) {
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