package com.example.android.tflitecamerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by humax on 18/11/27
 */
public class CameraActivity2 extends Activity {
    public final String EXTRA_SPEAK_CONTENT = "extra_speak_content";

    private final String TAG = "CameraActivity";

    public static final String ACTION_STOP_SPEAK = "com.humax.intent.ACTION_STOP_SPEAK";
    public static final String ACTION_START_SPEAK = "com.humax.intent.ACTION_START_SPEAK";

    private static final String HANDLE_THREAD_NAME = "CameraBackground";
    private final Object lock = new Object();
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;


    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "final_result";
    private static final String MODEL_FILE = "file:///android_asset/model/graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/model/labels.txt";


    private ImageClassifier classifier;
    private Camera camera;
    private SurfaceView sfv;
    private ImageView imgv;
    private TextView tv_result;
    private HashMap<String, String> map = new HashMap<String, String>();

    private Handler handler = new Handler();
    private String result;
    Runnable r = new Runnable() {
        @Override
        public void run() {
            tv_result.setText(result);
            if (camera != null) {
                camera.startPreview();
                takePicture();
            }
        }
    };

    private String oldKey = "";
    private String maxKey = "";
    private float maxPercent = 0f;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_2);
        sfv = findViewById(R.id.sfv);
        imgv = findViewById(R.id.imgv);
        tv_result = findViewById(R.id.tv_result);
