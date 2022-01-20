package com.wiservoda.vims.msg.receive.entity;

import lombok.*;
import org.apache.commons.codec.binary.Base64;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"bytes"})
public class MessageFile {

    private Integer id;
    private String filename;
    private Long size;
    // len = 64
    private byte[] testPointId;
    private byte[] bytes;

    public String getBase64() {
        return Base64.encodeBase64String(bytes);
    }
}
