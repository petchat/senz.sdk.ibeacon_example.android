package com.senz.filter.implementations;

import android.os.SystemClock;
import android.content.Context;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.senz.core.Senz;
import com.senz.filter.FilterImplementation;

public class MinInterval implements FilterImplementation {
    HashMap<Senz, Long> current, previous;
    long lastSwapped;
    final long expire;

    public MinInterval(long e) {
        this.expire = e;
    }

    @Override
    public void init(Context context) {
        this.current = new HashMap<Senz, Long>();
        this.previous = new HashMap<Senz, Long>();
        this.lastSwapped = SystemClock.uptimeMillis();
    }

    private void swap() {
        long now = SystemClock.uptimeMillis();
        if (now - lastSwapped <= expire)
            return;
        HashMap t = previous;
        previous = current;
        current = t;
        current.clear();
        lastSwapped = now;
    }

    public Set<Senz> filter(Set<Senz> senzes) {
        HashSet<Senz> result = new HashSet<Senz>();
        Long lastVisited;
        long now = SystemClock.uptimeMillis();

        swap();
        for (Senz senz : senzes) {
            lastVisited = this.current.get(senz);
            if (lastVisited != null)
                continue;
            lastVisited = this.previous.get(senz);
            if (lastVisited != null)
                if (now - lastVisited <= expire)
                    continue;
                else
                    this.previous.remove(senz);
            this.current.put(senz, Long.valueOf(now));
            result.add(senz);
        }

        return result;
    }

    public static final FilterImplementation.FilterGetter GETTER = new FilterImplementation.FilterGetter() {
        @Override
        public FilterImplementation get() {
            return new MinInterval(TimeUnit.MINUTES.toMillis(30));
        }
    };
}
