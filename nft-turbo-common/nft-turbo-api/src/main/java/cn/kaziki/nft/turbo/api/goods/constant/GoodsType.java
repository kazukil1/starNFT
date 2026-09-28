package cn.kaziki.nft.turbo.api.goods.constant;

/**
 * 商品类型
 */
public enum GoodsType {

    // 藏品
    COLLECTION("藏品"),

    // 盲盒
    BLIND_BOX("盲盒"),

    // 星尘
    STAR("星尘");


    private String value;

    GoodsType(String value) {
        this.value = value;
    }
}
