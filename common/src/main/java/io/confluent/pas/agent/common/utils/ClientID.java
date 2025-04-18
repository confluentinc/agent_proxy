package io.confluent.pas.agent.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utility class to manage a unique client ID for a process.
 */
public class ClientID {

    // Path to the file where the client ID will be stored
    private static final String ID_FILE_PATH = ".pid";

    /**
     * Retrieves the existing client ID from the file or generates a new one if the file does not exist.
     *
     * @return The client ID as a String.
     */
    public static String getOrCreateClientId() {
        File idFile = new File(ID_FILE_PATH);

        // Check if the ID file already exists
        if (idFile.exists()) {
            try {
                // Read and return the existing client ID from the file
                return new String(Files.readAllBytes(Paths.get(ID_FILE_PATH)));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read client ID file", e);
            }
        } else {
            // Generate a new unique client ID
            String newId = UUID.randomUUID().toString();
            try {
                // Write the new client ID to the file
                Files.write(Paths.get(ID_FILE_PATH), newId.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write client ID file", e);
            }

            // Return the new client ID
            return newId;
        }
    }
}