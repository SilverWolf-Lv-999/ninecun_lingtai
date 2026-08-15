package net.seraphina.ninecun_lingtai.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class RealmNameUtility {
    private static final RealmName[] CANONICAL_NAMES = {
            new RealmName("七星界", "qixing_jie"),
            new RealmName("幽冥界", "youming_jie"),
            new RealmName("混沌界", "hundun_jie"),
            new RealmName("太虚界", "taixu_jie"),
            new RealmName("鸿蒙界", "hongmeng_jie"),
            new RealmName("九霄界", "jiuxiao_jie"),
            new RealmName("紫微界", "ziwei_jie"),
            new RealmName("青冥界", "qingming_jie"),
            new RealmName("沧澜界", "canglan_jie"),
            new RealmName("玄黄界", "xuanhuang_jie"),
            new RealmName("昆仑界", "kunlun_jie"),
            new RealmName("蓬莱界", "penglai_jie")
    };
    private static final RealmNamePart[] PREFIXES = {
            new RealmNamePart("七星", "qixing"),
            new RealmNamePart("幽冥", "youming"),
            new RealmNamePart("混沌", "hundun"),
            new RealmNamePart("太虚", "taixu"),
            new RealmNamePart("鸿蒙", "hongmeng"),
            new RealmNamePart("九霄", "jiuxiao"),
            new RealmNamePart("紫微", "ziwei"),
            new RealmNamePart("青冥", "qingming"),
            new RealmNamePart("沧澜", "canglan"),
            new RealmNamePart("天河", "tianhe"),
            new RealmNamePart("星罗", "xingluo"),
            new RealmNamePart("玄黄", "xuanhuang"),
            new RealmNamePart("昆仑", "kunlun"),
            new RealmNamePart("蓬莱", "penglai"),
            new RealmNamePart("瑶池", "yaochi"),
            new RealmNamePart("碧落", "biluo"),
            new RealmNamePart("黄泉", "huangquan"),
            new RealmNamePart("赤霄", "chixiao"),
            new RealmNamePart("丹霞", "danxia"),
            new RealmNamePart("玉衡", "yuheng"),
            new RealmNamePart("天枢", "tianshu"),
            new RealmNamePart("天璇", "tianxuan"),
            new RealmNamePart("天玑", "tianji"),
            new RealmNamePart("开阳", "kaiyang"),
            new RealmNamePart("摇光", "yaoguang"),
            new RealmNamePart("太初", "taichu"),
            new RealmNamePart("元始", "yuanshi"),
            new RealmNamePart("无极", "wuji"),
            new RealmNamePart("两仪", "liangyi"),
            new RealmNamePart("三清", "sanqing"),
            new RealmNamePart("四象", "sixiang"),
            new RealmNamePart("五岳", "wuyue"),
            new RealmNamePart("八荒", "bahuang"),
            new RealmNamePart("十方", "shifang"),
            new RealmNamePart("大罗", "daluo"),
            new RealmNamePart("灵台", "lingtai"),
            new RealmNamePart("归墟", "guixu"),
            new RealmNamePart("扶桑", "fusang"),
            new RealmNamePart("瀛洲", "yingzhou"),
            new RealmNamePart("方丈", "fangzhang"),
            new RealmNamePart("玄天", "xuantian"),
            new RealmNamePart("苍穹", "cangqiong"),
            new RealmNamePart("云荒", "yunhuang"),
            new RealmNamePart("雪域", "xueyu"),
            new RealmNamePart("雷泽", "leize"),
            new RealmNamePart("风陵", "fengling"),
            new RealmNamePart("炎阳", "yanyang"),
            new RealmNamePart("寒渊", "hanyuan"),
            new RealmNamePart("月华", "yuehua"),
            new RealmNamePart("日曜", "riyao"),
            new RealmNamePart("星海", "xinghai"),
            new RealmNamePart("龙渊", "longyuan"),
            new RealmNamePart("凤鸣", "fengming"),
            new RealmNamePart("麒麟", "qilin"),
            new RealmNamePart("白泽", "baize"),
            new RealmNamePart("伏羲", "fuxi"),
            new RealmNamePart("女娲", "nuwa"),
            new RealmNamePart("盘古", "pangu"),
            new RealmNamePart("帝俊", "dijun"),
            new RealmNamePart("羲和", "xihe"),
            new RealmNamePart("少昊", "shaohao"),
            new RealmNamePart("祝融", "zhurong"),
            new RealmNamePart("共工", "gonggong")
    };
    private static final RealmNamePart[] SUFFIXES = {
            new RealmNamePart("仙域", "xianyu"),
            new RealmNamePart("神州", "shenzhou"),
            new RealmNamePart("天境", "tianjing"),
            new RealmNamePart("云天", "yuntian"),
            new RealmNamePart("星野", "xingye"),
            new RealmNamePart("霄汉", "xiaohan"),
            new RealmNamePart("碧海", "bihai"),
            new RealmNamePart("瑶天", "yaotian"),
            new RealmNamePart("苍梧", "cangwu"),
            new RealmNamePart("灵渊", "lingyuan"),
            new RealmNamePart("道宫", "daogong"),
            new RealmNamePart("玄域", "xuanyu"),
            new RealmNamePart("玉京", "yujing"),
            new RealmNamePart("赤城", "chicheng"),
            new RealmNamePart("瀛海", "yinghai"),
            new RealmNamePart("蓬壶", "penghu"),
            new RealmNamePart("霜华", "shuanghua"),
            new RealmNamePart("晨曦", "chenxi"),
            new RealmNamePart("暮霞", "muxia"),
            new RealmNamePart("流光", "liuguang"),
            new RealmNamePart("归元", "guiyuan"),
            new RealmNamePart("太清", "taiqing"),
            new RealmNamePart("上清", "shangqing"),
            new RealmNamePart("玉清", "yuqing"),
            new RealmNamePart("灵霄", "lingxiao"),
            new RealmNamePart("广寒", "guanghan"),
            new RealmNamePart("洞天", "dongtian"),
            new RealmNamePart("福地", "fudi"),
            new RealmNamePart("天门", "tianmen"),
            new RealmNamePart("地阙", "dique"),
            new RealmNamePart("星辰", "xingchen"),
            new RealmNamePart("月宫", "yuegong"),
            new RealmNamePart("日宫", "rigong"),
            new RealmNamePart("雷域", "leiyu"),
            new RealmNamePart("风洲", "fengzhou"),
            new RealmNamePart("火原", "huoyuan"),
            new RealmNamePart("水泽", "shui_ze"),
            new RealmNamePart("木域", "muyu"),
            new RealmNamePart("金阙", "jinqe"),
            new RealmNamePart("土洲", "tuzhou"),
            new RealmNamePart("阴司", "yinsi"),
            new RealmNamePart("阳关", "yangguan"),
            new RealmNamePart("生灭", "shengmie"),
            new RealmNamePart("轮回", "lunhui"),
            new RealmNamePart("因果", "yinguo"),
            new RealmNamePart("乾坤", "qiankun"),
            new RealmNamePart("造化", "zaohua"),
            new RealmNamePart("寂灭", "jimie"),
            new RealmNamePart("浮屠", "futu"),
            new RealmNamePart("琉璃", "liuli"),
            new RealmNamePart("琼楼", "qionglou"),
            new RealmNamePart("天池", "tianchi"),
            new RealmNamePart("云台", "yuntai"),
            new RealmNamePart("星宫", "xinggong"),
            new RealmNamePart("河图", "hetu"),
            new RealmNamePart("洛书", "loshu"),
            new RealmNamePart("龙宫", "longgong"),
            new RealmNamePart("凤阁", "fengge"),
            new RealmNamePart("神木", "shenmu"),
            new RealmNamePart("石林", "shilin"),
            new RealmNamePart("雾海", "wuhai"),
            new RealmNamePart("雪原", "xueyuan"),
            new RealmNamePart("炎海", "yanhai"),
            new RealmNamePart("雷池", "leichi")
    };
    private static final List<RealmName> NAME_POOL = createNamePool();

    private RealmNameUtility() {
    }

    public static RealmName createRandomName(Set<String> existingDisplayNames, Random random) {
        Objects.requireNonNull(existingDisplayNames, "existingDisplayNames");
        Objects.requireNonNull(random, "random");

        int startIndex = random.nextInt(NAME_POOL.size());
        for (int offset = 0; offset < NAME_POOL.size(); offset++) {
            RealmName candidate = NAME_POOL.get((startIndex + offset) % NAME_POOL.size());
            if (!existingDisplayNames.contains(candidate.getDisplayName())) {
                return candidate;
            }
        }
        throw new IllegalStateException("No unused three-thousand-realm name remains");
    }

    public static int getNameCapacity() {
        return NAME_POOL.size();
    }

    private static List<RealmName> createNamePool() {
        List<RealmName> names = new ArrayList<>(
                CANONICAL_NAMES.length + PREFIXES.length * SUFFIXES.length
        );
        for (RealmName canonicalName : CANONICAL_NAMES) {
            names.add(canonicalName);
        }
        for (RealmNamePart prefix : PREFIXES) {
            for (RealmNamePart suffix : SUFFIXES) {
                names.add(new RealmName(
                        prefix.getChinese() + suffix.getChinese() + "界",
                        prefix.getPinyin() + "_" + suffix.getPinyin() + "_jie"
                ));
            }
        }
        return List.copyOf(names);
    }

    public static final class RealmName {
        private final String thisDisplayName;
        private final String thisPathPrefix;

        private RealmName(String displayName, String pathPrefix) {
            if (!displayName.endsWith("界")) {
                throw new IllegalArgumentException("Realm display name must end with 界");
            }
            if (!pathPrefix.matches("[a-z0-9_]+_jie")) {
                throw new IllegalArgumentException("Realm path prefix must be a valid pinyin resource path");
            }
            this.thisDisplayName = displayName;
            this.thisPathPrefix = pathPrefix;
        }

        public String getDisplayName() {
            return this.thisDisplayName;
        }

        public String getPathPrefix() {
            return this.thisPathPrefix;
        }
    }

    private static final class RealmNamePart {
        private final String thisChinese;
        private final String thisPinyin;

        private RealmNamePart(String chinese, String pinyin) {
            this.thisChinese = chinese;
            this.thisPinyin = pinyin;
        }

        private String getChinese() {
            return this.thisChinese;
        }

        private String getPinyin() {
            return this.thisPinyin;
        }
    }
}
