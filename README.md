# PolarrAlbumAndroidSDK
Polarr Android SDK for Smart Album (Picky) - Includes photo auto grouping, duplicate removal, tagging, rating and etc.

This SDK includes a starter project (co.polarr.albumsdkdemo) that calls the Android SDK.

The minimum Android API Level is 14 (4.0.3).

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
