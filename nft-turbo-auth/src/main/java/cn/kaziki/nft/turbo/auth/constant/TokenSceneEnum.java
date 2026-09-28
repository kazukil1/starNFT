package cn.kaziki.nft.turbo.auth.constant;

import java.util.Arrays;

/**
 * token获取的场景枚举
 */
public enum TokenSceneEnum {
    // 下单-藏品
    BUY_COLLECTION("buy"),

    // 下单-盲盒
    BUY_BLIND_BOX("buyBb"),

    // 下单-星尘
    BUY_STAR("buyStar");

    // 场景的值
    private String scene;

    TokenSceneEnum(String scene) {
        this.scene = scene;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public static TokenSceneEnum getByScene(String scene) {
        return Arrays.stream(TokenSceneEnum.values()).filter(tokenSceneEnum -> tokenSceneEnum.getScene().equals(scene)).findFirst().orElse(null);
    }
}
