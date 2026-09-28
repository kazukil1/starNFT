package cn.kaziki.nft.turbo.synthesis.infrastructure.config;

import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.synthesis.domain.validator.MaterialValidator;
import cn.kaziki.nft.turbo.synthesis.domain.validator.RecipeValidator;
import cn.kaziki.nft.turbo.synthesis.domain.validator.StarBalanceValidator;
import cn.kaziki.nft.turbo.synthesis.domain.validator.SynthesisSubmitValidator;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * 合成校验器链配置 — 参照订单模块 OrderClientConfiguration 模式
 */
@Configuration
public class SynthesisValidatorConfiguration {

    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public RecipeValidator recipeValidator(SynthesisRecipeMapper synthesisRecipeMapper) {
        return new RecipeValidator(synthesisRecipeMapper);
    }

    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public MaterialValidator materialValidator(CollectionReadFacadeService collectionReadFacadeService,
                                                SynthesisRecipeMapper synthesisRecipeMapper) {
        return new MaterialValidator(collectionReadFacadeService, synthesisRecipeMapper);
    }

    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public StarBalanceValidator starBalanceValidator(StarAccountFacadeService starAccountFacadeService,
                                                      SynthesisRecipeMapper synthesisRecipeMapper) {
        return new StarBalanceValidator(starAccountFacadeService, synthesisRecipeMapper);
    }

    /**
     * 合成提交校验链：配方 → 材料 → 星尘余额
     */
    @Bean
    @Primary
    public SynthesisSubmitValidator synthesisValidatorChain(RecipeValidator recipeValidator,
                                                             MaterialValidator materialValidator,
                                                             StarBalanceValidator starBalanceValidator) {
        recipeValidator.setNext(materialValidator);
        materialValidator.setNext(starBalanceValidator);
        return recipeValidator;
    }
}
