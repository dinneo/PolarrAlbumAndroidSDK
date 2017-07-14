package co.polarr.albumsdkdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.polarr.processing.POGenerateHClusterCallbackFunction;
import co.polarr.processing.Processing;
import co.polarr.tagging.TaggingUtil;
import co.polarr.utils.ImageLoadUtil;
import co.polarr.utils.MemoryCache;
import co.polarr.utils.ThreadManager;

public class MainActivity extends AppCompatActivity {
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
     * open grouped photos view
     */
    private Button btnGroupResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputView = (TextView) findViewById(R.id.tf_output);
        outputCon = (ScrollView) findViewById(R.id.sv_output);
        thumbnailView = (ImageView) findViewById(R.id.iv_thumbnail);
        btnGroupResult = (Button) findViewById(R.id.btn_group_result);

        btnGroupResult.setEnabled(false);

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
    }

    /**
     * photo folder chooser
     */
    private void selectPhotos() {
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

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                output("start processing...");
                updateThumbnail(file);
                Map<String, Object> result = Processing.processingFile(MainActivity.this, file.getPath());
                output("Rating: " + result.get("rating_all"));

                output("tagging...");
                Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), file);
                result.putAll(taggingResult);
                output("\tLabel_top3: " + taggingResult.get("label_top3"));
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
        btnGroupResult.setEnabled(false);

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
                        btnGroupResult.setEnabled(true);
                        btnGroupResult.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(MainActivity.this, GroupingActivity.class));
                            }
                        });
                    }
                });
            }
        });
    }
}