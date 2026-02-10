package com.rfidresearchgroup.fragment.connect;

import com.rfidresearchgroup.models.AbstractDeviceModel;
import com.rfidresearchgroup.view.DeviceExistsView;

import java.util.ArrayList;

import com.rfidresearchgroup.javabean.DevBean;
import com.rfidresearchgroup.presenter.DeviceExistsPresenter;

public class DeviceConnectAllFragment
        extends DeviceConnectFragment
        implements DeviceExistsView {

    //重新定义类型!
    protected ArrayList<DeviceExistsPresenter> presenters = new ArrayList<>();

    @Override
    protected void initResource() {
        super.initResource();
        android.util.Log.d("DeviceConnectAll", "initResource() called, models=" + (models != null ? models.length : "null"));
        if (models != null) {
            for (AbstractDeviceModel model : models) {
                android.util.Log.d("DeviceConnectAll", "Processing model: " + model.getClass().getName());
                DeviceExistsPresenter presenter = new DeviceExistsPresenter(model);
                presenter.attachSubView(this);
                presenter.attachView(this);
                presenters.add(presenter);
                //registriere Broadcasts für USB-Geräte
                android.util.Log.d("DeviceConnectAll", "Calling presenter.register()");
                presenter.register();
                //直接取出相关的已存在设备即可!
                android.util.Log.d("DeviceConnectAll", "Calling presenter.existsDevList()");
                presenter.existsDevList();
            }
        } else {
            throw new RuntimeException("models no init exception!");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (DeviceExistsPresenter presenter : presenters) {
            //销毁视图绑定!
            presenter.unregister();
            presenter.detachSubView();
            presenter.detachView();
        }
    }

    @Override
    protected void onDiscovery() {
        for (DeviceExistsPresenter presenter : presenters) {
            //triggere discovery für USB-Geräte
            presenter.discovery(getContext());
            presenter.existsDevList();
        }
    }

    @Override
    public void showExistsDev(DevBean[] devList) {
        super.showExistsDev(devList);
        //判断需不需要显示或者隐藏填充视图!
        showOrDismissEmptyView();
    }
}
