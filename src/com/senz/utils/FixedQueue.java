package com.senz.utils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/***********************************************************************************************************************
 * @ClassName:   FixedQueue
 * @Author:      Woodie
 * @CreateAt:    Sat, Nov 20, 2014
 * @Description:
 ***********************************************************************************************************************/

public class FixedQueue<T> extends LinkedList<T> implements FIFO<T> {

    private int maxSize = Integer.MAX_VALUE;
    private final Object synObj = new Object();

    public FixedQueue() {
        super();
    }

    public FixedQueue(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    @Override
    public T addLastSafe(T addLast) {
        synchronized (synObj) {
            T head = null;
            while (size() >= maxSize) {
                head = poll();
            }
            addLast(addLast);
            return head;
        }
    }

    @Override
    public T pollSafe() {
        synchronized (synObj) {
            return poll();
        }
    }

    @Override
    public List<T> setMaxSize(int maxSize) {
        List<T> list = null;
        if (maxSize < this.maxSize) {
            list = new ArrayList<T>();
            synchronized (synObj) {
                while (size() > maxSize) {
                    list.add(poll());
                }
            }
        }
        this.maxSize = maxSize;
        return list;
    }

    @Override
    public int getMaxSize() {
        return this.maxSize;
    }
}
