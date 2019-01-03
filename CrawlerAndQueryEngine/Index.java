package CrawlerAndQueryEngine;

import java.io.*;

/**
 * A serializable index, using Java's native Serializable interface and ObjectStream.  Provides
 * methods to load and save indexes.
 */
public class Index implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Loads an Index from the given file, throwing an exception if there is an error during the
     * loading process. The actual concrete type of the Index will be automatically determined by
     * the ObjectStream.
     * @param filename The file to load the index from.
     */
    public static Index load(String filename) throws IOException, ClassNotFoundException {
        // Uses Java 7's try-with-resources to attempt to open the file, automatically closing it
        // upon completion or failure.
        try(ObjectInputStream oin = new ObjectInputStream(new FileInputStream(filename))) {
            return (Index) oin.readObject();
        }
    }

    /**
     * Saves an Index to the given file, throwing an exception if there is an error during the
     * saving process.
     */
    public void save(String filename) throws IOException {
        // Uses Java 7's try-with-resources to attempt to open the file, automatically closing it
        // upon completion or failure.
        try(ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(filename))) {
            oout.writeObject(this);
        }
    }
}
