package com.borvalcode.filedirlistener;

public class Main {

    public static void main(String[] args) {
        String directory = args.length > 0 ? args[0] : "src/main/resources";

        FileDirListener listener =
                FileDirListener.builder(directory)
                        .onUpdate(".*", path -> System.out.println("Updated: " + path))
                        .onCreate(".*", path -> System.out.println("Created: " + path))
                        .onDelete(".*", path -> System.out.println("Deleted: " + path))
                        .build();

        listener.start();
    }
}
