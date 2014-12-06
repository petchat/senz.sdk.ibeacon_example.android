package com.senz.utils;

import java.lang.Runnable;
import java.lang.Thread;
import com.senz.utils.L;

public class Asyncfied {
    // It's a Interface for user to define their own callback.
    public interface Asyncfiable<T> {
        public T runAndReturn() throws Exception;
        public void onReturn(T t);
        public void onError(Exception e);
    }

    private Asyncfiable mAsyncfied;

    // Check Definition of All callbacks(Asyncfiable<T>), and save it.
    public Asyncfied(Asyncfiable a) {
        // Check whether or not has user defined interface Asyncfiable<T>
        if (a == null)
            throw new NullPointerException();
        // copy the Asyncfiable<T> to private variable mAsyncfied.
        this.mAsyncfied = a;
    }

    // Create a new thread, and do:
    // - try mAsyncfied's runAndReturn(), and it will return ret.
    // - if throw a error, then excute mAsyncfied's onError(e)
    // - finally, return mAsyncfied's onReturn(ret)
    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Object ret;
                try {
                    //L.i(" --- Async started");
                    ret = Asyncfied.this.mAsyncfied.runAndReturn();
                }
                catch (Exception e) {
                    //L.e(" --- Async errored");
                    Asyncfied.this.mAsyncfied.onError(e);
                    return;
                }
                //L.i(" --- Async returned");
                Asyncfied.this.mAsyncfied.onReturn(ret);
            }
        }).start();
    }

    // It's a user-interface, User call this function and
    // do the callbacks(runAndReturn(), onReturn(T t), onError(Exception e)) in a defined order.
    public static void runAsyncfiable(Asyncfiable a) {
        Asyncfied as = new Asyncfied(a);
        as.run();
    }
}
