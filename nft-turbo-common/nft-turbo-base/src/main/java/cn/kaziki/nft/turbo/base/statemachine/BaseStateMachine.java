package cn.kaziki.nft.turbo.base.statemachine;

import cn.kaziki.nft.turbo.base.exception.BizException;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import java.util.Map;

import static cn.kaziki.nft.turbo.base.exception.BizErrorCode.STATE_MACHINE_TRANSITION_FAILED;

/**
 *
 */
public class BaseStateMachine<STATE, EVENT> implements StateMachine<STATE, EVENT> {
    private Map<String, STATE> stateTransitions = Maps.newHashMap();

    // 键：原始状态_事件  值：目标状态
    protected void putTransition(STATE origin, EVENT event, STATE target) {
        stateTransitions.put(Joiner.on("_").join(origin, event), target);
    }

    // 验证状态转换是否合法
    @Override
    public STATE transition(STATE state, EVENT event) {
        STATE target = stateTransitions.get(Joiner.on("_").join(state, event));
        if (target == null) {
            throw new BizException("state = " + state + " , event = " + event, STATE_MACHINE_TRANSITION_FAILED);
        }
        return target;
    }
}
