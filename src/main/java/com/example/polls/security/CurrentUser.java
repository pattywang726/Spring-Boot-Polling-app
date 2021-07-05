package com.example.polls.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.lang.annotation.*;

// The following CurrentUser annotation is a wrapper around
// @AuthenticationPrincipal annotation.
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal

//Custom annotation to access currently logged in user
public @interface CurrentUser {

}
