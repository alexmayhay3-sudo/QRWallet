package com.example.qrwallet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private static final String PREFS = "qr_wallet";
    private static final String KEY_INDEX = "current_index";

    private LinearLayout root;
    private FrameLayout imageFrame;
    private ImageView qrImage;
    private TextView titleText;
    private TextView pageText;
    private Button shareButton;
    private Button addButton;
    private final ArrayList<File> images = new ArrayList<>();
    private int currentIndex = 0;
    private float downX;
    private float downY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadImages();
        currentIndex = getPreferences(MODE_PRIVATE).getInt(KEY_INDEX, 0);
        if (currentIndex >= images.size()) currentIndex = Math.max(0, images.size() - 1);
        buildScreen();
        refreshScreen();

        if (images.isEmpty()) {
            // First launch: guide the user to choose the first QR image.
            openPicker();
        }
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(18));
        root.setBackgroundColor(Color.rgb(247, 248, 250));

        TextView heading = new TextView(this);
        heading.setText("My QR Wallet");
        heading.setTextColor(Color.rgb(25, 30, 40));
        heading.setTextSize(25);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Your saved QR codes, ready whenever you need them");
        subtitle.setTextColor(Color.rgb(105, 112, 125));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(36)));

        imageFrame = new FrameLayout(this);
        imageFrame.setBackgroundColor(Color.WHITE);
        imageFrame.setElevation(dp(3));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        imageParams.setMargins(0, dp(22), 0, dp(18));
        root.addView(imageFrame, imageParams);

        qrImage = new ImageView(this);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImage.setPadding(dp(12), dp(12), dp(12), dp(12));
        imageFrame.addView(qrImage, new FrameLayout.LayoutParams(-1, -1));

        // Swipe left/right anywhere on the QR display to change images.
        imageFrame.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (Math.abs(dx) > dp(55) && Math.abs(dx) > Math.abs(dy)) {
                        if (dx < 0) showNext();
                        else showPrevious();
                    }
                    return true;
            }
            return true;
        });

        titleText = new TextView(this);
        titleText.setTextColor(Color.rgb(35, 40, 50));
        titleText.setTextSize(16);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        root.addView(titleText, new LinearLayout.LayoutParams(-1, dp(34)));

        pageText = new TextView(this);
        pageText.setTextColor(Color.rgb(110, 118, 130));
        pageText.setTextSize(13);
        pageText.setGravity(Gravity.CENTER);
        root.addView(pageText, new LinearLayout.LayoutParams(-1, dp(30)));

        shareButton = new Button(this);
        shareButton.setText("Share QR Code");
        shareButton.setAllCaps(false);
        shareButton.setTextSize(16);
        shareButton.setOnClickListener(v -> shareCurrentImage());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(-1, dp(54));
        shareParams.setMargins(0, dp(16), 0, dp(8));
        root.addView(shareButton, shareParams);

        addButton = new Button(this);
        addButton.setText("＋ Add QR Code");
        addButton.setAllCaps(false);
        addButton.setOnClickListener(v -> openPicker());
        root.addView(addButton, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView hint = new TextView(this);
        hint.setText("Swipe left or right on the image to switch QR codes");
        hint.setTextColor(Color.rgb(120, 126, 138));
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, dp(38));
        root.addView(hint, hintParams);

        setContentView(root);
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE) return;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // The image is copied into app storage below, so persisted permission is not required.
            }
            try {
                File folder = new File(getFilesDir(), "qr_images");
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception("Could not create image storage folder");
                }
                String fileName = "qr_" + System.currentTimeMillis() + ".img";
                File destination = new File(folder, fileName);
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(destination)) {
                    if (in == null) throw new Exception("Unable to open selected image");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                }
                images.add(destination);
                currentIndex = images.size() - 1;
                saveIndex();
                refreshScreen();
                Toast.makeText(this, "QR image saved on this phone", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not save image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (images.isEmpty()) {
            refreshScreen();
        }
    }

    private void loadImages() {
        File folder = new File(getFilesDir(), "qr_images");
        if (!folder.exists()) return;
        File[] found = folder.listFiles();
        if (found == null) return;
        for (File file : found) {
            if (file.isFile()) images.add(file);
        }
        Collections.sort(images, (a, b) -> a.getName().compareTo(b.getName()));
    }

    private void refreshScreen() {
        boolean hasImages = !images.isEmpty();
        if (qrImage == null) return;
        qrImage.setImageDrawable(null);
        if (hasImages) {
            File file = images.get(currentIndex);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                qrImage.setImageBitmap(bitmap);
            } else {
                titleText.setText("Unable to display this image");
            }
            titleText.setText("QR Code " + (currentIndex + 1));
            pageText.setText((currentIndex + 1) + " / " + images.size());
        } else {
            titleText.setText("No QR codes saved yet");
            pageText.setText("Choose an image to get started");
        }
        shareButton.setEnabled(hasImages);
    }

    private void showNext() {
        if (images.size() < 2) return;
        currentIndex = (currentIndex + 1) % images.size();
        saveIndex();
        refreshScreen();
    }

    private void showPrevious() {
        if (images.size() < 2) return;
        currentIndex = (currentIndex - 1 + images.size()) % images.size();
        saveIndex();
        refreshScreen();
    }

    private void saveIndex() {
        getPreferences(MODE_PRIVATE).edit().putInt(KEY_INDEX, currentIndex).apply();
    }

    private void shareCurrentImage() {
        if (images.isEmpty()) return;
        try {
            File source = images.get(currentIndex);
            Uri contentUri = FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", source);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share QR code"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share this QR image", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
