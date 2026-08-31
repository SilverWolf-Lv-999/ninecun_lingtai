package net.seraphina.ninecun_lingtai.common.seal.lockgrid;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridBoard.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 锁纹盘面生成器——参数逐项移植自 prototype/阵禁玩法原型.html 的
 * {@code LK.gradeSpec / genLayer}，禁止凭感觉重调（施工图风险清单第 1 条）。
 *
 * <p>纹骨（节点布局 + 主径）由 boneSeed 派生：同一 sealId 同一层永久不变；
 * 假流光/雾区由 attemptSeed 派生：每次尝试换 nonce 即换变式——旧录屏不能成为
 * 永久答案，但主人的"笔迹"是认得出来的。</p>
 */
public final class LockGridGenerator {
    /** 品阶规格表：{节点, 路径长, 假流光, 真流光周期秒}（一至四阶，5+ 每阶递增）。 */
    private static final int[][] GRADE_SPECS = {
            {9, 4, 1, 4},
            {12, 5, 2, 5},
            {16, 6, 3, 6},
            {20, 8, 5, 8}
    };

    private static final Map<ElementType, ElementType> SHENG_NEXT = Map.of(
            ElementType.METAL, ElementType.WATER,
            ElementType.WATER, ElementType.WOOD,
            ElementType.WOOD, ElementType.FIRE,
            ElementType.FIRE, ElementType.EARTH,
            ElementType.EARTH, ElementType.METAL
    );

    private LockGridGenerator() {
    }

    public static int[] gradeSpec(int grade) {
        int index = Math.max(1, grade) - 1;
        if (index < GRADE_SPECS.length) {
            return GRADE_SPECS[index].clone();
        }
        int extra = grade - 4;
        return new int[]{20 + extra * 4, 8 + extra * 2, 5 + extra * 2, 8 + extra * 2};
    }

    /** 真流光周期（tick）。 */
    public static int cycleTicks(int grade) {
        return gradeSpec(grade)[3] * 20;
    }

    /**
     * @param grade      锁品阶
     * @param depth      当前层深（0 = 最外层；深层节点 +2、假光 +1）
     * @param soulDiff   破解者魂强 − 刻印者快照（决定雾区）
     * @param deathPact  死契态假流光 ×1.5
     * @param boneSeed   纹骨种子（sealId + 层序派生，永久）
     * @param attemptSeed 变式种子（nonce 派生，每次尝试不同）
     */
    public static LockGridBoard generate(
            int grade,
            int depth,
            int soulDiff,
            boolean deathPact,
            long boneSeed,
            long attemptSeed
    ) {
        int[] spec = gradeSpec(grade);
        int nodeCount = spec[0] + depth * 2;
        int pathLength = spec[1] + Math.max(0, depth);
        int fakeCount = (int) Math.ceil((spec[2] + depth) * (deathPact ? 1.5D : 1.0D));
        boolean shengConstraint = grade >= 2;

        Random boneRandom = new Random(boneSeed);
        for (int attempt = 0; attempt < 80; attempt++) {
            BoneLayout bone = generateBone(boneRandom, nodeCount, pathLength, shengConstraint);
            if (bone == null) {
                continue;
            }
            Random attemptRandom = new Random(attemptSeed ^ (attempt * 0x9E3779B97F4A7C15L));
            List<List<Integer>> fakes = generateFakes(attemptRandom, bone, pathLength, fakeCount);
            if (fakes.size() < Math.min(1, fakeCount)) {
                continue;
            }
            List<Node> nodes = applyFog(attemptRandom, bone, soulDiff);
            return new LockGridBoard(nodes, bone.edges, bone.path, fakes);
        }
        // 兜底：一阶教学规格必然可生成
        if (nodeCount > 9) {
            return generate(1, 0, soulDiff, deathPact, boneSeed + 1L, attemptSeed + 1L);
        }
        throw new IllegalStateException("Lock grid generation failed even at the tutorial spec");
    }

    private record BoneLayout(
            List<RawNode> rawNodes,
            List<int[]> edges,
            List<List<Integer>> adjacency,
            List<Integer> path
    ) {
    }

    private record RawNode(int x, int y, int ring, ElementType element) {
    }

