package com.liquorshop.inventory.exception;

public class ResourceNotFoundException extends RuntimeException {


    public ResourceNotFoundException(String err) {
        super(err);
    }

}
