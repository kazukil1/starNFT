package cn.kaziki.nft.turbo.collection.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品销毁 入参
 */
@Getter
@Setter
public class DestroyParam {

    @NotNull(message = "heldCollectionId is null")
    private String heldCollectionId;

}
