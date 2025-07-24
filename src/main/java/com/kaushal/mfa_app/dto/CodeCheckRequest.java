package com.kaushal.mfa_app.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeCheckRequest {

    private String email;
    private String sixDigitCode;

}
