package com.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason="No movie found on this search, try again")
public class NotFoundException extends RuntimeException {}