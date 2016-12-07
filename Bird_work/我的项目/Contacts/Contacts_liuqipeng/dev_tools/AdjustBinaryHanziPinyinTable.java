import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * <p>
 * 这是一个工具类，用于修改汉字拼音表中指定汉字的拼音数据。
 * 目前的汉字拼音表保存为一个二进制数据文件，每2个字节组成一个short值，
 * 每个short的字节0为short的低字节，字节1为short的高字节，即little-endian表示。
 * 以代码的方式表示为：<br/>
 * short s = (short)((binaryData[charIndex * 2] & 0xFF) | ((binaryData[(charIndex * 2) + 1] & 0xFF) << 8));<br/>
 * </p><p>
 * 这个short是一个索引值，即在字符串数组 PINYIN_TOKENS 中的位置。
 * </p><p>
 * 字符串数组 PINYIN_TOKENS 表示了当前支持的所有拼音的音节。每个汉字的拼音都会对应到其中的一个元素。
 * </p><p>
 * 针对多音字的情况，目前每个汉字最多支持查询到3个拼音，所以在汉字拼音表中，每3个short对应1个汉字。
 * 拼音表按照汉字的编码顺序提供拼音，从编码为 HANZI_START 的汉字起，共提供 HANZI_COUNT 个汉字的拼音。
 * 即第0、1、2个short对应编码为 HANZI_START 的汉字，第3、4、5个short对应编码为 HANZI_START+1 的汉字，以此类推。
 * </p>
 * <p>
 * 此程序还会给ContactsProvider生成一份拼音数据，跟联系人应用的数据相同。
 * 但是由于ContactsProvider只需要为姓氏定位其所属的拼音首字母，所以每个汉字只需要一个拼音数据。
 * 并且针对多音字的情况，只提供作为姓氏用的拼音。
 * </p><p>
 * 2015-02-13补充：<br/>
 * 由于有bug报告联系人列表搜索联系人无法支持汉字多音字，
 * 所以，现在给ContactsProvider生成的拼音数据也为多音字汉字提供至多3个拼音。
 * </p>
 * @author junqi.qjq
 */
public class AdjustBinaryHanziPinyinTable {

    public static final int HANZI_START = 19968;
    public static final int HANZI_COUNT = 20902;

    /** The count of pinyin candidate for each Chinese character. */
    private static final int PINYIN_CANDIDATE_PER_CHAR = 3;

    /**
     * 在为ContactsProvider生成的拼音数据中，由于Java的一些限制，
     * 拼音的索引值会分段保存在多个short[]中。
     * 这个值是定义每个short[]中容纳多少个汉字的拼音数据。
     */
    public static final int MAX_HANZI_COUNT_IN_SHORT_ARRAY = 2832;
    /**
     * Java的方法大小有65536字节的限制。
     * 在一个初始化short[]的方法中，创建一个带初始数据的short[]的大小是有限制的。
     * 经简单试验，以下值可以作为short[]的长度，使编译器不报错。
     * 并且，现在方案中，一个汉字最多对应三个拼音，所以要求这个数是3的倍数，
     * 以确保单个汉字的数据不会跨数组，这样代码会简单一些。
     */
    public static final int MAX_ELEMENTS_IN_SHORT_ARRAY_INIT_METHOD
        = MAX_HANZI_COUNT_IN_SHORT_ARRAY * PINYIN_CANDIDATE_PER_CHAR;

