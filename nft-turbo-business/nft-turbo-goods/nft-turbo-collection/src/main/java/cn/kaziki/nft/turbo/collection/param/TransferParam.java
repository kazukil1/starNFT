package cn.kaziki.nft.turbo.collection.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品转让 入参
 */
@Getter
@Setter
public class TransferParam {

    @NotNull(message = "heldCollectionId is null")
    private String heldCollectionId;

    @NotNull(message = "recipientUserId is null")
    private String recipientUserId;

}
