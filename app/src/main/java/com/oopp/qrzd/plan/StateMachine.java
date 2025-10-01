package com.oopp.qrzd.plan;

/**
 * 有限状态机（含超时、自救）
 */
public final class StateMachine {
    public enum State { HOME, DAILY_MENU, BATTLE, RESULT, UNKNOWN }
    public List<Action> step(FrameArtifacts fa, long nowMs); // 输出待执行动作序列
}