    /**
     * 所有汉字用到的拼音音节。
     * 必须是A-Z的字母，首字母大写。
     */
    public static final String[] PINYIN_TOKENS = {
        "A", "Ai", "An", "Ang", "Ao",
        "Ba", "Bai", "Ban", "Bang", "Bao", "Bei", "Ben", "Beng", "Bi", "Bian", "Biao", "Bie", "Bin", "Bing", "Bo", "Bu",
        "Ca", "Cai", "Can", "Cang", "Cao", "Ce", "Cen", "Ceng", "Cha", "Chai", "Chan", "Chang", "Chao", "Che", "Chen", "Cheng",
        "Chi", "Chong", "Chou", "Chu", "Chua", "Chuai", "Chuan", "Chuang", "Chui", "Chun", "Chuo", "Ci", "Cong", "Cou", "Cu", "Cuan",
        "Cui", "Cun", "Cuo",
        "Da", "Dai", "Dan", "Dang", "Dao", "De", "Dei", "Den", "Deng", "Di", "Dia", "Dian", "Diao", "Die", "Ding", "Diu",
        "Dong", "Dou", "Du", "Duan", "Dui", "Dun", "Duo",
        "E", "Ei", "En", "Er",
        "Fa", "Fan", "Fang", "Fei", "Fen", "Feng", "Fiao", "Fo", "Fou", "Fu",
        "Ga", "Gai", "Gan", "Gang", "Gao", "Ge", "Gei", "Gen", "Geng", "Gong", "Gou", "Gu", "Gua", "Guai", "Guan", "Guang",
        "Gui", "Gun", "Guo",
        "Ha", "Hai", "Han", "Hang", "Hao", "He", "Hei", "Hen", "Heng", "Hong", "Hou", "Hu", "Hua", "Huai", "Huan", "Huang",
        "Hui", "Hun", "Huo",
        "Ji", "Jia", "Jian", "Jiang", "Jiao", "Jie", "Jin", "Jing", "Jiong", "Jiu", "Ju", "Juan", "Jue", "Jun",
        "Ka", "Kai", "Kan", "Kang", "Kao", "Ke", "Kei", "Ken", "Keng", "Kong", "Kou", "Ku", "Kua", "Kuai", "Kuan", "Kuang",
        "Kui", "Kun", "Kuo",
        "La", "Lai", "Lan", "Lang", "Lao", "Le", "Lei", "Leng", "Li", "Lia", "Lian", "Liang", "Liao", "Lie", "Lin", "Ling",
        "Liu", "Long", "Lou", "Lu", "Luan", "Lun", "Luo", "Lv", "Lve",
        "M", "Ma", "Mai", "Man", "Mang", "Mao", "Me", "Mei", "Men", "Meng", "Mi", "Mian", "Miao", "Mie", "Min", "Ming",
        "Miu", "Mo", "Mou", "Mu",
        "N", "Na", "Nai", "Nan", "Nang", "Nao", "Ne", "Nei", "Nen", "Neng", "Ni", "Nian", "Niang", "Niao", "Nie", "Nin",
        "Ning", "Niu", "Nong", "Nou", "Nu", "Nuan", "Nun", "Nuo", "Nv", "Nve",
        "O", "Ou",
        "Pa", "Pai", "Pan", "Pang", "Pao", "Pei", "Pen", "Peng", "Pi", "Pian", "Piao", "Pie", "Pin", "Ping", "Po", "Pou",
        "Pu",
        "Qi", "Qia", "Qian", "Qiang", "Qiao", "Qie", "Qin", "Qing", "Qiong", "Qiu", "Qu", "Quan", "Que", "Qun",
        "Ran", "Rang", "Rao", "Re", "Ren", "Reng", "Ri", "Rong", "Rou", "Ru", "Ruan", "Rui", "Run", "Ruo",
        "Sa", "Sai", "San", "Sang", "Sao", "Se", "Sen", "Seng", "Sha", "Shai", "Shan", "Shang", "Shao", "She", "Shei", "Shen",
        "Sheng", "Shi", "Shou", "Shu", "Shua", "Shuai", "Shuan", "Shuang", "Shui", "Shun", "Shuo", "Si", "Song", "Sou", "Su", "Suan",
        "Sui", "Sun", "Suo",
        "Ta", "Tai", "Tan", "Tang", "Tao", "Te", "Teng", "Ti", "Tian", "Tiao", "Tie", "Ting", "Tong", "Tou", "Tu", "Tuan",
        "Tui", "Tun", "Tuo",
        "Wa", "Wai", "Wan", "Wang", "Wei", "Wen", "Weng", "Wo", "Wu",
        "Xi", "Xia", "Xian", "Xiang", "Xiao", "Xie", "Xin", "Xing", "Xiong", "Xiu", "Xu", "Xuan", "Xue", "Xun",
        "Ya", "Yan", "Yang", "Yao", "Ye", "Yi", "Yin", "Ying", "Yo", "Yong", "You", "Yu", "Yuan", "Yue", "Yun",
        "Za", "Zai", "Zan", "Zang", "Zao", "Ze", "Zei", "Zen", "Zeng", "Zha", "Zhai", "Zhan", "Zhang", "Zhao", "Zhe", "Zhei",
        "Zhen", "Zheng", "Zhi", "Zhong", "Zhou", "Zhu", "Zhua", "Zhuai", "Zhuan", "Zhuang", "Zhui", "Zhun", "Zhuo", "Zi", "Zong", "Zou",
        "Zu", "Zuan", "Zui", "Zun", "Zuo",
    };

