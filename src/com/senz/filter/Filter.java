package com.senz.filter;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashSet;
import com.senz.core.Senz;
import com.senz.filter.ImplementationList;

public class Filter {
    Context mContext;
    FilterImplementation mFilterImpls[];

    public Filter(Context context) {
        this.mContext = context;
        mFilterImpls = new FilterImplementation[ImplementationList.getters.length];
        for (int i = 0; i < ImplementationList.getters.length; i++) {
            mFilterImpls[i] = (FilterImplementation) ImplementationList.getters[i].get();
            mFilterImpls[i].init(context);
        }
    }

    public ArrayList<Senz> filter(ArrayList<Senz> senzes) {
        HashSet<Senz> orig = new HashSet<Senz>(senzes);
        HashSet<Senz> res = (HashSet<Senz>) orig.clone();

        for (FilterImplementation f : mFilterImpls)
            res.retainAll(f.filter(orig));

        return new ArrayList<Senz>(res);
    }
}
