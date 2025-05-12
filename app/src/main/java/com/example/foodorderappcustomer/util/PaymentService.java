package com.example.foodorderappcustomer.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.foodorderappcustomer.Models.Order;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PaymentService {
    private static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_RETURN_URL = "foodorderapp://payment/result";
    private static final String VNPAY_TMN_CODE = "YOUR_VNPAY_TMN_CODE"; // Replace with your VNPay merchant code
    private static final String VNPAY_HASH_SECRET = "YOUR_VNPAY_HASH_SECRET"; // Replace with your VNPay hash secret

    public interface PaymentCallback {
        void onPaymentSuccess(String transactionId);
        void onPaymentFailure(String errorMessage);
    }

    public static void processVNPayPayment(Activity activity, Order order, PaymentCallback callback) {
        try {
            // Generate VNPay payment URL
            String vnpayUrl = generateVNPayUrl(order);
            
            // Open VNPay payment page
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(vnpayUrl));
            activity.startActivity(intent);

            // Update order status in Firebase
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(order.getId());
            orderRef.child("paymentStatus").setValue("pending");
            orderRef.child("paymentMethod").setValue("VNPay");
            orderRef.child("paymentTime").setValue(new Date());

            // Note: In a real app, you would need to implement a deep link handler
            // to receive the payment result from VNPay
            callback.onPaymentSuccess("VNPay_" + System.currentTimeMillis());
        } catch (Exception e) {
            callback.onPaymentFailure("Lỗi khi xử lý thanh toán VNPay: " + e.getMessage());
        }
    }

    public static void processCardPayment(Activity activity, Order order, String cardNumber, 
                                        String expiryDate, String cvv, PaymentCallback callback) {
        try {
            // In a real app, you would integrate with a payment gateway like Stripe
            // For now, we'll simulate a successful payment
            String transactionId = "CARD_" + System.currentTimeMillis();
            
            // Update order status in Firebase
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(order.getId());
            orderRef.child("paymentStatus").setValue("completed");
            orderRef.child("paymentMethod").setValue("Card");
            orderRef.child("paymentTime").setValue(new Date());
            orderRef.child("transactionId").setValue(transactionId);

            callback.onPaymentSuccess(transactionId);
        } catch (Exception e) {
            callback.onPaymentFailure("Lỗi khi xử lý thanh toán thẻ: " + e.getMessage());
        }
    }

    private static String generateVNPayUrl(Order order) {
        // Generate VNPay payment URL with required parameters
        String vnpayUrl = VNPAY_URL + "?";
        vnpayUrl += "vnp_Version=2.1.0";
        vnpayUrl += "&vnp_Command=pay";
        vnpayUrl += "&vnp_TmnCode=" + VNPAY_TMN_CODE;
        vnpayUrl += "&vnp_Amount=" + (long)(order.getTotal() * 100); // Convert to smallest currency unit
        vnpayUrl += "&vnp_CurrCode=VND";
        vnpayUrl += "&vnp_TxnRef=" + order.getId();
        vnpayUrl += "&vnp_OrderInfo=Thanh toan don hang " + order.getId();
        vnpayUrl += "&vnp_OrderType=other";
        vnpayUrl += "&vnp_Locale=vn";
        vnpayUrl += "&vnp_ReturnUrl=" + VNPAY_RETURN_URL;
        vnpayUrl += "&vnp_IpAddr=127.0.0.1"; // Replace with actual IP in production
        vnpayUrl += "&vnp_CreateDate=" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

        // Add secure hash
        vnpayUrl += "&vnp_SecureHash=" + generateSecureHash(vnpayUrl);

        return vnpayUrl;
    }

    private static String generateSecureHash(String url) {
        // In a real app, implement proper hash generation according to VNPay's documentation
        // This is just a placeholder
        return String.format("%032x", new Random().nextLong());
    }
} 