package com.wm.aidlservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WuMeng
 */
public class BookManagerService extends Service {

    private static final String TAG = "WU-MENG";

    private List<Book> mBookList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book("Android开发艺术探究",00001));
        mBookList.add(new Book("第一行代码",00002));
    }

    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public void addBook(Book book) throws RemoteException {
            synchronized(this) {
                if (book != null && !mBookList.contains(book)) {
                    mBookList.add(book);
                    Log.d(TAG,"调用服务端的addBook()");
                }
            }
        }

        @Override
        public List<Book> getBookList() throws RemoteException {
            Log.d(TAG,"调用服务端的getBookList()");
            return mBookList;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }
}