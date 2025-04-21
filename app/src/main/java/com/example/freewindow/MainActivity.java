package com.example.freewindow;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodReplacement;

public class MainActivity extends AppCompatActivity {

    private LinkedHashMap<String, Bundle> bundleCache = new LinkedHashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        try {
            //绕过白名单
            Method method = HiddenApiBypass.getDeclaredMethod(
                    Class.forName("android.util.MiuiMultiWindowUtils"),
                    "checkAuthority",
                    Context.class
            );
            Pine.hook(
                    method,
                    new MethodReplacement() {
                        @Override
                        protected Object replaceCall(Pine.CallFrame callFrame) throws Throwable {
                            return true;
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        findViewById(R.id.textView).setOnClickListener(v -> {
            Rect bounds = new Rect(
                    0,
                    0,
                    720, 1280
            );
            bounds.offset(
                    300, 300
            );
            launchApp(
                    getApplicationInfo(),
                    bounds
            );
        });
    }

    //以小窗方式启动应用
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void launchApp(ApplicationInfo applicationInfo, Rect bounds) {
        //发送一次广播即可
        if (!bundleCache.containsKey(applicationInfo.packageName)) {
            Bundle bundle = new Bundle();
            bundle.putString("packageName", applicationInfo.packageName);
            bundle.putInt("userId", applicationInfo.uid + 100);//uid任意即可，不重复就行
            Intent intent = new Intent("miui.intent.action_launch_fullscreen_from_freeform");
            intent.putExtras(bundle);
            sendBroadcast(intent);
            bundleCache.put(applicationInfo.packageName, bundle);
        }
        ActivityOptions options = getActivityOptions(applicationInfo.packageName);
        if (bounds != null) {
            //设置小窗位置和宽高
            options.setLaunchBounds(bounds);
        }
        startActivity(getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName), options.toBundle());
    }

    //构造一个ActivityOptions
    @RequiresApi(api = Build.VERSION_CODES.P)
    public ActivityOptions getActivityOptions(String packageName) {
        ActivityOptions base = ActivityOptions.makeBasic();
        try {
            HiddenApiBypass.invoke(ActivityOptions.class, base, "setLaunchWindowingMode", 5);
            HiddenApiBypass.invoke(ActivityOptions.class, base, "setMiuiConfigFlag", 2);

            Method injector = HiddenApiBypass.getDeclaredMethod(ActivityOptions.class, "getActivityOptionsInjector");
            injector.setAccessible(true);
            Object injectorResult = injector.invoke(base);
            Class<?> clazz = injectorResult != null ? injectorResult.getClass() : null;
            if (clazz != null) {
                try {
                    //设置小窗默认缩放大小
                    HiddenApiBypass.invoke(clazz, injector.invoke(base), "setFreeformScale", 0.7f);
                } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
                try {
                    //感觉没效果
                    HiddenApiBypass.invoke(clazz, injector.invoke(base), "setNormalFreeForm", true);
                } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
                try {
                    //感觉没效果
                    HiddenApiBypass.invoke(clazz, injector.invoke(base), "setUseCustomLaunchBounds", false);
                } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException |
                 java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
        return base;
    }

}