package cn.kaziki.nft.turbo.file;

import java.io.InputStream;

/**
 * 文件 服务
 */
public interface FileService {

    /**
     * 文件上传
     * @param path
     * @param fileStream
     * @return
     */
    public boolean upload(String path, InputStream fileStream);

}
