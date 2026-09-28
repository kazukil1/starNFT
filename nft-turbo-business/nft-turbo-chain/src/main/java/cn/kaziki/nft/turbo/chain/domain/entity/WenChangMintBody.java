package cn.kaziki.nft.turbo.chain.domain.entity;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 铸造请求体
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WenChangMintBody extends WenChangRequestBody{

    @JSONField(name = "name")
    private String name;

    @JSONField(name = "recipient")
    private String  recipient;

}
