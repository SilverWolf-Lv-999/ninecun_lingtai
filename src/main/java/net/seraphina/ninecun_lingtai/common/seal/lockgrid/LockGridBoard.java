package net.seraphina.ninecun_lingtai.common.seal.lockgrid;

import net.seraphina.ninecun_lingtai.api.element.ElementType;

import java.util.List;

/**
 * 一层锁纹的服务端盘面。主径与假流光路径是秘密——只有节点/边/雾区经
 * OpenLockGridPacket 下发，流光以渲染帧事件包按周期下发。
 *
 * @param nodes 节点（虚拟坐标系 430×430，客户端按屏幕缩放）
 * @param edges 邻接边（a &lt; b 的索引对）
 * @param path  主径节点索引序列（答案，绝不下发全量）
 * @param fakes 假流光路径（走相生序但会跳环）
 */
public record LockGridBoard(
        List<Node> nodes,
        List<int[]> edges,
        List<Integer> path,
        List<List<Integer>> fakes
) {
    /** 虚拟盘面边长（与原型画布一致，参数原样移植）。 */
    public static final int VIRTUAL_SIZE = 430;

    public record Node(int x, int y, ElementType element, boolean fog) {
    }

    public int startNode() {
        return this.path.get(0);
    }
}