    private static BoneLayout generateBone(
            Random random,
            int nodeCount,
            int pathLength,
            boolean shengConstraint
    ) {
        int center = LockGridBoard.VIRTUAL_SIZE / 2;
        // 双环/三环布点（原型 rings 参数）
        int[][] rings;
        if (nodeCount <= 12) {
            int inner = (int) Math.ceil(nodeCount * 0.35D);
            rings = new int[][]{{inner, 95}, {nodeCount - inner, 185}};
        } else {
            int inner = (int) Math.round(nodeCount * 0.22D);
            int middle = (int) Math.round(nodeCount * 0.36D);
            rings = new int[][]{{inner, 78}, {middle, 150}, {nodeCount - inner - middle, 225}};
        }
        List<double[]> positions = new ArrayList<>(nodeCount);
        List<Integer> ringOf = new ArrayList<>(nodeCount);
        for (int ring = 0; ring < rings.length; ring++) {
            int count = rings[ring][0];
            int radius = rings[ring][1];
            for (int k = 0; k < count; k++) {
                double angle = (double) k / count * Math.PI * 2.0D
                        + range(random, -0.14D, 0.14D) + ring * 0.5D;
                double x = center + Math.cos(angle) * (radius + range(random, -13.0D, 13.0D));
                double y = center + Math.sin(angle) * (radius + range(random, -11.0D, 11.0D)) * 0.86D;
                positions.add(new double[]{x, y});
                ringOf.add(ring);
            }
        }

        // 邻接：环内相邻 + 跨环最近两点
        List<Set<Integer>> adjacencySets = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            adjacencySets.add(new LinkedHashSet<>());
        }
        int offset = 0;
        for (int[] ring : rings) {
            int count = ring[0];
            for (int k = 0; k < count; k++) {
                int a = offset + k;
                int b = offset + (k + 1) % count;
                if (a != b) {
                    adjacencySets.get(a).add(b);
                    adjacencySets.get(b).add(a);
                }
            }
            offset += count;
        }
        for (int i = 0; i < nodeCount; i++) {
            final int from = i;
            List<Integer> candidates = new ArrayList<>();
            for (int j = 0; j < nodeCount; j++) {
                if (j != i && !ringOf.get(j).equals(ringOf.get(i))) {
                    candidates.add(j);
                }
            }
            candidates.sort((a, b) -> Double.compare(
                    distanceSquared(positions, from, a),
                    distanceSquared(positions, from, b)
            ));
            for (int k = 0; k < Math.min(2, candidates.size()); k++) {
                int j = candidates.get(k);
                adjacencySets.get(i).add(j);
                adjacencySets.get(j).add(i);
            }
        }
        List<List<Integer>> adjacency = new ArrayList<>(nodeCount);
        for (Set<Integer> set : adjacencySets) {
            adjacency.add(List.copyOf(set));
        }

        // 主径：随机游走
        List<Integer> path = null;
        for (int t = 0; t < 220 && path == null; t++) {
            List<Integer> walk = new ArrayList<>();
            Set<Integer> used = new HashSet<>();
            int start = random.nextInt(nodeCount);
            walk.add(start);
            used.add(start);
            while (walk.size() < pathLength) {
                List<Integer> next = new ArrayList<>();
                for (int neighbor : adjacency.get(walk.get(walk.size() - 1))) {
                    if (!used.contains(neighbor)) {
                        next.add(neighbor);
                    }
                }
                if (next.isEmpty()) {
                    break;
                }
                int chosen = next.get(random.nextInt(next.size()));
                walk.add(chosen);
                used.add(chosen);
            }
            if (walk.size() == pathLength) {
                path = walk;
            }
        }
        if (path == null) {
            return null;
        }

        // 染色：二阶起主径严格相生序；一阶教学锁纯随机
        ElementType[] elements = new ElementType[nodeCount];
        List<ElementType> five = ElementType.FIVE_ELEMENTS;
        if (shengConstraint) {
            ElementType current = five.get(random.nextInt(five.size()));
            for (int index : path) {
                elements[index] = current;
                current = SHENG_NEXT.get(current);
            }
        } else {
            for (int index : path) {
                elements[index] = five.get(random.nextInt(five.size()));
            }
        }
        Set<Integer> onPath = new HashSet<>(path);
        for (int i = 0; i < nodeCount; i++) {
            if (elements[i] == null) {
                elements[i] = five.get(random.nextInt(five.size()));
            }
        }
        // 迷惑染色：主径邻居 55% 概率染"相生后继"，让推理有噪音
        if (shengConstraint) {
            for (int step = 0; step < path.size() - 1; step++) {
                int pathNode = path.get(step);
                for (int neighbor : adjacency.get(pathNode)) {
                    if (!onPath.contains(neighbor) && random.nextDouble() < 0.55D) {
                        elements[neighbor] = SHENG_NEXT.get(elements[pathNode]);
                    }
                }
            }
        }

