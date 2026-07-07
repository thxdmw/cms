package com.thx.module.admin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogMessage {

    private String id;

    private Date timestamp;

    private String level;

    private String logger;

    private String message;
}
