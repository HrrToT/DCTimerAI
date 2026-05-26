package com.dctimer.model;

/**
 * 不可变 solve 快照 —— 在 solve 完成瞬间冻结生成，
 * 用作成绩保存和回放数据的唯一来源，不受后续实时状态变化影响。
 *
 * 对应 228design 中的 SolveSnapshot 语义。
 */
public class SolveSnapshot {
    private final int result;
    private final SmartCubeSolveReconstruction reconstruction;

    public SolveSnapshot(int result, SmartCubeSolveReconstruction reconstruction) {
        this.result = result;
        this.reconstruction = reconstruction;
    }

    /** solve 原始累计时间（毫秒） */
    public int getResult() {
        return result;
    }

    public SmartCubeSolveReconstruction getReconstruction() {
        return reconstruction;
    }

    /** 重建后的步数（含 EMS 压缩，不含转体） */
    public int getReconstructedMovesCount() {
        return reconstruction == null ? 0 : reconstruction.getMoveCount();
    }

    /**
     * 返回带阶段标注和统计的解法文本（存入 DB moves 列）。
     * 等价于旧版 SmartCube.getMoveSequence()。
     */
    public String getMoveSequence() {
        if (reconstruction == null) return null;
        String ps = reconstruction.getPrettySolve(result);
        if (ps == null || ps.isEmpty()) return null;
        return ps;
    }

    /**
     * 返回完整的 solve_meta JSON 字符串（存入 DB solve_meta 列）。
     * 等价于旧版 SmartCube.getSolveMeta()。
     */
    public String getSolveMeta() {
        if (reconstruction == null) return null;
        return reconstruction.toJson(result);
    }
}