    private static final String FILENAME_DUMP_PINYIN_BEFORE_ADJUST = "pinyin_for_all_chars_before_adjust";
    private static final String FILENAME_DUMP_PINYIN_AFTER_ADJUST = "pinyin_for_all_chars_after_adjust";
    private static final String CONTACTSPROVIDER_PINYIN_DATA_CLASS_NAME = "PinyinData";
    private static final String CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET = "UTF-8";
    private static final String CONTACTSPROVIDER_PINYIN_DATA_PACKAGE_NAME = "com.android.providers.contacts.util";

    private static String mPinyinDataFilePath = null;
    private static File mPinyinDataFile = null;
    private static ArrayList<AdjustInfo> mAdjustList = new ArrayList<AdjustInfo>();
    private static short[] mRawPinyinData;

    public static void main(String[] args) {
        if (!checkArgs(args)) {
            return;
        }
        loadBinaryPinyinContent();
        dumpPinyinForAllCharsToFile(FILENAME_DUMP_PINYIN_BEFORE_ADJUST);
        System.out.println("1、调整前的汉字拼音文本对照列表已经保存到文件: \""+FILENAME_DUMP_PINYIN_BEFORE_ADJUST+"\"。");
        adjustPinyinForChars();
        dumpPinyinForAllCharsToFile(FILENAME_DUMP_PINYIN_AFTER_ADJUST);
        System.out.println("2、调整后的汉字拼音文本对照列表已经保存到文件: \""+FILENAME_DUMP_PINYIN_AFTER_ADJUST+"\"。");
        System.out.println("3、请用diff命令对比上述两个文件，以确保对二进制内容的修改跟预期一致。");
        String pinyinDataFilePathAfterAdjust = mPinyinDataFilePath+".new";
        saveBinaryPinyinContent(pinyinDataFilePathAfterAdjust);
        System.out.println("4、调整后的汉字拼音表（二进制）已经保存到文件\""+pinyinDataFilePathAfterAdjust+"\"。请将这个文件替换联系人应用中原有的汉字拼音表。");
        dumpPinyinDataForContactsProvider(CONTACTSPROVIDER_PINYIN_DATA_CLASS_NAME, CONTACTSPROVIDER_PINYIN_DATA_PACKAGE_NAME);
        System.out.println("5、为ContactsProvider准备的汉字拼音Java类文件已经保存到文件: \""+CONTACTSPROVIDER_PINYIN_DATA_CLASS_NAME+".java\"。请将这个文件替换ContactsProvider中的同名文件。");
    }

    private static void dumpPinyinDataForContactsProvider(String clsName, String pkgName) {
        FileOutputStream fos = null;
        String filePath = clsName+".java";
        try {
            fos = new FileOutputStream(filePath);
            dumpCopyrightAndNote(fos);
            dumpPackage(fos, pkgName);
            dumpClassStart(fos, clsName);
            dumpHanziConstants(fos);
            dumpIndexArrays(fos);
            dumpPinyinTokenArray(fos);
            dumpFunctionMethods(fos);
            dumpDataMethods(fos);
            dumpClassEnd(fos);
        } catch (Exception e) {
            throw new RuntimeException("dumpPrimaryPinyinForContactsProvider发生意外", e);
        } finally {
            closeCloseable(fos);
        }
    }

