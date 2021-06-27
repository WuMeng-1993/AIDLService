package com.wm.aidlservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WuMeng
 */
public class BookManagerService extends Service {

    private static final String TAG = "WU-MENG";

    /**
     * 图书列表
     */
    private final List<Book> mBookList = new ArrayList<>();

    /**
     * 接口实例的队列
     */
    private RemoteCallbackList<IOnNewBookArrivedListener> listeners = new RemoteCallbackList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book("Android开发艺术探究", 00001));
        mBookList.add(new Book("第一行代码", 00002));
        new Thread(new ServiceWorker()).start();
    }

    private Binder mBinder = new IBookManager.Stub() {

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String packageName = null;
            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
            if (packages != null && packages.length > 0) {
                packageName = packages[0];
            }

            if (packageName == null) {
                return false;
            }

            boolean result = checkPermission(BookManagerService.this,"com.wm.aidlservice.permission.ACCESS_BOOK_SERVICE",packageName);
            if (!result) {
                Log.d(TAG,"授权失败");
                return false;
            }

            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            synchronized (this) {
                if (book != null && !mBookList.contains(book)) {
                    mBookList.add(book);
                    Log.d(TAG, "调用服务端的addBook()");
                }
            }
        }

        @Override
        public List<Book> getBookList() throws RemoteException {
            Log.d(TAG, "调用服务端的getBookList()");
            Log.d(TAG,"getBookList()线程是" + Thread.currentThread().getName());
            return mBookList;
        }

        // 注册
        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            synchronized (this) {
                listeners.register(listener);
                Log.d(TAG, "注册成功");
            }
        }

        // 解注册
        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            synchronized (this) {
                boolean value = listeners.unregister(listener);
                if (value) {
                    Log.d(TAG,"解注册成功");
                }
            }
        }
    };

    /**
     * 检查权限
     * @param mContext
     * @param permissionName
     * @param packageName
     * @return
     */
    private boolean checkPermission(Context mContext,String permissionName,String packageName) {
        int permission = mContext.getPackageManager().checkPermission(permissionName,packageName);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class ServiceWorker implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);
                    Book book = new Book("添加图书" + (mBookList.size() + 1), mBookList.size() + 1);
                    onNewBookArrived(book);
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onNewBookArrived(Book book) throws RemoteException {
        mBookList.add(book);
        final int N = listeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener listener = listeners.getBroadcastItem(i);
            if (listener != null) {
                listener.onNewBookArrived(book);
            }
        }
        listeners.finishBroadcast();
    }

}