package com.zj.compress;

public class FileOutOfSizeException extends Throwable {

    FileOutOfSizeException(long limited, String cases) {
        super("You has set limited size = " + limited + " ,error case : " + cases);
    }
}
