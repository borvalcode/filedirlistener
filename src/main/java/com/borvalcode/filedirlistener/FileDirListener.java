package com.borvalcode.filedirlistener;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class FileDirListener {
    private final Thread listenerThread;

    private FileDirListener(Builder builder) {
        listenerThread =
                new Thread(
                        new Routine(
                                builder.directory,
                                builder.creates,
                                builder.updates,
                                builder.deletes));
    }

    public void start() {
        listenerThread.start();
    }

    public void stop() {
        listenerThread.interrupt();
    }

    public static Builder builder(String directory) {
        return new Builder(directory);
    }

    public static final class Builder {
        private final String directory;
        private final Map<String, Consumer<Path>> creates = new HashMap<>();
        private final Map<String, Consumer<Path>> updates = new HashMap<>();
        private final Map<String, Consumer<Path>> deletes = new HashMap<>();

        private Builder(String directory) {
            this.directory = directory;
        }

        public Builder onCreate(String pattern, Consumer<Path> callback) {
            creates.put(pattern, callback);
            return this;
        }

        public Builder onUpdate(String pattern, Consumer<Path> callback) {
            updates.put(pattern, callback);
            return this;
        }

        public Builder onDelete(String pattern, Consumer<Path> callback) {
            deletes.put(pattern, callback);
            return this;
        }

        public FileDirListener build() {
            return new FileDirListener(this);
        }
    }

    private static class Routine implements Runnable {
        private final String directory;
        private final Map<String, Consumer<Path>> creates;
        private final Map<String, Consumer<Path>> updates;
        private final Map<String, Consumer<Path>> deletes;

        private Routine(
                String directory,
                Map<String, Consumer<Path>> creates,
                Map<String, Consumer<Path>> updates,
                Map<String, Consumer<Path>> deletes) {
            this.directory = directory;
            this.creates = creates;
            this.updates = updates;
            this.deletes = deletes;
        }

        @Override
        public void run() {
            try {
                FileSystem fileSystem = FileSystems.getDefault();
                WatchService watchService = fileSystem.newWatchService();

                Path dir = Paths.get(directory);
                dir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);

                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException ex) {
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();

                        if (kind == ENTRY_MODIFY) {
                            executeCallback(updates, fileName);
                        } else if (kind == ENTRY_CREATE) {
                            executeCallback(creates, fileName);
                        } else if (kind == ENTRY_DELETE) {
                            executeCallback(deletes, fileName);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }

            } catch (Exception e) {

            }
        }

        private void executeCallback(Map<String, Consumer<Path>> map, Path fileName) {
            find(map, fileName).ifPresent(pathConsumer -> pathConsumer.accept(fileName));
        }

        private static Optional<Consumer<Path>> find(
                Map<String, Consumer<Path>> map, Path fileName) {
            return map.entrySet().stream()
                    .filter(
                            entry -> {
                                String pattern = entry.getKey();
                                return fileName.toString().matches(pattern);
                            })
                    .findFirst()
                    .map(Map.Entry::getValue);
        }
    }
}
