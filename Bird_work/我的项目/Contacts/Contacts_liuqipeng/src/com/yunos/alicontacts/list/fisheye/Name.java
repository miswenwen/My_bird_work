package com.yunos.alicontacts.list.fisheye;


public final class Name {
    /** Contact name. */
    public String nameSource;
    //在列表中的位置
    public int pos;
    //中文姓
    public String xing;
    //如果有中文姓的话,归属的英文字母
    public char pyIndex;

    @Override
    public String toString(){
        return new String("Xing:"+xing+"|Pos="+pos+"|pyIndex="+pyIndex+"~");
    }
}
