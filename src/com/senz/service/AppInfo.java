package com.senz.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import com.senz.core.App;
import com.senz.utils.L;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by woodie on 14/12/5.
 */
public class AppInfo {

    public ArrayList<App> appList = new ArrayList<App>();

    public AppInfo(Context ctx)
    {
        L.i("=== APP INFO ===");
        List<PackageInfo> packages = ctx.getPackageManager().getInstalledPackages(0);
        for(PackageInfo packageInfo : packages) {
            App tmp =new App();
            tmp.setAppName(packageInfo.applicationInfo.loadLabel(ctx.getPackageManager()).toString());
            tmp.setPackageName(packageInfo.packageName);
            tmp.setVersionName(packageInfo.versionName);
            tmp.setVersionCode(packageInfo.versionCode);
            if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0)
            {
                tmp.print();
                appList.add(tmp);//If it is non system app, then add it into the list
            }
        }
    }


}
