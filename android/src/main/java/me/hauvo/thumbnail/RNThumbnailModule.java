
package me.hauvo.thumbnail;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.media.MediaMetadataRetriever;
import 	android.graphics.Matrix;

import java.util.UUID;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;


public class RNThumbnailModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNThumbnailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNThumbnail";
  }

  @ReactMethod
  public void get(String filePath, Promise promise) {
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    if (URLUtil.isFileUrl(filePath)) {
      filePath = filePath.replace("file://","");
      retriever.setDataSource(filePath);
    } else{
      retriever.setDataSource(filePath,new HashMap<String, String>());
    }
    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    long timeInMillisec = Long.parseLong(time );
    Bitmap image = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    String fullPath = reactContext.getApplicationContext().getCacheDir().getAbsolutePath() + "/thumb";
    try {
      File dir = new File(fullPath);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      OutputStream fOut = null;
      // String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";
      String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";
      File file = new File(fullPath, fileName);
      file.createNewFile();
      fOut = new FileOutputStream(file);

      image.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
      fOut.flush();
      fOut.close();

      long cacheDirSize = 200 * 1024 * 1024;
      long newSize = image.getByteCount() + getDirSize(dir);
      if(newSize>cacheDirSize){
        cleanDir(dir, cacheDirSize / 2);
      }

      WritableMap map = Arguments.createMap();

      map.putString("path", "file://" + fullPath + '/' + fileName);
      map.putDouble("timeInMilliSec",timeInMillisec);
      retriever.release();
      promise.resolve(map);

    } catch (Exception e) {
      Log.e("E_RNThumnail_ERROR", e.getMessage());
      promise.reject("E_RNThumnail_ERROR", e);
    }
  }

  private static long getDirSize(File dir) {
    long size = 0;
    File[] files = dir.listFiles();

    for (File file : files) {
      if (file.isFile()) {
        size += file.length();
      }
    }
    return size;
  }

  private static void cleanDir(File dir, long bytes) {
    long bytesDeleted = 0;
    File[] files = dir.listFiles();
    Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

    for (File file : files) {
      bytesDeleted += file.length();
      file.delete();

      if (bytesDeleted >= bytes) {
        break;
      }
    }
  }
}