//        camera.setPreviewCallback(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] bytes, Camera camera) {
//                //获取图片
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                //
//                startImageClassifier(bitmap);
//
//            }
//        });
        tv_result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
        map.put("cover", "爱睡觉的木茲");
        map.put("page1", "人物介绍 熊小米 一个聪明、温和、乐观、健康、开朗的小熊。乐于助人，" +
                "乐于交朋友，富有同情心,在感情上容易与其他朋友产生共鸣。" +
                "小豆 熊小米的宠物，时刻陪伴在熊小米身边。" +
                "企鹅志平 熊小米最好的朋友，与熊小米形影不离，非常善良有礼貌，只是有一点点胆小。" +
                "小狮子雷 一只胆小的小狮子，因为叫声像猫咪，经常被人嘲笑。遇到熊小米后，和小米成为好朋友，" +
                "在关键的时候能发出威猛的狮吼" +
                "小驼鹿木茲 一只嗜睡的小驼鹿，看起来有些憨憨傻傻，很有爱心，喜欢大自然，最喜欢做的事情就是" +
                "照顾花花草草" +
                "小象艾莉 一只有力气的小象，遇到困难的狮吼总是虫灾前面，看似粗枝大叶，其实内心情感细腻，有些多愁善感");
        map.put("page2", "呼呼...是谁睡得这么香？原来是我们的好朋友木茲，她最爱睡觉了");
        map.put("page3", "伙伴们在草地上开心地玩跷跷板。可是，木茲玩着玩着居然睡着了。哗哗哗，天空下着倾盆大雨，伙伴们撑着伞、顶着荷叶" +
                "匆匆忙忙地往家跑。木茲，下雨了。小米焦急地喊，可是木茲让在呼呼大睡");
        map.put("page4", "一、二、三......伙伴们开心地玩起了捉迷藏。艾丽藏在粗粗的大树后，雷伪装成美丽的向日葵雷，志平和小豆躲在大石头" +
                "后面，可是木兹躲在哪儿了呢？木兹，木兹......伙伴们着急地到处找她。夜晚来临，皎洁的月光照亮大地，大家还是没找到木兹，原来" +
                "她躲在角落里睡着了。");
        map.put("page5", "冬天下雪的时候，伙伴们在洁白的雪地上快乐地打雪仗。你们看，那边有一个奇怪的雪人。雷喊道。这个雪人居然在打呼噜。呀" +
                "！原来是木兹，她在雪地里睡着了。咚咚......可怕的大暴龙来了");
        map.put("page6", "伙伴们喊着：啊，大暴龙，快跑，快跑......可是木兹睡着了。大暴龙扑过来了，伙伴们急忙抬起熟睡的木兹逃走......虽然" +
                "木兹很爱睡觉，但小伙伴们都很喜欢她，因为她很有爱心。");
        Log.v(TAG, "onCreate1");
        try {
            classifier = new ImageClassifier(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
        //
        startBackgroundThread();
    }


    @Override
    protected void onResume() {
        initCamera();
        super.onResume();
        takePicture();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        handler.removeCallbacks(r);
        stopCamera();
    }


    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
        Intent intent = new Intent();
        intent.setAction(ACTION_STOP_SPEAK);
        sendBroadcast(intent);
        classifier.close();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }


    private void initCamera() {
        //1.OpenCamera
        int cameraIndex = -1;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        if (cameraCount == 1) {
            cameraIndex = 0;
        } else if (cameraCount > 0) {
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(cameraIndex, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraIndex = cameraIndex;
                    break;
                }
            }
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        camera = Camera.open(cameraIndex);
//        camera = Camera.open();
        //2.设置参数
        Camera.Parameters params = camera.getParameters();
        Camera.Parameters parameters = camera.getParameters();
        List<String> FocusModes = parameters.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

//        int rotation = getCameraRotation(cameraIndex);
//        parameters.setRotation(rotation);
        parameters.setRecordingHint(true);
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(new Rect(-100, -100, 100, 100), 600));
            meteringAreas.add(new Camera.Area(new Rect(800, -1000, 1000, -800), 400));
            parameters.setMeteringAreas(meteringAreas);
        }

        Camera.Size fitPreviewSize = null;
        CameraSizeAccessor sizeAccessor = getCameraSizeAccessor();
        int minDiff = Integer.MAX_VALUE;
        for (Camera.Size size : camera.getParameters().getSupportedPreviewSizes()) {
            int diff = (int) Math.sqrt(Math.pow(sizeAccessor.getWidth(size) - 1280, 2) + Math.pow(sizeAccessor.getHeight(size) - 720, 2));
            if (diff < minDiff) {
                minDiff = diff;
                fitPreviewSize = size;
            }
        }
        parameters.setPreviewSize(fitPreviewSize.width, fitPreviewSize.height);
        camera.setParameters(parameters);
        //3.打开预览
        SurfaceHolder holder = sfv.getHolder();
        holder.setFormat(PixelFormat.RGBA_8888);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                //
                Log.v(TAG, "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.v(TAG, "surfaceChanged");

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.v(TAG, "surfaceDestroyed");
            }
        });
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(TAG, e.getMessage());
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
    }


    private void stopCamera() {

        if (camera != null) {
            camera.release();
            camera = null;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    }

    private CameraSizeAccessor getCameraSizeAccessor() {
        return new CameraSizeAccessor(this.getResources().getConfiguration().orientation);
    }

    private void takePicture() {
        Log.v(TAG, "takePicture");
        camera.takePicture(null, null, new Camera.PictureCallback() {
            public void onPictureTaken(byte[] _data, Camera _camera) {
                Log.v(TAG, "onPictureTaken");
                backgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            /* 取得相片 */
                            Bitmap bitmap = BitmapFactory.decodeByteArray(_data, 0,
                                    _data.length);
//                    imgv.setImageBitmap(bitmap);
                            Log.i(TAG, Thread.currentThread()+": startImageClassifier ");
                            classifyFrame(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * 请求存储和相机权限
     */
    private void requestMultiplePermissions() {

//        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
//        String cameraPermission = Manifest.permission.CAMERA;
//
//        int hasStoragePermission = ActivityCompat.checkSelfPermission(this, storagePermission);
//        int hasCameraPermission = ActivityCompat.checkSelfPermission(this, cameraPermission);
//
//        List<String> permissions = new ArrayList<>();
//        if (hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
//            permissions.add(storagePermission);
//        }
//
//        if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
//            permissions.add(cameraPermission);
//        }
//
//        if (!permissions.isEmpty()) {
//            String[] params = permissions.toArray(new String[permissions.size()]);
//            ActivityCompat.requestPermissions(this, params, PERMISSIONS_REQUEST);
//        }
    }

    private class Increment {
        public float percent;
        public float increment;

        public Increment(float percent, float increment) {
            this.percent = percent;
            this.increment = increment;
        }
    }

    private class MaxIncrement {
        public String key;
        public float increment;

        public MaxIncrement(String key, float increment) {
            this.key = key;
            this.increment = increment;
        }
    }

    HashMap<String, Increment> map_increment;

    private void classifyFrame(final Bitmap bitmap) {
        Bitmap croppedBitmap = null;
        try {
            croppedBitmap = getScaleBitmap(bitmap, INPUT_SIZE);
//            String result = classifier.classifyFrame(croppedBitmap);
//            Log.v(TAG, "result = " + result);
//            tv_result.setText(result);
//            handler.postDelayed(r, 1000);
//            String[] results = result.split(":");
//            float percent= Float.parseFloat(results[1]);
//            Log.v(TAG, "results[0] = " + results[0]);
//            Log.v(TAG, "oldKey = " + oldKey);
//            Log.v(TAG, "percent = " + percent);
//            if(!results[0].equals(oldKey) && percent>=0.5f) {
//                Log.v(TAG, "speak content = " + map.get(results[0]));
//                oldKey = results[0];
//                Intent intent = new Intent();
//                intent.putExtra(EXTRA_SPEAK_CONTENT, map.get(results[0]));
//                intent.setAction(ACTION_START_SPEAK);
//                sendBroadcast(intent);
//            }

            maxKey = "";
            maxPercent = 0f;
            MaxIncrement maxIncrement = new MaxIncrement("", 0);
            HashMap<String, Float> mapClassify = classifier.classifyFrame(croppedBitmap);
            bitmap.recycle();
            croppedBitmap.recycle();

            if (map_increment == null) {
                map_increment = new HashMap<String, Increment>();
                Iterator iter = mapClassify.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Float> entry = (Map.Entry) iter.next();
                    map_increment.put(entry.getKey(), new Increment(entry.getValue(), 0));
                    if (maxPercent < entry.getValue()) {
                        maxKey = entry.getKey();
                        maxPercent = entry.getValue();
                    }
                }
            } else {//
                Iterator iter = map_increment.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Increment> entry = (Map.Entry) iter.next();
                    String key = entry.getKey();
                    Increment increment = entry.getValue();
                    if (mapClassify.containsKey(key)) {
                        float percent = mapClassify.get(key);
                        float increment_new = percent - increment.percent;
                        if (increment.increment > 0 && increment_new > 0) {
                            increment.increment += increment_new;
                        } else if (increment.increment < 0 && increment_new < 0) {
                            increment.increment -= increment_new;
                        } else {
                            increment.increment = increment_new;
                        }

                        increment.percent = percent;
                        if (maxIncrement.increment < increment.increment) {
                            maxIncrement.key = key;
                            maxIncrement.increment = increment.increment;
                        }
                        mapClassify.remove(key);
                    } else {
                        iter.remove();
                    }
                }
                iter = mapClassify.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Float> entry = (Map.Entry) iter.next();
                    String key = entry.getKey();
                    Float percent = entry.getValue();
                    Increment increment = new Increment(percent, 0);
                    map_increment.put(key, increment);
                }
            }
            Iterator it = map_increment.entrySet().iterator();
            result = "";
            while (it.hasNext()) {
                Map.Entry<String, Increment> entry = (Map.Entry) it.next();
                result += entry.getKey() + ": " + entry.getValue().percent + ":        " + entry.getValue().increment + "\n";
                if (maxPercent < entry.getValue().percent) {
                    maxKey = entry.getKey();
                    maxPercent = entry.getValue().percent;
                }
            }
            if (maxIncrement.increment > 0.2 && !maxIncrement.key.equals(oldKey)) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SPEAK_CONTENT, map.get(maxIncrement.key));
                intent.setAction(ACTION_START_SPEAK);
                sendBroadcast(intent);
                result += "选中的：" + maxIncrement.key + "\n";
                result += "读的内容:" + map.get(maxIncrement.key) + "\n";
                oldKey = maxIncrement.key;

            } else if (!maxKey.equals(oldKey) && maxPercent > 0.8) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SPEAK_CONTENT, map.get(maxIncrement.key));
                intent.setAction(ACTION_START_SPEAK);
                sendBroadcast(intent);
                result += "选中的：" + maxIncrement.key + "\n";
                result += "读的内容:" + map.get(maxIncrement.key) + "\n";
                oldKey = maxIncrement.key;
            } else {
                Increment increment = map_increment.get(oldKey);
                if (increment != null && increment.increment < 0) {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_STOP_SPEAK);
                    sendBroadcast(intent);
                    result += "停止说话" + "\n";
                } else {
                    result += "选中的：" + oldKey + "\n";
                    result += "读的内容:" + map.get(oldKey) + "\n";
                }

            }

            handler.post(r);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 对图片进行缩放
     *
     * @param bitmap
     * @param size
     * @return
     * @throws IOException
     */
    private static Bitmap getScaleBitmap(Bitmap bitmap, int size) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) size) / width;
        float scaleHeight = ((float) size) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }


    private static class CameraSizeAccessor {
        private int orientation;

        CameraSizeAccessor(int orientation) {
            this.orientation = orientation;
        }

        int getWidth(Camera.Size size) {
            return orientation == Configuration.ORIENTATION_LANDSCAPE ? Math.max(size.width, size.height) : Math.min(size.width, size.height);
        }

        int getHeight(Camera.Size size) {
            return orientation == Configuration.ORIENTATION_LANDSCAPE ? Math.min(size.width, size.height) : Math.max(size.width, size.height);
        }
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
//
//    /** Takes photos and classify them periodically. */
//    private Runnable periodicClassify =
//            new Runnable() {
//                @Override
//                public void run() {
//                    synchronized (lock) {
//                        if (runClassifier) {
//                            classifyFrame();
//                        }
//                    }
//                    backgroundHandler.post(periodicClassify);
//                }
//            };
}

