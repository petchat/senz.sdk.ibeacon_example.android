package com.senz.filter;

import android.content.Context;
import java.util.Set;
import com.senz.core.Senz;

public interface FilterImplementation {
    public void init(Context context);
    public Set<Senz> filter(Set<Senz> senz);

    public interface FilterGetter {
        public FilterImplementation get();
    }
}
