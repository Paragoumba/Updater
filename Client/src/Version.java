import java.util.Arrays;

class Version {

    Version(String version){

        int bIndex = -1;

        if (version.contains("b")) {

            bIndex = version.lastIndexOf('b');
            build = Integer.parseInt(version.substring(bIndex + 1, version.length()));

        }

        version = version.substring(0, bIndex != -1 ? bIndex : version.length());
        String[] splitVersion = version.split("\\.");
        numbers = new int[splitVersion.length];

        if (build < 0) build = 0;

        for (int i = 0; i < splitVersion.length; ++i){

            numbers[i] = Integer.parseInt(splitVersion[i]);

        }
    }

    private int[] numbers;
    private int build;

    boolean isNewer(Version otherVersion){

        if (equals(otherVersion)) return false;

        if (build != 0 && otherVersion.build != 0){

            if (build > otherVersion.build) return true;
            if (build < otherVersion.build) return true;

        }

        for (int i = 0; i < numbers.length; ++i){

            if (numbers[i] > otherVersion.numbers[i]) return true;
            if (numbers[i] < otherVersion.numbers[i]) return false;
            if (numbers.length == i + 1) return false;

        }

        return true;

    }

    private boolean equals(Version version) {

        if (version.numbers.length != numbers.length || (version.build != 0 && build != 0 && version.build != build)) return false;

        for (int i = 0; i < numbers.length; ++i) if (numbers[i] != version.numbers[i]) return false;

        return true;

    }

    @Override
    public String toString() {

        String version = Arrays.toString(numbers);

        return version.replace("[", "").replace("]", "").replace(", ", ".") + (build != 0 ? 'b' + build : "");

    }
}
