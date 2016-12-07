package com.yunos.alicontacts.dialpad.smartsearch;

public interface PinyinSearchStateChangeListener {
    void onStateChanged(boolean isReady);
    void onSearchTableChanged();
}
