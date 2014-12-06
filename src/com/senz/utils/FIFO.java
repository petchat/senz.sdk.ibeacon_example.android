package com.senz.utils;

import java.util.Deque;
import java.util.List;

/***********************************************************************************************************************
 * @InterfaceName: FIFO
 * @Author:        Woodie
 * @CreateAt:      Sat, Nov 20, 2014
 * @Description:
 ***********************************************************************************************************************/
interface FIFO<T> extends List<T>, Deque<T>, Cloneable, java.io.Serializable {

    // Add a new element to the last of list,
    // If there is already full, then poll an element out.
    T addLastSafe(T addLast);

    // Poll the head of list, if there is not exist, then return null
    T pollSafe();

    // Get the max size of list
    int getMaxSize();

    // Set the max size of list, if the new size is smaller then the number of existing elements, then poll then out
    List<T> setMaxSize(int maxSize);
}
