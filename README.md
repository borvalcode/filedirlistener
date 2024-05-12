# File dir listener

## Use

```
        String directory = "path/to/directory";
        
        FileDirListener listener =
                FileDirListener.builder(directory)
                        .onUpdate(".*", path -> System.out.println("Updated: " + path))
                        .onCreate(".*.txt", path -> System.out.println("Created: " + path))
                        .onDelete("foo.bar", path -> System.out.println("Deleted: " + path))
                        .build();

        listener.start();
        
        // Do stuff
        
        listener.stop();
```