package co.polarr.albumsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.polarr.processing.POGenerateHClusterCallbackFunction;
import co.polarr.processing.Processing;
import co.polarr.renderer.render.OnThumbnailBitmapCallback;
import co.polarr.renderer.render.RenderUtil;
import co.polarr.tagging.TaggingUtil;
import co.polarr.utils.ImageLoadUtil;
import co.polarr.utils.MemoryCache;
import co.polarr.utils.ThreadManager;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PHOTO = 1;
    private static final int REQUEST_PHOTOS = 2;
    private static final int REQUEST_AUTOENHANCE = 3;
    private static final int REQUEST_EXPORT = 4;

    /**
     * debug log view
     */
    private TextView outputView;
    private ScrollView outputCon;

    /**
     * show processing photo
     */
    private ImageView thumbnailView;
    /**
     * show auto enhanced photo
     */
    private ImageView autoEnhanceView;
    /**
     * open grouped photos view
     */
    private Button btnGroupResult;
    /**
     * rendered photo to export
     */
    private Bitmap toExportPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputView = (TextView) findViewById(R.id.tf_output);
        outputCon = (ScrollView) findViewById(R.id.sv_output);
        thumbnailView = (ImageView) findViewById(R.id.iv_thumbnail);
        autoEnhanceView = (ImageView) findViewById(R.id.iv_autoenhance);
        btnGroupResult = (Button) findViewById(R.id.btn_group_result);

        autoEnhanceView.setVisibility(View.GONE);
        btnGroupResult.setVisibility(View.GONE);

        findViewById(R.id.btn_import).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhotos();
            }
        });
        findViewById(R.id.btn_tag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });
        findViewById(R.id.btn_auto_enhance).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhotoForAutoEnhance();
            }
        });
    }

    /**
     * photo folder chooser
     */
    private void selectPhotos() {
        if (!checkAndRequirePermission(REQUEST_PHOTOS)) {
            return;
        }
        new ChooserDialog().with(this)
                .withFilter(true, false)
                .withStartFile(Environment.getExternalStorageDirectory().getPath() + "/DCIM")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        grouping(pathFile);
                    }
                })
                .build()
                .show();
    }

    /**
     * photo chooser
     */
    private void selectPhoto() {
        if (!checkAndRequirePermission(REQUEST_PHOTO)) {
            return;
        }
        new ChooserDialog().with(this)
                .withFilter(false, false, "jpg", "jpeg", "png")
                .withStartFile(Environment.getExternalStorageDirectory().getPath() + "/DCIM")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        ratingPhoto(pathFile);
                    }
                })
                .build()
                .show();
    }

    private void selectPhotoForAutoEnhance() {
        if (!checkAndRequirePermission(REQUEST_AUTOENHANCE)) {
            return;
        }
        clearOutput();
        btnGroupResult.setVisibility(View.GONE);
        autoEnhanceView.setVisibility(View.GONE);

        new ChooserDialog().with(this)
                .withFilter(false, false, "jpg", "jpeg", "png")
                .withStartFile(Environment.getExternalStorageDirectory().getPath() + "/DCIM")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        final Map<String, Object> autoEnhanceStates = Processing.processingAutoEnhance(pathFile.getPath());
                        output("[Top] Original [Bottom] Auto Enhanced. Long click the photo to export.");
                        updateThumbnail(pathFile);

                        final Bitmap imageBitmap = ImageLoadUtil.decodeThumbBitmapForFile(pathFile.getPath(), Integer.MAX_VALUE, Integer.MAX_VALUE, ImageLoadUtil.getImageOrientation(pathFile.getPath()));
                        RenderUtil.renderThumbnailBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), getResources(), autoEnhanceStates, new OnThumbnailBitmapCallback() {
                            @Override
                            public void onExport(final Bitmap bitmap) {
                                ThreadManager.executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageBitmap.recycle();
                                        autoEnhanceView.setVisibility(View.VISIBLE);
                                        autoEnhanceView.setImageBitmap(bitmap);

                                        if (toExportPhoto != null && !toExportPhoto.isRecycled()) {
                                            toExportPhoto.recycle();
                                        }

                                        toExportPhoto = bitmap;

                                        autoEnhanceView.setOnLongClickListener(new View.OnLongClickListener() {
                                            @Override
                                            public boolean onLongClick(View v) {
                                                exportImage();
                                                return true;
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                })
                .build()
                .show();
    }

    private void exportImage() {
        if (!checkAndRequirePermission(REQUEST_EXPORT)) {
            return;
        }

        if (toExportPhoto == null || toExportPhoto.isRecycled()) {
            output("The auto enhanced photo is recyled, please do auto enhance again.");
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File storageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PolarrDemo");

            //Creating the storage directory if it doesn't exist
            if (!storageDirectory.exists()) {
                storageDirectory.mkdirs();
            }

            //Creating the temporary storage file
            File targetImagePath = File.createTempFile(timeStamp + "_", ".jpg", storageDirectory);
            OutputStream outputStream = new FileOutputStream(targetImagePath);
            toExportPhoto.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            //Rescanning the icon_library/gallery so it catches up with our own changes
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(targetImagePath));
            sendBroadcast(mediaScanIntent);

            Toast.makeText(MainActivity.this, String.format(Locale.ENGLISH, "Saved to: %s", targetImagePath.getAbsolutePath()), Toast.LENGTH_LONG).show();
            output(String.format(Locale.ENGLISH, "Saved to: %s", targetImagePath.getAbsolutePath()));

        } catch (Exception e) {
            e.printStackTrace();
            output("Export photo faild. Please check the log in console.");
        }
    }

    private boolean checkAndRequirePermission(int permissionRequestId) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    permissionRequestId);

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHOTO && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectPhoto();
            }
        } else if (requestCode == REQUEST_PHOTOS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectPhotos();
            }
        } else if (requestCode == REQUEST_AUTOENHANCE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectPhotoForAutoEnhance();
            }
        } else if (requestCode == REQUEST_EXPORT && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportImage();
            }
        }
    }

    /**
     * clear all logs
     */
    private void clearOutput() {
        outputView.setText("");
    }

    /**
     * show output logs
     */
    private void output(final String text) {
        ThreadManager.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                String currentText = outputView.getText().toString();
                currentText += "\n" + text;
                outputView.setText(currentText);

                outputView.post(new Runnable() {
                    @Override
                    public void run() {
                        outputCon.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    /**
     * show current photo
     */
    private void updateThumbnail(File file) {
        final Bitmap bitmap = ImageLoadUtil.decodeThumbBitmapForFile(file.getPath(), 500, 500, ImageLoadUtil.getImageOrientation(file.getPath()));
        ThreadManager.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                thumbnailView.setImageBitmap(bitmap);
            }
        });
    }

    /**
     * processing photo, get photo rating and tagging
     */
    private void ratingPhoto(final File file) {
        clearOutput();
        btnGroupResult.setVisibility(View.GONE);
        autoEnhanceView.setVisibility(View.GONE);

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                output("start processing...");
                updateThumbnail(file);
                Map<String, Object> result = Processing.processingFile(MainActivity.this, file.getPath());
                output("Rating: " + result.get("rating_all"));
                for (String label : result.keySet()) {
                    if (label.startsWith("metric_")) {
                        String showLable = label.substring("metric_".length());
                        output("\t" + showLable + ": " + result.get(label));

                    }
                }
                output("tagging...");
                Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), file);
                result.putAll(taggingResult);

                output("\tLabel_top3: " + taggingResult.get("label_top3_output"));

                output("end processing.");
            }
        });
    }

    /**
     * processing and groupping photos
     *
     * @param folderPath folder of photos
     */
    private void grouping(final File folderPath) {
        clearOutput();
        btnGroupResult.setVisibility(View.GONE);
        autoEnhanceView.setVisibility(View.GONE);


        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                output("Prepare photos...");
                File[] photoFiles = folderPath.listFiles();
                List<File> realPhotos = new ArrayList<>();
                int maxProcessing = 30;
                for (File photo : photoFiles) {
                    BitmapFactory.Options size = ImageLoadUtil.decodeImageSize(photo.getPath());

                    if (size.outWidth > 0 && size.outHeight > 0) {
                        realPhotos.add(photo);
                    }
                    if (--maxProcessing == 0) {
                        break;
                    }
                }
                List<Map<String, Object>> features = new ArrayList<>();
                output("Processing photo features...");
                for (File photo : realPhotos) {
                    output("\tFeature...");
                    updateThumbnail(photo);
                    Map<String, Object> featureResult = Processing.processingFile(MainActivity.this, photo.getPath());
                    output("\tRating: " + featureResult.get("rating_all"));

                    output("\tTagging...");
                    Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), photo);
                    featureResult.putAll(taggingResult);
                    output("\tLabel_top3: " + taggingResult.get("label_top3"));

                    features.add(featureResult);
                }

                output("Grouping photos...");

                String identifier = "identifier";
                List<List<Integer>> result = Processing.processingGrouping(identifier, features, false, false, new POGenerateHClusterCallbackFunction() {
                    @Override
                    public void progress(double progress) {
                        output(String.format(Locale.ENGLISH, "Processing %.2f%%", progress * 100));
                    }
                });

                output("Grouping result:" + result.toString());

                List<List<File>> optFiles = new ArrayList<>();
                for (List<Integer> subGroup : result) {
                    List<File> sub = new ArrayList<>();
                    for (Integer index : subGroup) {
                        sub.add(realPhotos.get(index));
                    }
                    optFiles.add(sub);
                }

                MemoryCache.put("group_files", optFiles);

                output("Optimal group:" + optFiles.toString());

                ThreadManager.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        btnGroupResult.setVisibility(View.VISIBLE);
                        btnGroupResult.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(MainActivity.this, GroupingActivity.class));
                            }
                        });
                    }
                });

                output("end processing.\nClick 'Grouping result' button to see the result.");
            }
        });
    }
}