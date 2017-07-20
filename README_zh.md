# 霹雳相册AndroidSDK
霹雳相册 (Picky) - 通过先进的照片分析引擎对照片进行分类，打标签，打分。同时进行照片自动增强. 本SDK通过运用深度学习以及机器学习技术，提供给Android开发者照片分析以及图片增强功能。您可以在苹果应用商店下载到[《霹雳相册》](https://itunes.apple.com/cn/app/picky-smart-collage-maker-and-batch-photo-editor/id1156687266?mt=8)。本SDK包含了霹雳相册的核心功能 

本SDK目前尚为Alpha版，包含了霹雳相册的部分功能。未来几个月内，我们将持续更新更多的功能

本SDK包含了一个示例工程 (co.polarr.albumsdkdemo) 用于调试以及开发对接

最低版本限制 Android API Level 14 (4.0.3)

## 版权限制
包含本SDK在内的所有版本库中的内容，属于Polarr, Inc.版权所有。未经允许均不得用于商业目的。当前版本的示例SDK失效时间为2017年9月30日。如需要获取完整授权等更多相关信息，请联系我们[info@polarr.co](mailto:info@polarr.co)

## 功能模块
### 评分与打标签
本SDK提供图片打标签功能，从图片中获取最接近的3个描述标签。通过图片分析，计算出图片得分，分值从 1.0 至 5.0 （由差到好）。分析基于一下五个维度：
- 图片构成 (照片中出现了那些主题内容)
- 图片色彩
- 曝光度 (曝光充足或曝光不足，曝光过低或曝光过度)
- 清晰度 (图像清晰模糊的程度)
- 表现力 (如果照片中包含人脸, 评价是否微笑或睁开眼睛)

![图片评分](https://user-images.githubusercontent.com/5923363/28441168-0508b1c2-6ddc-11e7-8e0f-205ad2605d29.gif)

### 图片归类
根据图片的的特点，主题，色彩，评分进行相似图片归类，优质图片排序

![图片归类](https://user-images.githubusercontent.com/5923363/28239963-66cebc84-69aa-11e7-9809-47147ec6ac26.gif)

### 图片自动增强
根据图片的特性，进行图片自动增强

![自动增强](https://user-images.githubusercontent.com/5923363/28404915-c244e942-6d5d-11e7-90af-8ab7481927b6.gif)

## 增加 dependencies 到 Gradle 文件
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

## 图片评分接口
给一个图片文件评分，分值从 1.0 至 5.0
```java
String filePath;
Map<String, Object> result = Processing.processingFile(context, filePath);
float rating = (float)result.get("rating_all");
```

## 获取图片标签
从图片中获取最接近的3个描述标签
```java
Map<String, Object> taggingResult = TaggingUtil.tagPhoto(context.getAssets(), file);
List<String> labelTop3List = taggingResult.get("label_top3");
```

## 图片归类
先对每张图片进行评分，打标签获取图片特性，之后对图片进行归类
### 获取图片特性
图片特性包括评分和打标签两部分组成
```java
String filePath;
Map<String, Object> featureResult = Processing.processingFile(context, filePath);
Map<String, Object> taggingResult = TaggingUtil.tagPhoto(getAssets(), photo);
featureResult.putAll(taggingResult);
```
### 图片归类接口
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

## 自动增强
### 计算自动增强参数

```java
String filePath;
Map<String, Object> autoEnhanceStates = Processing.processingAutoEnhance(filePath);
```

### 渲染自动增强后的图片
如果需要渲染自动增强后的图片，需要使用泼辣修图的渲染SDK，请把下面的内容增加到Gradle文件
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
