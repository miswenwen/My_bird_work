[This file is encoded in UTF-8]
此目录下包含了开发过程中可能会用到的一些辅助代码或者工具。
以下是针对每个文件的说明。

[readme.txt]
这个说明文件。

[AdjustBinaryHanziPinyinTable.java]
这个Java类是用于调整联系人应用和ContactsProvider中汉字拼音数据的。
用于J2SE环境，在命令行执行:
javac AdjustBinaryHanziPinyinTable.java
来编译此文件。
执行:
java AdjustBinaryHanziPinyinTable <data_hzpinyin_bin的路径> <汉字> <汉字的拼音> ...
来修改联系人的汉字拼音查询表，如果是多音字则后面以空格隔开提供多个拼音，最多提供3个。
其中data_hzpinyin_bin是联系人应用使用的二进制格式的汉字拼音表，一般在联系人应用工程的/res/raw/下。
例如，data_hzpinyin_bin在D:\Contacts\res\raw\下，则一下命令可以将“卡”字的拼音替换为"Ka"和"Qia"。
其中"Ka"为主拼音（可用于ContactsProvider定位汉字所属的字母），"Qia"是另一个拼音。
java AdjustBinaryHanziPinyinTable D:\Contacts\res\raw\data_hzpinyin_bin 卡 Ka Qia
运行完此程序后，会将原汉字拼音表中“卡”字的拼音数据换成Ka和Qia，
并且将新的汉字拼音表生成在D:\Contacts\res\raw\data_hzpinyin_bin.new，
同时生成一个PrimaryPinyin.java。
这个PrimaryPinyin.java是给ContactsProvider用来查询姓氏拼音的汉字拼音表。
在ContactsProvider的代码中找到同名类，覆盖即可。
如果在ContactsProvider中没有同名类，则是因为ContactsProvider没有使用与联系人应用相同的拼音方案，需要另外解决汉字拼音修正的问题。
注意：
如果碰到无法使用中文作为命令行参数的工作环境，请正确配置工作环境以支持中文。
强行在不支持中文的环境下处理中文容易产生乱码问题。
