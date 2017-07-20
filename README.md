# PolarrAlbumAndroidSDK
Polarr Android SDK for Smart Album (Picky) - Includes photo auto grouping, tagging, rating and etc. The SDK serves as an arsenal for Android developers to leverage deep learning and machine learning to organize and enhance a set of photos. Polarr currently has an iOS App called [Picky](https://itunes.apple.com/us/app/picky-smart-collage-maker-and-batch-photo-editor/id1156687266?mt=8) which showcases all functions of the SDK plus some. This SDK is a subset of the functions provided by Picky, and is currently noted as an alpha release. In the coming months, more functionalities will be added and feature requests are welcomed.

This SDK includes a starter project (co.polarr.albumsdkdemo) that calls the Android SDK.

The minimum Android API Level is 14 (4.0.3).

## License
The SDK included in this repository must not be used for any commercial purposes without the direct written consent of Polarr, Inc. The current version of the SDK expires on September 30, 2017. For pricing and more info regarding the full license SDK, please email [info@polarr.co](mailto:info@polarr.co).

## Functionalities
### Tagging a photo
The SDK performs image classification and produce the top 3 most likely labels for the given photo. It also returns an overall rating value from 1.0 to 5.0 (where 1.0 is the worst and 5.0 is the best), which is based on the following metrics:
- Composition (how well subjects within the photos are placed)
- Colorfulness
- Exposure (well-exposed photo or poorly exposed: either under-exposed or over-exposed)
- Clarity (bluriness of the photo)
- Expression (if faces are detected, are they smiling, are the eyes open)

![Tagging a photo](https://user-images.githubusercontent.com/5923363/28239964-69369e06-69aa-11e7-9092-3ca7d0901378.gif)

### Grouping photos
Similar photos are grouped together based on their subjects, features, colors, and other metrics.
![Grouping photos](https://user-images.githubusercontent.com/5923363/28239963-66cebc84-69aa-11e7-9809-47147ec6ac26.gif)

### Auto enhance photos
Photos can be auto-enhanced based on their extracted features.
![Auto enhance photo](https://user-images.githubusercontent.com/5923363/28402063-60eb0148-6d50-11e7-8b1b-de49e46b8dfa.gif)

## Add dependencies to Gradle
```groovy
repositories {
    maven {
        url 'https://dl.bintray.com/tzutalin/maven'
    }
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    compile(name: 'utils-release', ext: 'aar')
    // tagging photo lib
    compile(name: 'tagging-release', ext: 'aar')
    // rating and grouping photos lib
    compile(name: 'processing-release', ext: 'aar')
    // Tensorflow lib
    compile 'org.tensorflow:tensorflow-android:+'
    // Face detection lib
    compile 'com.tzutalin.dlib-android-app:dlib:1.0.4'
}
```

## Rating photo
Rating a photo file. The score from 1.0 to 5.0.
```java
String filePath;
Map<String, Object> result = Processing.processingFile(context, filePath);
float rating = (float)result.get("rating_all");
```

## Tagging photo
Recognize a photo, get top 3 possible objects from the photo
```java
Map<String, Object> taggingResult = TaggingUtil.tagPhoto(context.getAssets(), file);
List<String> labelTop3List = taggingResult.get("label_top3");
```

## Grouping photos
First, rating and tagging photo to a feature result. Then grouping the feature results.
### Feature a photo
Join rating result and tagging result.
```java
String filePath;
Map<String, Object> featureResult = Processing.processingFile(context, filePath);
Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), photo);
featureResult.putAll(taggingResult);
```
### Grouping feature results
```java
// Todo please add file to realPhotos list.
List<File> realPhotos = new ArrayList<>();
```
```java
// Get photos' features
List<Map<String, Object>> features = new ArrayList<>();
for (File photo : realPhotos) {
    Map<String, Object> featureResult = Processing.processingFile(context, photo.getPath());
    Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), photo);
    featureResult.putAll(taggingResult);
    features.add(featureResult);
}
```
```java
// Grouping photos
String identifier = "group1";
boolean orderByTime = false;
boolean includePhotosWithText = false;
List<List<Integer>> result = Processing.processingGrouping(identifier, features, orderByTime, includePhotosWithText, new POGenerateHClusterCallbackFunction() {
    @Override
    public void progress(double progress) {
        // grouping progress
    }
});
 ```
 ```java
// Convert index array to file array
List<List<File>> groupedFiles = new ArrayList<>();
for (List<Integer> subGroup : result) {
    List<File> subFiles = new ArrayList<>();
    for (Integer index : subGroup) {
        subFiles.add(realPhotos.get(index));
    }
    groupedFiles.add(subFiles);
}
```

## Auto enhance photo
### Get auto enhanced render states

```java
String filePath;
Map<String, Object> autoEnhanceStates = Processing.processingAutoEnhance(filePath);
```

### Use the render states to render photo
If need render the auto enhanced photo, you should add the polarr render module in gradle
```groovy
dependencies {
    compile (name: 'renderer-release', ext: 'aar')
    // fast json decoder for native render
    compile 'com.alibaba:fastjson:1.1.55.android'
}
```

```java
ImageView autoEnhanceImageView; // to show auto enhanced photo
// get scaled bitmap
Bitmap imageBitmap; // bitmap to render
RenderUtil.renderThumbnailBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), getResources(), autoEnhanceStates, new OnThumbnailBitmapCallback() {
    @Override
    public void onExport(final Bitmap bitmap) {
        // callback will be on render thread
        ThreadManager.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                autoEnhanceImageView.setImageBitmap(bitmap);
            }
        });
    }
});
```
