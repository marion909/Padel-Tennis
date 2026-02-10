package com.rfidresearchgroup.activities.tools;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.rfidresearchgroup.activities.main.BaseActivity;
import com.rfidresearchgroup.fragment.init.InitFragment;
import com.rfidresearchgroup.fragment.init.LoginFragment;
import com.rfidresearchgroup.rfidtools.R;
import com.rfidresearchgroup.util.Commons;

import com.rfidresearchgroup.common.application.App;
import com.rfidresearchgroup.common.util.AppUtil;
import com.rfidresearchgroup.common.util.LanguageUtil;

import com.rfidresearchgroup.common.implement.PermissionCallback;
import com.rfidresearchgroup.common.util.FragmentUtil;
import com.rfidresearchgroup.common.util.PermissionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/*
 * 登陆界面，，用于登陆用户系统，初始化环境，对于用户的验证，都放在这里实现
 */
public class LoginActivity
        extends BaseActivity {

    private PermissionUtil permissionUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 多语言适配!
        App app = AppUtil.getInstance().getApp();
        app.setCallback(new App.ApplicationCallback() {
            @Override
            public Context onAttachBaseContext(Context context) {
                String language = Commons.getLanguage();
                if (language.equals("auto")) {
                    //如果value = auto，则设置为跟随系统!
                    return context;
                } else {
                    //否则国际化!
                    return LanguageUtil.setAppLanguage(context, language);
                }
            }
        });

        // Zum Hauptmenü weiterleiten
        startActivity(new android.content.Intent(this, com.rfidresearchgroup.activities.main.MainMenuActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean result = true;
        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                result = false;
            }
            //在这里再次检查权限，如果所有的权限都通过的话则可以直接进入!
            permissionUtil.checks();
        }
        //如果所有的权限都有才能让他初始化
        if (!result) {
            Toast.makeText(this, R.string.tips_permission_request_failed, Toast.LENGTH_SHORT).show();
            //执行finish，结束当前act，直接退出初始化!!!
            finish();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