    private static void dumpCopyrightAndNote(FileOutputStream fos) throws IOException {
        fos.write("/* Copyright (C) 2014-2015 Alibaba Inc. */\n".getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
        fos.write(
                 ("/*\n"
                + " * Note: This class is generated by AdjustBinaryHanziPinyinTable.java,\n"
                + " * which can be find in aliyunos/packages/apps/Contacts/dev_tools/.\n"
                + " * Do ***NOT*** modify this file manually!\n"
                + " */\n").getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpPackage(FileOutputStream fos, String pkgName) throws IOException {
        StringBuilder pkgLine = new StringBuilder();
        pkgLine.append("package ").append(pkgName).append(";\n\n");
        fos.write(pkgLine.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpClassStart(FileOutputStream fos, String clsName) throws IOException {
        StringBuilder clsLine = new StringBuilder();
        clsLine.append("public final class ").append(clsName).append(" {\n\n");
        fos.write(clsLine.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpHanziConstants(FileOutputStream fos) throws IOException {
        StringBuilder code = new StringBuilder();
        code.append("    public static final int HANZI_START = ").append(HANZI_START)
            .append(";\n    public static final int HANZI_COUNT = ").append(HANZI_COUNT)
            .append(";\n    public static final int PINYIN_CANDIDATE_PER_CHAR = ").append(PINYIN_CANDIDATE_PER_CHAR)
            .append(";\n\n");
        fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpIndexArrays(FileOutputStream fos) throws IOException {
        int arrayCount = (HANZI_COUNT + MAX_HANZI_COUNT_IN_SHORT_ARRAY - 1) / MAX_HANZI_COUNT_IN_SHORT_ARRAY;
        int normalCount = (HANZI_COUNT + arrayCount - 1) / arrayCount;
        StringBuilder code = new StringBuilder();
        code.append("    /*\n")
            .append("     * There is a size limit (65535 bytes) for static initializer,\n")
            .append("     * (actually, the limit comes from the size limit of a method).\n")
            .append("     * So we divide the index array into ").append(arrayCount).append(" parts,\n")
            .append("     * and assign values generated from individual methods.\n")
            .append("     */\n")
            .append("    private static final int PINYIN_INDEXES_ARRAY_COUNT = ").append(arrayCount).append(";\n")
            .append("    private static final int PINYIN_INDEXES_ARRAY_SIZE = ").append(normalCount).append(";\n")
            .append("    private static final short[][] PINYIN_INDEXES = getPinyinData();\n\n");

        fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void trimTailingSpaces(StringBuilder sb) {
        int index = sb.length() - 1;
        while (index >= 0) {
            if (sb.charAt(index) == ' ') {
                sb.deleteCharAt(index);
                index--;
            } else {
                break;
            }
        }
    }

    private static void dumpPinyinTokenArray(FileOutputStream fos) throws IOException {
        StringBuilder code = new StringBuilder();
        char prevCap = ' ', cap;
        int curCount  = 0;
        code.append("    private static final String[] PINYIN_TOKENS = {\n        ");
        for (int i = 0; i < PINYIN_TOKENS.length; i++) {
            cap = PINYIN_TOKENS[i].charAt(0);
            if (prevCap != cap) {
                prevCap = cap;
                if ((curCount % 16) != 0) {
                    trimTailingSpaces(code);
                    code.append("\n        ");
                }
                curCount = 0;
            }
            code.append('\"').append(PINYIN_TOKENS[i].toUpperCase(Locale.US)).append("\", ");
            curCount++;
            if (curCount % 16 == 0) {
                trimTailingSpaces(code);
                code.append("\n        ");
            }
        }
        trimTailingSpaces(code);
        code.append("\n    };\n\n");
        fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpFunctionMethods(FileOutputStream fos) throws IOException {
        StringBuilder code = new StringBuilder();
        code.append("    public static boolean isHanzi(int charCode) {\n")
            .append("        return (charCode >= HANZI_START) && (charCode < (HANZI_START + HANZI_COUNT));\n")
            .append("    }\n\n")
            .append("    public static String getPinyin(int charCode, int pyIndex) {\n")
            .append("        if (!isHanzi(charCode)) {\n")
            .append("            return null;\n")
            .append("        }\n")
            .append("        charCode -= HANZI_START;\n")
            .append("        int segment = charCode / PINYIN_INDEXES_ARRAY_SIZE;\n")
            .append("        int offset = charCode - (PINYIN_INDEXES_ARRAY_SIZE * segment);\n")
            .append("        int indexInSegment = offset * PINYIN_CANDIDATE_PER_CHAR + pyIndex;\n")
            .append("        return getPinyinToken(PINYIN_INDEXES[segment][indexInSegment]);\n")
            .append("    }\n\n")
            .append("    private static String getPinyinToken(int index) {\n")
            .append("        if (index < 0) {\n")
            .append("            return null;\n")
            .append("        }\n")
            .append("        return PINYIN_TOKENS[index];\n")
            .append("    }\n\n");
        fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void dumpDataMethods(FileOutputStream fos) throws IOException {
        StringBuilder code = new StringBuilder();
        int arrayCount = (HANZI_COUNT + MAX_HANZI_COUNT_IN_SHORT_ARRAY - 1) / MAX_HANZI_COUNT_IN_SHORT_ARRAY;
        int normalCount = (HANZI_COUNT + arrayCount - 1) / arrayCount;
        int lastCount = HANZI_COUNT - (normalCount * (arrayCount - 1));
        // 生成调用创建每一个分组数据的方法
        code.append("    private static short[][] getPinyinData() {\n")
            .append("        short[][] pinyinData = new short[PINYIN_INDEXES_ARRAY_COUNT][];\n");
        for (int i = 0; i < arrayCount; i++) {
            code.append("        pinyinData[").append(i).append("] = getPinyinIndexes_").append(i).append("();\n");
        }
        code.append("        return pinyinData;\n")
            .append("    }\n\n");
        fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
        code.setLength(0);

        // 生成每一个分组数据的方法
        int offset = 0;
        for (int i = 0; i < arrayCount; i++) {
            int count = (i == (arrayCount - 1)) ? lastCount : normalCount;
            dumpDataMethod(code, i, offset, count);
            offset += count;
            fos.write(code.toString().getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
            code.setLength(0);
        }
    }

    private static void dumpDataMethod(StringBuilder code, int suffix, int start, int count) {
        code.append("    private static short[] getPinyinIndexes_").append(suffix).append("() {\n")
            .append("        return new short[] {");
        int elementOffset = PINYIN_CANDIDATE_PER_CHAR * start;
        int elementCount = PINYIN_CANDIDATE_PER_CHAR * count;
        for (int i = 0; i < elementCount; i++) {
            if ((i % 16) == 0) {
                trimTailingSpaces(code);
                code.append("\n            ");
            }
            code.append(mRawPinyinData[elementOffset + i]).append(", ");
        }
        trimTailingSpaces(code);
        code.append("\n        };\n")
            .append("    }\n\n");
    }

    private static void dumpClassEnd(FileOutputStream fos) throws IOException {
        fos.write("}\n".getBytes(CONTACTSPROVIDER_PINYIN_DATA_CLASS_FILE_CHARSET));
    }

    private static void saveBinaryPinyinContent(String filePath) {
        FileOutputStream fos = null;
        try {
            byte[] content = new byte[2 * PINYIN_CANDIDATE_PER_CHAR * HANZI_COUNT];
            int contentIndex = 0;
            short raw;
            for (int hzIndex = 0; hzIndex < HANZI_COUNT; hzIndex++) {
                for (int pyIndex = 0; pyIndex < PINYIN_CANDIDATE_PER_CHAR; pyIndex++) {
                    raw = mRawPinyinData[PINYIN_CANDIDATE_PER_CHAR * hzIndex + pyIndex];
                    content[contentIndex++] = (byte)(raw & 0xFF);
                    content[contentIndex++] = (byte)((raw >> 8) & 0xFF);
                }
            }
            fos = new FileOutputStream(filePath);
            fos.write(content);
        } catch (Exception e) {
            throw new RuntimeException("saveBinaryPinyinContent发生意外", e);
        } finally {
            closeCloseable(fos);
        }
    }

    private static void adjustPinyinForChars() {
        for (AdjustInfo adj : mAdjustList) {
            adjustPinyin(adj);
        }
    }

    private static void adjustPinyin(AdjustInfo adj) {
        String pinyin;
        int index;
        for (int i = 0; i < PINYIN_CANDIDATE_PER_CHAR; i++) {
            pinyin = adj.getPinyinData(i);
            index = getIndexForPinyin(pinyin);
            setHanziPinyinDataForCharCode(adj.mCharCode, i, index);
        }
    }

    private static int getIndexForPinyin(String pinyin) {
        if (pinyin == null) {
            return -1;
        }
        for (int i = 0; i < PINYIN_TOKENS.length; i++) {
            if (PINYIN_TOKENS[i].equals(pinyin)) {
                return i;
            }
        }
        throw new RuntimeException("此程序发现bug: 正在调整一个不支持的拼音\""+pinyin+"\"");
    }

    private static void dumpPinyinForAllCharsToFile(String filePath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            for (int i = 0; i < HANZI_COUNT; i++) {
                dumpPinyinForCharToStream(HANZI_START + i, fos);
            }
        } catch (Exception e) {
            throw new RuntimeException("dumpPinyinForAllCharsToFile发生意外", e);
        } finally {
            closeCloseable(fos);
        }
    }

    private static void dumpPinyinForCharToStream(int chCode, OutputStream os) throws IOException {
        StringBuilder sb = new StringBuilder();
        String pinyin;
        sb.append((char)(chCode)).append(':');
        for (int i = 0; i < PINYIN_CANDIDATE_PER_CHAR; i++) {
            pinyin = getHanziPinyinDataForCharCode(chCode, i);
            if (pinyin == null) {
                break;
            }
            sb.append(pinyin).append(';');
        }
        sb.append('\n');
        os.write(sb.toString().getBytes("UTF-8"));
    }

    private static void loadBinaryPinyinContent() {
        byte[] content = readDataToArray(mPinyinDataFile);
        int totalPinyinCount = HANZI_COUNT * PINYIN_CANDIDATE_PER_CHAR;
        mRawPinyinData = new short[totalPinyinCount];
        int resultIndex = 0;
        for (int pinyinIndex = 0; pinyinIndex < totalPinyinCount; pinyinIndex++) {
            mRawPinyinData[pinyinIndex] = (short)((content[resultIndex] & 0xFF) | ((content[resultIndex+1] & 0xFF) << 8));
            resultIndex += 2;
        }
    }

    private static void usage() {
        System.out.println("这个程序用于修改汉字拼音表的数据文件，订正个别汉字的拼音数据。");
        System.out.println("用法:");
        System.out.println("java "+AdjustBinaryHanziPinyinTable.class.getCanonicalName()+" -d <pinyin_data_file> <Chinese character> <Pinyin1> [<Pinyin2> [<Pinyin3>]]");
        System.out.println("其中:");
        System.out.println("    -d表示后面跟的参数是拼音数据文件");
        System.out.println("    <pinyin_data_file>是二进制汉字拼音表文件，'<'和'>'表示此参数必须指定");
        System.out.println("    <Chinese character>是需要修改拼音的汉字，每次仅限一个汉字字符");
        System.out.println("    <Pinyin1>/<Pinyin2>/<Pinyin3>是前面汉字的拼音，'['和']'表示此参数可选，这个汉字有几个拼音就指定几个，最多指定3个，每个拼音中间不包含空格，多个拼音之间用空格分隔");
        System.out.println("        如果有多个汉字的拼音需要修改，<Chinese character> <Pinyin1> [<Pinyin2> [<Pinyin3>]]这个序列可以出现多次");
    }

    private static boolean checkArgs(String[] args) {
        boolean success = false;
        try {
            parseArgs(args);
            checkPinyinDataFile();
            checkAdjustPinyin();
            success = true;
        } catch (Exception e) {
            usage();
            e.printStackTrace();
        }
        return success;
    }

    private static void checkPinyinDataFile() {
        if (mPinyinDataFilePath == null) {
            throw new RuntimeException("未指定汉字拼音表文件");
        }
        mPinyinDataFile = new File(mPinyinDataFilePath);
        if (!mPinyinDataFile.isFile()) {
            throw new RuntimeException("未找到汉字拼音表文件\""+mPinyinDataFilePath+"\"");
        }
        System.out.println("汉字拼音表路径: "+mPinyinDataFilePath+"; 大小: "+mPinyinDataFile.length());
    }

    private static void checkAdjustPinyin() {
        if (mAdjustList.size() == 0) {
            throw new RuntimeException("未指定要调整拼音的汉字");
        }
        HashSet<String> adjustChars = new HashSet<String>();
        for (AdjustInfo adj : mAdjustList) {
            if (adjustChars.contains(adj.mChar)) {
                throw new RuntimeException("汉字\""+adj.mChar+"\"被重复调整");
            }
            if (adj.getPinyinCount() > PINYIN_CANDIDATE_PER_CHAR) {
                throw new RuntimeException("为\""+adj.mChar+"\"指定的拼音超过上限:"+PINYIN_CANDIDATE_PER_CHAR+"个");
            }
            adjustChars.add(adj.mChar);
            System.out.println("调整内容: "+adj);
        }
    }

    private static void parseArgs(String[] args) {
        if ((args == null) || (args.length < 4)) {
            throw new RuntimeException("缺少必要参数。");
        }
        int index = 0, len = args.length;
        String arg;
        AdjustInfo adjust = null;
        while (index < len) {
            arg = args[index];
            if ("-d".equals(arg)) {
                index++;
                mPinyinDataFilePath = args[index];
                index++;
                continue;
            }
            if (isSingleChineseChar(arg)) {
                adjust = new AdjustInfo(arg);
                mAdjustList.add(adjust);
                index++;
                continue;
            } else if (isValidPinyinToken(arg)) {
                if (adjust == null) {
                    throw new RuntimeException("没有为拼音\""+arg+"\"指定汉字");
                }
                if ((adjust.getPinyinCount() == 0) && (!adjust.mChar.equals(args[index-1]))) {
                    throw new RuntimeException("不确定拼音\""+arg+"\"是为哪个汉字指定的");
                }
                adjust.addPinyin(arg);
                index++;
                continue;
            } else {
                throw new RuntimeException("不支持的参数: "+arg);
            }
        }
    }

    private static boolean isSingleChineseChar(String str) {
        if ((str == null) || (str.length() != 1)) {
            return false;
        }
        int chCode = str.charAt(0);
        return (chCode >= HANZI_START) && (chCode < (HANZI_START + HANZI_COUNT));
    }

    private static boolean isValidPinyinToken(String str) {
        for (String pinyin : PINYIN_TOKENS) {
            if (pinyin.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static void setHanziPinyinDataForCharCode(int chCode, int pinyinIndex, int pinyinTokenArrayIndex) {
        int indexInRawData = (chCode - HANZI_START) * PINYIN_CANDIDATE_PER_CHAR + pinyinIndex;
        mRawPinyinData[indexInRawData] = (short) pinyinTokenArrayIndex;
    }

    public static String getHanziPinyinDataForCharCode(int chCode, int index) {
        int indexInRawData = (chCode - HANZI_START) * PINYIN_CANDIDATE_PER_CHAR + index;
        int pinyinTokenArrayIndex = mRawPinyinData[indexInRawData];
        if (pinyinTokenArrayIndex < 0) {
            return null;
        }
        return PINYIN_TOKENS[pinyinTokenArrayIndex];
    }

    public static byte[] readDataToArray(File f) {
        byte[] ret = null;
        FileInputStream fis = null;
        try {
            int size = (int) f.length();
            ret = new byte[size];
            fis = new FileInputStream(f);
            int cnt = 0, n;
            while (cnt < size) {
                n = fis.read(ret, cnt, size - cnt);
                if (n < 0) {
                    break;
                }
                cnt += n;
            }
        } catch (Exception e) {
            throw new RuntimeException("readDataToArray发生意外", e);
        } finally {
            closeCloseable(fis);
        }
        return ret;
    }

    public static void closeCloseable(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class AdjustInfo {
        private final String mChar;
        private final int mCharCode;
        private final ArrayList<String> mPinyinList = new ArrayList<String>();

        public AdjustInfo(String ch) {
            mChar = ch;
            mCharCode = mChar.charAt(0);
        }

        public void addPinyin(String pinyin) {
            mPinyinList.add(pinyin);
        }

        public int getPinyinCount() {
            return mPinyinList.size();
        }

        public String getPinyinData(int index) {
            if (index < mPinyinList.size()) {
                return mPinyinList.get(index);
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AdjustInfo:{char:\'").append(mChar)
              .append("\'; charCode:")
              .append(mCharCode)
              .append("; pinyin:[");
            for (String pinyin : mPinyinList) {
                sb.append('\"').append(pinyin).append("\", ");
            }
            sb.append("]}");
            return sb.toString();
        }
    }

}
