package com.borvalcode.filedirlistener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class FileDirListenerTest {

    private static final String DIRECTORY = "src/test/resources";

    @Test
    void updating_a_file() throws Exception {
        createFile("config.txt");

        AtomicReference<Path> updated = new AtomicReference<>(null);

        FileDirListener listener =
                FileDirListener.builder(DIRECTORY).onUpdate("config.txt", updated::set).build();

        whileListening(
                listener,
                () -> {
                    editFile("config.txt");

                    Awaitility.await()
                            .atMost(Duration.ofSeconds(10))
                            .until(
                                    () ->
                                            updated.get() != null
                                                    && updated.get()
                                                            .toString()
                                                            .equals("config.txt"));
                });
    }

    @Test
    void deleting_a_file() throws Exception {
        createFile("config_del.txt");

        AtomicReference<Path> deleted = new AtomicReference<>(null);

        FileDirListener listener =
                FileDirListener.builder(DIRECTORY).onDelete(".*.del.txt", deleted::set).build();

        whileListening(
                listener,
                () -> {
                    deleteFile("config_del.txt");

                    Awaitility.await()
                            .atMost(Duration.ofSeconds(10))
                            .until(
                                    () ->
                                            deleted.get() != null
                                                    && deleted.get()
                                                            .toString()
                                                            .equals("config_del.txt"));
                });
    }

    @Test
    void creating_a_file() throws Exception {
        deleteFile("new_config.txt");

        AtomicReference<Path> created = new AtomicReference<>(null);

        FileDirListener listener =
                FileDirListener.builder(DIRECTORY).onCreate(".*", created::set).build();

        whileListening(
                listener,
                () -> {
                    createFile("new_config.txt");

                    Awaitility.await()
                            .atMost(Duration.ofSeconds(10))
                            .until(
                                    () ->
                                            created.get() != null
                                                    && created.get()
                                                            .toString()
                                                            .equals("new_config.txt"));
                });
    }

    private static void whileListening(FileDirListener listener, ThrowableRunnable runnable)
            throws Exception {
        listener.start();

        waitSomeTime();

        runnable.run();

        listener.stop();
    }

    private static void waitSomeTime() throws InterruptedException {
        Thread.sleep(2500);
    }

    private static void createFile(String fileName) throws IOException {
        File configFile = new File("src/test/resources/" + fileName);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(configFile)))) {
            writer.print(new Random().nextInt());
        }
    }

    private static void editFile(String fileName) throws IOException {
        File configFile = new File("src/test/resources/" + fileName);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(configFile)))) {
            writer.print(new Random().nextInt());
        }
    }

    private static boolean deleteFile(String fileName) {
        File configFile = new File("src/test/resources/" + fileName);
        return configFile.delete();
    }

    private interface ThrowableRunnable {
        void run() throws Exception;
    }
}
