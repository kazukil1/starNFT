package cn.kaziki.nft.turbo.chain.domain.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 创建文昌链请求体
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WenChangCreateBody extends WenChangRequestBody{

    @JSONField(name = "name")
    private String name;
}
