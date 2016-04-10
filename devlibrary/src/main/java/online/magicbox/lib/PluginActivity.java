package online.magicbox.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import online.magicebox.devlibrary.R;


/**
 * Created by george.yang on 2016-3-30.
 */
public class PluginActivity extends Activity {
    private String packageName = null, animType = null, className = null, version = null;
    public Object mSlice;//切片
    private Context mContext;//

    private static String ACTION = "online.magicbox.plugin";
    private static String SCHEME = "magicbox";

    public static void init(String action, String scheme) {
        PluginActivity.ACTION = action;
        PluginActivity.SCHEME = scheme;
    }

    private static Context getPluginContent(Context context,String packageName,String version) {
        try {
//            String pluginPath = AssetUtils.copyAsset(this,String.format("%s_%s.apk",new Object[]{packageName,version}), getFilesDir());
            String pluginPath = new File(context.getFilesDir(),String.format("%s_%s.apk",new Object[]{packageName,version})).getAbsolutePath();

            Class pluginContextClass = context.getClassLoader().loadClass("online.magicbox.app.PluginContext");
            Constructor<?> localConstructor = pluginContextClass.getConstructor(new Class[]{Context.class});
            Object pluginContext = localConstructor.newInstance(new Object[]{context});
            Method loadResourcesMethod = pluginContextClass.getMethod("loadResources",new Class[]{String.class,String.class});
            loadResourcesMethod.invoke(pluginContext,new Object[]{pluginPath,packageName});
            return (Context)pluginContext;

//            Log.i("test","load plugin:" + pluginPath);
//            PluginContext proxyContext = new PluginContext(context);
//            proxyContext.loadResources(pluginPath,packageName);
//            return proxyContext;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }

    //http://blog.csdn.net/cauchyweierstrass/article/details/51087198
    private static void replaceClassLoader(String tagPackage,DexClassLoader loader){
        try {
            Class clazz_Ath = Class.forName("android.app.ActivityThread");
            Class clazz_LApk = Class.forName("android.app.LoadedApk");

            Object currentActivityThread = clazz_Ath.getMethod("currentActivityThread").invoke(null);
            Field field1 = clazz_Ath.getDeclaredField("mPackages");
            field1.setAccessible(true);
            Map mPackages = (Map)field1.get(currentActivityThread);

            WeakReference ref = (WeakReference) mPackages.get(tagPackage);
            Field field2 = clazz_LApk.getDeclaredField("mClassLoader");
            field2.setAccessible(true);
            field2.set(ref.get(), loader);
        } catch (Exception e){
            System.out.println("-------------------------------------" + "click");
            e.printStackTrace();
        }
    }

    private static final List<PluginActivity> allActivity = new ArrayList<>();

    public static void pushMessage(int type, Object object) {
        Log.i("test", "push:" + object);
        for (PluginActivity activity : allActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (activity.isDestroyed()) {
                    Log.i("test", "isDestroyed:" + activity);
                    continue;
                }
            }
            if (activity.isFinishing()) {
                Log.i("test", "isFinishing:" + activity);
                continue;
            }

            callMethodByCache(activity.mSlice, "onReceiveMessage", new Class[]{int.class, Object.class}, new Object[]{type, object});
        }
    }


    public static Intent buildIntent(Context context,Class clazz) {
        return buildIntent(context,clazz.getPackage().getName(), clazz.getSimpleName(), PluginConfig.pluginVersion);
    }

    public static Intent buildIntent(Context context,Class clazz, String animType) {
        HashMap<String, String> params = new HashMap<>();
        params.put("animType", animType);
        return buildIntent(context,clazz.getPackage().getName(), clazz.getSimpleName(), params);
    }

    public static Intent buildIntent(Context context,Class clazz, Map<String, String> params) {
        return buildIntent(context,clazz.getPackage().getName(), clazz.getSimpleName(), params);
    }

    public static Intent buildIntent(Context context,String packageName, String className,String version) {
        HashMap<String, String> params = new HashMap<>();
        params.put("animType", "System");
        params.put("version", version);
        return buildIntent(context,packageName, className, params);
    }

    public static Intent buildIntent(Context context,String packageName, String className,String animType,String version) {
        HashMap<String, String> params = new HashMap<>();
        params.put("animType", animType);
        params.put("version", version);
        return buildIntent(context,packageName, className, params);
    }

    public static Intent buildIntent(Context context,String packageName, String className, Map<String, String> params) {
        if (params==null) {
            params = new HashMap<>();
        }
        if (!params.containsKey("animType")) {
            params.put("animType",PluginConfig.System);
        }
        if (!params.containsKey("version")) {
            params.put("version",PluginConfig.pluginVersion);
        }


        Log.i("test","buildIntent=====");
        Log.i("test","context:" + context);
        Log.i("test","loder:" + context.getClassLoader());
        Log.i("test","packageName:" + packageName);
        Log.i("test","className:" + className);
        for (String key:params.keySet()) {
            Log.i("test","key:" + key + ">>" + params.get(key));
        }
        Log.i("test","buildIntent end=====");

        if ("ProxyActivity".equals(className)) {
            String version = params.get("version");
            Context plugInContent = getPluginContent(context,packageName,version);
            ClassLoader classLoader =  plugInContent.getClassLoader();
            Log.i("test","buildIntent classLoader:" + classLoader);
            if (!(classLoader instanceof DexClassLoader)) {
                try {
                    Class<?> activity = classLoader.loadClass("online.magicbox.app.ProxyActivity");
                    Intent intent = new Intent(context, activity);
                    return intent;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                replaceClassLoader(context.getPackageName(), (DexClassLoader) classLoader);
                try {
                    Class<?> activity = classLoader.loadClass("online.magicbox.ProxyActivity");
                    Intent intent = new Intent(context, activity);
                    return intent;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Uri.Builder builder = new Uri.Builder().scheme(SCHEME).path(packageName + "." + className);
        if (params != null) {
            for (String key : params.keySet()) {
                builder.appendQueryParameter(key, params.get(key));
            }
        }
        Uri uri = builder.build();
        Intent intent = new Intent(ACTION);
        intent.setData(uri);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Intent intent = getIntent();
            if (intent == null || intent.getData() == null) {
                Toast.makeText(this, "缺少参数", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Uri uri = intent.getData();

            String path = uri.getPath();
            packageName = path.substring(1, path.lastIndexOf('.'));
            Log.i("test", "packageName:" + packageName);
            if (TextUtils.isEmpty(packageName)) {
                Toast.makeText(this, "未指定插件名", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            className = path.substring(path.lastIndexOf('.') + 1, path.length());
            Log.i("test", "className:" + className);
            if (TextUtils.isEmpty(className)) {
                className = "MainFragment";
            }

            version = uri.getQueryParameter("version");
            if (TextUtils.isEmpty(version)) {
                version = PluginConfig.pluginVersion;
            }

            animType = uri.getQueryParameter("animType");
            if (TextUtils.isEmpty(animType)) {
                animType = PluginConfig.System;
            }

            mContext = getPluginContent(this,packageName,className);

            Class pluginActivityClass = mContext.getClassLoader().loadClass(String.format("%s.%s", new Object[]{packageName, className}));
            Constructor<?> localConstructor = pluginActivityClass.getConstructor(new Class[]{Context.class,Object.class});
            mSlice = localConstructor.newInstance(new Object[]{mContext,PluginActivity.this});

        } catch (Exception e) {
            Toast.makeText(this, "加载失败:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("demo", Log.getStackTraceString(e).toString());
            e.printStackTrace();

            finish();
        }

        Log.d("demo", "animType:" + animType);

        loadAnim(false);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            this.setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        } else if (android.os.Build.VERSION.SDK_INT >= 13) {
            this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
        } else {
            this.setTheme(android.R.style.Theme_Black_NoTitleBar);
        }

        super.onCreate(savedInstanceState);

        allActivity.add(this);
        callMethodByCache(mSlice, "onCreate", new Class[]{Bundle.class}, new Object[]{savedInstanceState});
    }

    @Override
    protected void onResume() {
        super.onResume();
        callMethodByCache(mSlice, "onResume", new Class[]{}, new Object[]{});
    }

    private static final Map<String, Method> methodCache = new WeakHashMap<>();
    private static Object callMethodByCache(Object receiver, String methodName, Class[] parameterTypes, Object[] args) {
        try {
            String key = receiver.getClass() + "#" + methodName + "&" + Arrays.toString(parameterTypes);
            Method method = methodCache.get(key);
            if (method == null) {
                method = receiver.getClass().getMethod(methodName, parameterTypes);
                methodCache.put(key, method);
            }
            return method.invoke(receiver, args);
        } catch (Exception e) {
        }
        return null;
    }

    private static Class findClass(Class clazz,Class tagClass) {
        if (clazz==tagClass) {
            return clazz;
        } else {
            Class sup = clazz.getSuperclass();
            if (sup!=null) {
                return findClass(sup,tagClass);
            }
        }
        return null;
    }

    @Override
    public void finish() {
        callMethodByCache(mSlice, "finish", new Class[]{}, new Object[]{});
        super.finish();
        loadAnim(true);
    }

    /**
     * 虚拟方法,如果fragment有boolean onBackPressed()方法，调用
     */
    @Override
    public void onBackPressed() {
        Object ret = callMethodByCache(mSlice, "onBackPressed", new Class[]{}, new Object[]{});
        if (ret!=null) {
            try {
                if ((boolean)ret) {
                    return;
                }
            } catch (Exception e) {

            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        callMethodByCache(mSlice, "onDestroy", new Class[]{}, new Object[]{});
        super.onDestroy();
    }

    private void loadAnim(boolean isExit) {
        switch (animType) {
            case PluginConfig.LeftInRightOut:
                if (isExit) {
                    overridePendingTransition(R.anim.right_in, R.anim.right_out);
                } else {
                    overridePendingTransition(R.anim.left_in, R.anim.left_out);
                }
                break;
            case PluginConfig.AlphaShow:
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case PluginConfig.TopOut:
                overridePendingTransition(R.anim.push_in_down, R.anim.push_no_ani);
                break;
            case PluginConfig.BottomInTopOut:
                overridePendingTransition(R.anim.push_in_down, R.anim.push_out_down);
                break;
            case PluginConfig.ZoomShow:
                overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
                break;
            case PluginConfig.NONE:
                overridePendingTransition(0, 0);
                break;
            case PluginConfig.System:
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean bool = false;
        try {
            bool = (boolean) callMethodByCache(mSlice, "onCreateOptionsMenu", new Class[]{Menu.class}, new Object[]{menu});
        } catch (Exception e) {

        }
        if (!bool) {
            return super.onCreateOptionsMenu(menu);
        }
        return bool;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        boolean bool = false;
        try {
            bool = (boolean) callMethodByCache(mSlice, "onOptionsMenuClosed", new Class[]{Menu.class}, new Object[]{menu});
        } catch (Exception e) {

        }
        if (!bool) {
            super.onOptionsMenuClosed(menu);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
       callMethodByCache(mSlice, "onSaveInstanceState", new Class[]{Bundle.class}, new Object[]{outState});
    }


    @Override
    public void onStart() {
        super.onStart();
        callMethodByCache(mSlice, "onStart", new Class[]{}, new Object[]{});
    }

    @Override
    public void onStop() {
        super.onStop();
        callMethodByCache(mSlice, "onStop", new Class[]{}, new Object[]{});
    }
}