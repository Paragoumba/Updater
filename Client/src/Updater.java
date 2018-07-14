import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Updater {

    private final static String serverUrl = "http://localhost/updater/";
    private final static File localDir = new File("D:/Documents/Dev/Dev-Java/Code/Snippets/Updater/local");

    private final static HashMap<String, String> localFiles = new HashMap<>();

    private static Version appVersion = new Version("0");
    private static Version latestVersion = new Version("0");

    public static void main(String[] args) {

        try {

            File localVersion = new File(localDir, "version");

            if (localVersion.exists() && localVersion.isFile()) {

                System.out.println("versioon");

                try(FileInputStream fis = new FileInputStream(localVersion);
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader in = new BufferedReader(isr)) {

                    appVersion = new Version(in.readLine());

                }
            }

            System.out.println("Actual version: " + appVersion);

            String latestURL = serverUrl + "latest";
            System.out.println("Getting latest version on " + latestURL);
            latestVersion = new Version(getRemoteFileContent(latestURL));
            System.out.println("Latest version is " + latestVersion);

            try {

                if (latestVersion.isNewer(appVersion)) update(latestVersion);
                else System.out.println("App is already newest version.");

                // /index.html v1.0 47f3023df6e1b8b55d86c30551e2399b2329fdd8
                // /index.html v2.0 ae23bc867d811feb7a0468ad67f5f02d190c21e0

            } catch (IOException e){

                System.out.println("Version doesn't exists.");
                e.printStackTrace();

            } catch (NoSuchAlgorithmException e){

                System.out.println("Can't calculate file hash.");

            }

        } catch (IOException e) {

            System.out.println("Connection impossible.");

        }
    }

    private static String getRemoteFileContent(String path) throws IOException {

        URL url = new URL(path);
        URLConnection urlConnection = url.openConnection();
        StringBuilder builder = new StringBuilder();

        urlConnection.connect();

        try(InputStream in = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr)){

            String line;

            while ((line = br.readLine()) != null){

                builder.append(line).append('\n');

            }

            builder.replace(builder.lastIndexOf("\n"), builder.length(), "");

        }

        return builder.toString();

    }

    private static void calculateFileHash(File file) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-1");

        try (FileInputStream in = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(in, md)) {

            //noinspection StatementWithEmptyBody
            while (dis.read() > -1) ;

            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();

            for (byte b : hash) sb.append(String.format("%02X", b));

            String absolutePath = file.getAbsolutePath();

            localFiles.put(absolutePath.substring(localDir.getAbsolutePath().length(), absolutePath.length()).replace("\\", "/"), sb.toString().toLowerCase());

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    private static String getKey(String value){

        for (Entry<String, String> entry : Updater.localFiles.entrySet()){

            if (entry.getValue().equals(value)) return entry.getKey();

        }

        return null;

    }

    private static void createNewDirs(String relativeFilePath){

        String[] parents = relativeFilePath.split("/");

        if (parents.length > 2){

            File parentDir = localDir;

            for (int i = 1; i < parents.length - 1; ++i) {

                parentDir = new File(parentDir, parents[i]);

                if (!parentDir.exists()) parentDir.mkdir();

            }
        }
    }

    private static void exploreDirectory(File directory) throws NoSuchAlgorithmException {

        File[] files = directory.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.isFile() && !file.getName().equals("version")) calculateFileHash(file);
                else if (file.isDirectory()) exploreDirectory(file);

            }
        }
    }

    private static void update(Version version) throws IOException, NoSuchAlgorithmException {

        System.out.println("Getting update on " + serverUrl + version + "/update");

        Yaml yaml = new Yaml();
        HashMap<String, String> remoteFiles = (HashMap<String, String>) yaml.load(getRemoteFileContent(serverUrl + version + "/update"));exploreDirectory(localDir);

        for (Entry<String, String> remoteEntry : remoteFiles.entrySet()){

            String localFileHash = localFiles.get(remoteEntry.getKey());
            String localFileRelativePath = getKey(localFileHash);

            System.out.println("localPath: " + localFileRelativePath + ", remotePath: " + remoteEntry.getKey());
            System.out.println("localHash: " + localFileHash + ", remoteHash: " + remoteEntry.getValue());

            /*if (localFileHash == null || !localFileHash.equalsIgnoreCase(remoteEntry.getValue())) {

                String remoteFilePath = serverUrl + version + remoteEntry.getKey();
                File file = new File(localDir, remoteEntry.getKey());

                if (file.exists()) //noinspection ResultOfMethodCallIgnored
                    file.delete();

                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                System.out.println("Downloading " + remoteFilePath);

                try(FileOutputStream fos = new FileOutputStream(file);
                    PrintWriter out = new PrintWriter(fos)){

                    out.print(getRemoteFileContent(remoteFilePath));

                }
            }*/

            if (!remoteEntry.getValue().equals(localFileHash)){

                String remoteFilePath = serverUrl + version + remoteEntry.getKey();
                File file = new File(localDir, remoteEntry.getKey());

                if (file.exists()) //noinspection ResultOfMethodCallIgnored
                    file.delete();

                createNewDirs(remoteEntry.getKey());

                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                System.out.println("Downloading " + remoteFilePath);

                try(FileOutputStream fos = new FileOutputStream(file);
                    PrintWriter out = new PrintWriter(fos)){

                    out.print(getRemoteFileContent(remoteFilePath));

                }

            } else if (localFileHash.equals(remoteEntry.getValue()) && !remoteEntry.getKey().equals(localFileRelativePath) && localFileRelativePath != null){

                File oldFile = new File(localDir, localFileRelativePath);
                File newFile = new File(localDir, remoteEntry.getKey());

                System.out.println("Moving " + localFileRelativePath + " to " + remoteEntry.getKey());

                Files.move(oldFile.toPath(), newFile.toPath());

            }

        }

        for (Entry<String, String> localEntry : localFiles.entrySet()){

            String remoteFileHash = remoteFiles.get(localEntry.getKey());
            String remoteFileRelativePath = getKey(remoteFileHash);

            System.out.println("localPath: " + localEntry.getKey() + ", remotePath: " + remoteFileRelativePath);
            System.out.println("remoteHash: " + remoteFileHash + ", localHash: " + localEntry.getValue());

            if (remoteFileHash == null) {

                File file = new File(localDir, localEntry.getKey());

                System.out.println("Deleting " + file.getAbsolutePath());

                //noinspection ResultOfMethodCallIgnored
                file.delete();

            }
        }

        File versionFile = new File(localDir, "version");

        if (versionFile.exists()) versionFile.delete();

        try(FileWriter fw = new FileWriter(versionFile);
            BufferedWriter out = new BufferedWriter(fw)){

            out.write(latestVersion.toString());

        }
    }
}