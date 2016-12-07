package com.yunos.alicontacts.dialpad.smartsearch;



public class HanziUtil {
    public static final int HANZI_START = 19968;
    public static final int HANZI_COUNT = 20902;

    public static final char[] Data_Letters_To_T9 = {
                            '2', '2', '2', '3', '3', '3',
        '4', '4', '4',      '5', '5', '5', '6', '6', '6',
        '7', '7', '7', '7', '8', '8', '8', '9', '9', '9', '9'
    };

    public static final String Data_Reg_Pinyin =
        "2     24    26    264   22    224   226   2264  234   236   2364  2426  243   246   2464  28    23    " +
        "242   2424  24264 2436  24364 244   24664 2468  248   2482  24824 24826 2482642484  2486  2664  268   " +
        "2826  284   286   32    324   326   3264  33    334   336   3364  34    342   3426  343   3464  348   " +
        "3664  368   38    3826  384   386   3     36    37    42    424   426   4264  43    434   436   4364  " +
        "4664  468   48    482   4824  4826  48264 484   486   54    542   5426  54264 543   546   5464  54664 " +
        "548   58    5826  583   586   52    524   526   5264  53    534   536   5364  5664  568   582   5824  " +
        "58264 584   6     62    624   626   6264  63    634   636   6364  64    6426  643   646   6464  648   " +
        "66    668   68    64264 6664  6826  686   683   72    724   726   7264  734   736   7364  74    7426  " +
        "743   746   7464  76    768   78    742   74264 74664 748   7826  783   786   73    7664  784   7424  " +
        "7434  7436  74364 744   7468  7482  74824 74826 7482647484  7486  82    824   826   8264  83    8364  " +
        "84    8426  843   8464  8664  868   88    8826  884   886   92    924   926   9264  934   936   9364  " +
        "96    98    94    942   9426  94264 943   946   9464  94664 948   9826  983   986   93    9664  968   " +
        "9424  9434  9436  94364 944   9468  9482  94824 94826 9482649484  9486  984   ";

    public static boolean isHanziCharCode(int chCode) {
        return    (chCode >= HanziUtil.HANZI_START)
                && (chCode < (HanziUtil.HANZI_START + HanziUtil.HANZI_COUNT));
    }

    public static boolean hasPinyin(int chCode) {
        if (!isHanziCharCode(chCode)) {
            return false;
        }
        int pinyinIndex = PinyinSearch.getHanziPinyinIndexForCharCode(chCode, 0);
        return pinyinIndex >= 0;
    }

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

}
