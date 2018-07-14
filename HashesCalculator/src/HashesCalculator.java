import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HashesCalculator {

    private static HashMap<String, String> pathsAndHashes = new HashMap<>();
    private static File updateDir;

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

        long start = System.currentTimeMillis();

        if (args.length < 1){

            System.out.println("You need to give the update's directory path as first argument");
            return;

        }

        updateDir = new File(args[0]);

        if (!updateDir.exists() || updateDir.isFile() && updateDir.list() == null){

            System.out.println("Update's directory doesn't exists or is a file");
            return;

        }

        File updateFile = new File(updateDir, "/update");

        if (updateFile.exists()) //noinspection ResultOfMethodCallIgnored
            updateFile.delete();

        System.out.println("Creating update file for update " + updateDir.getName());

        exploreDirectory(updateDir);

        Yaml yaml = new Yaml();

        try(FileOutputStream fos = new FileOutputStream(updateFile);
            PrintWriter out = new PrintWriter(fos)){

            yaml.dump(pathsAndHashes, out);

        }

        System.out.println("Calculated " + (pathsAndHashes.size()) + " hashes in " + ((System.currentTimeMillis() - start) / 1000d) + "s");

    }

    private static void exploreDirectory(File directory) throws NoSuchAlgorithmException {

        File[] files = directory.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.isFile() && !file.getName().equals("update")) calculateHash(file);
                else if (file.isDirectory()) exploreDirectory(file);

            }
        }
    }

    private static void calculateHash(File file) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-1");

        try(FileInputStream in = new FileInputStream(file);
            DigestInputStream dis = new DigestInputStream(in, md)){

            //noinspection StatementWithEmptyBody
            while (dis.read() > -1);

            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();

            for (byte b : bytes) sb.append(String.format("%02X", b));

            String absolutePath = file.getAbsolutePath();
            String relativePath = absolutePath.substring(updateDir.getAbsolutePath().length(), absolutePath.length()).replace("\\", "/");
            String hash = sb.toString().toLowerCase();

            System.out.println(relativePath + ": " + hash);

            pathsAndHashes.put(relativePath, hash);

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}