        List<RawNode> rawNodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            rawNodes.add(new RawNode(
                    (int) Math.round(positions.get(i)[0]),
                    (int) Math.round(positions.get(i)[1]),
                    ringOf.get(i),
                    elements[i]
            ));
        }
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            for (int j : adjacency.get(i)) {
                if (j > i) {
                    edges.add(new int[]{i, j});
                }
            }
        }
        return new BoneLayout(rawNodes, edges, adjacency, path);
    }

    /** 假流光：同引位起步、80% 取相生后继（"看似合理"但必有跳环破绽），与主径重叠 ≤60%。 */
    private static List<List<Integer>> generateFakes(
            Random random,
            BoneLayout bone,
            int pathLength,
            int fakeCount
    ) {
        List<List<Integer>> fakes = new ArrayList<>();
        for (int f = 0; f < fakeCount; f++) {
            List<Integer> best = null;
            for (int t = 0; t < 60 && best == null; t++) {
                int targetLength = Math.max(4, pathLength + random.nextInt(3) - 1);
                List<Integer> walk = new ArrayList<>();
                Set<Integer> used = new HashSet<>();
                int start = bone.path.get(0);
                walk.add(start);
                used.add(start);
                while (walk.size() < targetLength) {
                    int current = walk.get(walk.size() - 1);
                    List<Integer> next = new ArrayList<>();
                    for (int neighbor : bone.adjacency.get(current)) {
                        if (!used.contains(neighbor)) {
                            next.add(neighbor);
                        }
                    }
                    if (next.isEmpty()) {
                        break;
                    }
                    List<Integer> sheng = new ArrayList<>();
                    ElementType expected = SHENG_NEXT.get(bone.rawNodes.get(current).element());
                    for (int neighbor : next) {
                        if (bone.rawNodes.get(neighbor).element() == expected) {
                            sheng.add(neighbor);
                        }
                    }
                    int chosen = (!sheng.isEmpty() && random.nextDouble() < 0.8D)
                            ? sheng.get(random.nextInt(sheng.size()))
                            : next.get(random.nextInt(next.size()));
                    walk.add(chosen);
                    used.add(chosen);
                }
                if (walk.size() < 4) {
                    continue;
                }
                int overlap = 0;
                for (int index = 0; index < walk.size(); index++) {
                    if (index < bone.path.size() && bone.path.get(index).equals(walk.get(index))) {
                        overlap++;
                    }
                }
                if ((double) overlap / walk.size() <= 0.6D) {
                    best = walk;
                }
            }
            if (best != null) {
                fakes.add(best);
            }
        }
        return fakes;
    }

    /** 雾区（阵禁篇 5.2.4）：魂强差 −1 → 30% 节点笼雾；≤ −2 → 全图重雾。引位永不笼雾。 */
    private static List<Node> applyFog(Random random, BoneLayout bone, int soulDiff) {
        List<Node> nodes = new ArrayList<>(bone.rawNodes.size());
        int startNode = bone.path.get(0);
        for (int i = 0; i < bone.rawNodes.size(); i++) {
            RawNode raw = bone.rawNodes.get(i);
            boolean fog = false;
            if (i != startNode) {
                if (soulDiff <= -2) {
                    fog = true;
                } else if (soulDiff == -1) {
                    fog = random.nextDouble() < 0.3D;
                }
            }
            nodes.add(new Node(raw.x(), raw.y(), raw.element(), fog));
        }
        return nodes;
    }

    private static double range(Random random, double minimum, double maximum) {
        return minimum + random.nextDouble() * (maximum - minimum);
    }

    private static double distanceSquared(List<double[]> positions, int a, int b) {
        double dx = positions.get(a)[0] - positions.get(b)[0];
        double dy = positions.get(a)[1] - positions.get(b)[1];
        return dx * dx + dy * dy;
    }
}
