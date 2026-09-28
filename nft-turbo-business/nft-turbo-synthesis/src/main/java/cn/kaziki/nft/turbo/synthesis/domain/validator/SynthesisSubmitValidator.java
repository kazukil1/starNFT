package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;

/**
 * 合成提交校验器接口（责任链模式）
 */
public interface SynthesisSubmitValidator {

    /** 设置下一个校验器 */
    void setNext(SynthesisSubmitValidator nextValidator);

    /** 返回下一个校验器 */
    SynthesisSubmitValidator getNext();

    /**
     * 执行校验，失败抛 SynthesisException 终止链路
     */
    void validate(SynthesisSubmitRequest request) throws SynthesisException;
}
