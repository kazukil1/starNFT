package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;

/**
 * 合成提交校验器抽象基类 — 模板方法：doValidate → next.validate
 */
public abstract class BaseSynthesisSubmitValidator implements SynthesisSubmitValidator {

    protected SynthesisSubmitValidator nextValidator;

    @Override
    public void setNext(SynthesisSubmitValidator nextValidator) {
        this.nextValidator = nextValidator;
    }

    @Override
    public SynthesisSubmitValidator getNext() {
        return nextValidator;
    }

    @Override
    public void validate(SynthesisSubmitRequest request) throws SynthesisException {
        doValidate(request);
        if (nextValidator != null) {
            nextValidator.validate(request);
        }
    }

    /** 子类实现具体校验逻辑，失败抛 SynthesisException */
    protected abstract void doValidate(SynthesisSubmitRequest request) throws SynthesisException;
}
