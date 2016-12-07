package com.yunos.alicontacts.dialpad.smartsearch;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.LinkedList;

public class NameConvertWorker {
    private static final String TAG = "NameConvertWorker";

    public static final String CHANGED_CONTACT_URI = "changed.contact.uri";
    private static final NameConvertWorker sInstance = new NameConvertWorker();

    private PersistWorker mPersister = null;

    private PersistTaskThread mPersistTaskThread = null;

    private ContactObserver mContactObserver = null;

    private Context mAppContext = null;

    private static final int NAMEWORKER_QUERY_CONTACT_DELAY = 350;
    private static Handler mHandler = new Handler();

    public static synchronized NameConvertWorker getInstance() {
        return sInstance;
    }

    /* NOTE: This thread will cost long time to finish if there are many contacts.
     * If it is doing in the background, it will effect the listview scroll of the calllog and
     * the startup of the Contacts. In some case, it will block the UI several seconds. So use
     * the pause and resume function to control the thread
     */
    // mPause to indicate if pause the NameConvertThread.
    private static volatile boolean mPause;
    private class PersistTaskThread extends Thread {

        private volatile boolean mToQuit = false;
        public PersistTaskThread() {
            super("PERSIST_TASK_THREAD");
        }

        @Override
        public void run() {
            Bundle req = null;
            mRequests.add(null);
            while (true) {
                try {
                    synchronized (mRequests) {
                        // If no request, just wait
                        while (mRequests.isEmpty() || mPause) {
                            mRequests.wait();
                        }
                        // Clear the queue, handle the last request.
                        req = mRequests.getLast();
                        mRequests.clear();
                    }
                } catch (InterruptedException ie) {
                    // ignore interrupt, it is caused by stop processing
                    Log.d(TAG, "PersistTaskThread.run: interrupted.");
                }
                if (mToQuit) {
                    return;
                }
                PinyinSearch.setPersistInterrupted(false);
                doContactsChanges(req);
            }
        }

        private void stopProcessing() {
            mToQuit = true;
        }
    }

    public void checkPause() {
        if (mPause) {
            try {
                synchronized (mRequests) {
                    while (mPause)
                        mRequests.wait();
                }
            } catch (InterruptedException ie) {
                Log.d(TAG, "checkPause: interrupted wait.");
            }
        }
    }

    public static void pause() {
        mPause = true;
    }

    public static void resume() {
        if (mPause) {
            mPause = false;
            synchronized (mRequests) {
                try {
                    mRequests.notifyAll();
                } catch (Exception e) {
                    Log.d(TAG, "resume: Got exception during notifyAll, "+e, e);
                }
            }
        }
    }

    private static final LinkedList<Bundle> mRequests = new LinkedList<Bundle>();
    protected void executeRequest(Bundle bundle) {
        synchronized (mRequests) {
            PinyinSearch.setPersistInterrupted(true);
            mRequests.add(bundle);
            mRequests.notifyAll();
        }
    }

    private void doContactsChanges(Bundle bundle) {
        Log.d(TAG, "doContactsChanges:");
        mPersister.doChangesList(bundle, this);
    }

    private volatile boolean inited = false;
    public boolean isInited() {
        return inited;
    }
    public synchronized void init(Context context) {
        Log.d(TAG, "init: inited="+inited);
        if (inited) {
            return;
        }
        mAppContext = context.getApplicationContext();
        mPersister = new PersistWorker(mAppContext);
        mContactObserver = new ContactObserver(this);
        mAppContext.getContentResolver().registerContentObserver(
                ContactsContract.Data.CONTENT_URI, false, mContactObserver);
        startRequestProcessing();
        inited = true;
    }

    private void startRequestProcessing() {
        // Idempotence... if a thread is already started, don't start another.
        if (mPersistTaskThread != null) return;

        mPersistTaskThread = new PersistTaskThread();
        mPersistTaskThread.setPriority(Thread.MIN_PRIORITY);
        mPersistTaskThread.start();
    }

    public synchronized void destroy() {
        Log.d(TAG, "destroy:");
        if (!inited) {
            return;
        }
        mAppContext.getContentResolver().unregisterContentObserver(mContactObserver);
        stopRequestProcessing();
        inited = false;
    }

    private void stopRequestProcessing() {
        if (mPersistTaskThread == null) {
            return;
        }
        mPersistTaskThread.interrupt();
        mPersistTaskThread.stopProcessing();
        mPersistTaskThread = null;
    }

    private static class ContactObserver extends ContentObserver {

        private NameConvertWorker mWorker;
        public ContactObserver(NameConvertWorker worker) {
            super(null);
            mWorker = worker;
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange,final Uri uri) {
            Log.d(TAG, "onChange: The changed Uri is "+uri+"; selfChange="+selfChange);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (uri != null) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(CHANGED_CONTACT_URI, uri);
                        mWorker.executeRequest(bundle);
                    } else {
                        mWorker.executeRequest(null);
                    }
                }
            }, NAMEWORKER_QUERY_CONTACT_DELAY);
        }
    }

}
