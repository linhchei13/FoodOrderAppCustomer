package com.example.foodorderappcustomer.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Fetches all images from a specific folder in Firebase Storage
     * 
     * @param folderName The folder name in Firebase Storage
     * @param callback Callback to handle the result
     */
    public static void getAllImagesFromFolder(String folderName, ImageListCallback callback) {
        StorageReference folderRef = FirebaseStorage.getInstance().getReference().child(folderName);
        
        folderRef.listAll()
            .addOnSuccessListener(listResult -> {
                List<String> imageUrls = new ArrayList<>();
                List<CompletableFuture<String>> futures = new ArrayList<>();

                for (StorageReference item : listResult.getItems()) {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    futures.add(future);

                    item.getDownloadUrl()
                        .addOnSuccessListener(uri -> future.complete(uri.toString()))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting download URL for " + item.getName() + ": " + e.getMessage());
                            future.complete(null);
                        });
                }

                // Wait for all futures to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        for (CompletableFuture<String> future : futures) {
                            String url = future.join();
                            if (url != null) {
                                imageUrls.add(url);
                            }
                        }
                        callback.onSuccess(imageUrls);
                    })
                    .exceptionally(e -> {
                        callback.onFailure(new Exception("Error processing image URLs: " + e.getMessage()));
                        return null;
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error listing files in " + folderName + ": " + e.getMessage());
                callback.onFailure(e);
            });
    }

    /**
     * Fetches all images from multiple folders in Firebase Storage
     * 
     * @param folderNames List of folder names to search in
     * @param callback Callback to handle the result
     */
    public static void getAllImagesFromFolders(List<String> folderNames, ImageListCallback callback) {
        List<String> allImageUrls = new ArrayList<>();
        List<CompletableFuture<Void>> folderFutures = new ArrayList<>();

        for (String folderName : folderNames) {
            CompletableFuture<Void> folderFuture = new CompletableFuture<>();
            folderFutures.add(folderFuture);

            getAllImagesFromFolder(folderName, new ImageListCallback() {
                @Override
                public void onSuccess(List<String> imageUrls) {
                    allImageUrls.addAll(imageUrls);
                    folderFuture.complete(null);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error getting images from " + folderName + ": " + e.getMessage());
                    folderFuture.complete(null); // Complete even on failure to continue with other folders
                }
            });
        }

        // Wait for all folders to be processed
        CompletableFuture.allOf(folderFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> callback.onSuccess(allImageUrls))
            .exceptionally(e -> {
                callback.onFailure(new Exception("Error processing folders: " + e.getMessage()));
                return null;
            });
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
     * 
     * @param context The context
     * @param imageUrl The image URL or Firebase Storage path
     * @param imageView The ImageView to load into
     * @param placeholderResId Resource ID for the placeholder image
     * @param errorResId Resource ID for the error image
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView, int placeholderResId, int errorResId) {
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
                    .placeholder(placeholderResId)
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
        // If it's just a file name, try to load from Firebase Storage
        else {
            // Try firebase storage with different folders
            loadImageFromStorageFolder(context, "food_images", imageUrl, imageView, placeholderResId, errorResId);
        }
    }

    /**
     * Attempts to load an image from a specified folder in Firebase Storage
     *
     * @param context The context
     * @param folderName The folder name in Firebase Storage
     * @param fileName The image file name
     * @param imageView The ImageView to load into
     * @param placeholderResId Resource ID for the placeholder image
     * @param errorResId Resource ID for the error image
     */
    private static void loadImageFromStorageFolder(Context context, String folderName, String fileName, 
                                                 ImageView imageView, int placeholderResId, int errorResId) {
        // Get reference to the file in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child(folderName).child(fileName);
        
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Picasso.get()
                .load(uri.toString())
                .placeholder(placeholderResId)
                .error(errorResId)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Image loaded successfully from " + folderName);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error loading image from " + folderName + ": " + e.getMessage());
                    }
                });
        }).addOnFailureListener(e -> {
            // If this folder failed, try another common folder
            if (folderName.equals("food_images")) {
                loadImageFromStorageFolder(context, "restaurant_images", fileName, imageView, placeholderResId, errorResId);
            } else if (folderName.equals("restaurant_images")) {
                loadImageFromStorageFolder(context, "images", fileName, imageView, placeholderResId, errorResId);
            } else {
                // If all folders failed, load error image
                imageView.setImageResource(errorResId);
                Log.e(TAG, "Error loading image from all folders: " + e.getMessage());
            }
        });
    }
} 