package com.rfidresearchgroup.fragment.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rfidresearchgroup.binder.BannerImageViewBinder;
import com.rfidresearchgroup.binder.DeviceInfoViewBinder;
import com.rfidresearchgroup.binder.TitleTextViewBinder;

import java.util.ArrayList;

import com.rfidresearchgroup.common.util.AppUtil;
import com.rfidresearchgroup.mifare.StdMifareImpl;
import com.rfidresearchgroup.devices.PN53X;
import com.rfidresearchgroup.activities.chameleon.ChameleonGUIActivity;
import com.rfidresearchgroup.activities.connect.Acr122uHkUsbConnectActivity;
import com.rfidresearchgroup.activities.connect.ChameleonUsb2UartConnectActivity;
import com.rfidresearchgroup.activities.connect.PN532UartConnectActivity;
import com.rfidresearchgroup.activities.connect.PN53XUsbBulkTransferActivity;
import com.rfidresearchgroup.activities.connect.Proxmark3Rdv4RRGConnectActivity;
import com.rfidresearchgroup.activities.main.GeneralNfcDeviceMain;
import com.rfidresearchgroup.activities.main.PN53XNfcMain;
import com.rfidresearchgroup.activities.proxmark3.rdv4_rrg.Proxmark3NewTerminalInitActivity;
import com.rfidresearchgroup.javabean.BannerBean;
import com.rfidresearchgroup.javabean.DeviceInfoBean;
import com.rfidresearchgroup.javabean.TitleTextBean;
import com.rfidresearchgroup.rfidtools.R;

import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;

/**
 * UI redesign starts on July 29, 2019!
 *
 * @author DXL
 */
public class AppMainDevicesFragment extends BaseFragment {

    public static String ACTION_CONNECTION_STATE_UPDATE = "AppMainDevicesFragment.connection_state_update";
    public static String EXTRA_CONNECTION_STATE = "state";

    private boolean isBackPressed = false;
    private boolean isConnected = false;

    private MultiTypeAdapter multiTypeAdapter;
    private Items deviceItems;

    private DeviceInfoBean deviceInfoBean;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_CONNECTION_STATE_UPDATE.equals(action)) {
                isConnected = intent.getBooleanExtra(EXTRA_CONNECTION_STATE, false);
                updateDeviceStatus(isConnected);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.act_app_devices, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    receiver, new IntentFilter(ACTION_CONNECTION_STATE_UPDATE)
            );
        }

        multiTypeAdapter = new MultiTypeAdapter();
        deviceItems = new Items();

        initDeviceList(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews(view);
        updateDeviceStatus(isConnected);
    }

    private void initViews(View view) {
        RecyclerView rvMainContainer = view.findViewById(R.id.rvMainContainer);
        multiTypeAdapter.register(DeviceInfoBean.class, new DeviceInfoViewBinder());
        multiTypeAdapter.register(TitleTextBean.class, new TitleTextViewBinder());
        multiTypeAdapter.register(BannerBean.class, new BannerImageViewBinder());
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Object type = deviceItems.get(position);
                if (type instanceof BannerBean) {
                    return 2;
                }
                if (type instanceof DeviceInfoBean) {
                    return 1;
                }
                return 1;
            }
        };
        GridLayoutManager gridLayoutManager = new GridLayoutManager(view.getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(spanSizeLookup);
        rvMainContainer.setLayoutManager(gridLayoutManager);
        multiTypeAdapter.setItems(deviceItems);
        rvMainContainer.setAdapter(multiTypeAdapter);
    }

    private void initDeviceList(Context context) {
        // init device list!
        deviceItems.add(new DeviceInfoBean(PN53X.NAME.PN532, R.drawable.pn532core) {
            @Override
            public void onClick() {
                deviceInfoBean = this;
                connectOrGotoFunctionMain(
                        PN532UartConnectActivity.class,
                        PN53XNfcMain.class
                );
            }
        });
        multiTypeAdapter.notifyDataSetChanged();
    }

    private void updateDeviceStatus(boolean status) {
        // update device status to bean!
        if (status) {
            if (deviceInfoBean != null) {
                deviceInfoBean.setEnable(status);
            }
            for (Object tmp : deviceItems) {
                if (tmp instanceof DeviceInfoBean) {
                    if (tmp != deviceInfoBean) {
                        ((DeviceInfoBean) tmp).setEnable(false);
                    }
                }
            }
        } else {
            for (Object tmp : deviceItems) {
                if (tmp instanceof DeviceInfoBean) {
                    ((DeviceInfoBean) tmp).setEnable(true);
                }
            }
        }
        // update view from adapter!
        multiTypeAdapter.notifyDataSetChanged();
    }

    private void connectOrGotoFunctionMain(Class connPage, Class main) {
        if (isConnected) {
            startActivity(new Intent(getContext(), main));
        } else {
            startActivity(new Intent(getContext(), connPage));
        }
    }

    public void onBackPressed() {
        isBackPressed = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBackPressed) {
            AppUtil.getInstance().finishAll();
        }
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        }
    }

    class PN53XDeviceInfoBean extends DeviceInfoBean {

        PN53XDeviceInfoBean(@NonNull String name, int icon) {
            super(name, icon);
        }

        @Override
        public void onClick() {
            deviceInfoBean = this;
            if (isConnected) {
                startActivity(
                        new Intent(getContext(), PN53XNfcMain.class)
                                .putExtra("name", getName())
                );
            } else {
                startActivity(new Intent(getContext(), PN53XUsbBulkTransferActivity.class));
            }
        }
    }
}
