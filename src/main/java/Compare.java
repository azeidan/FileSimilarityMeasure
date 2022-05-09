import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LongestCommonSubsequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compare {

    /**
     * Array containing the result table's header
     * The Object[] is to satisfy the warning when the array is used with printf
     */
    private static final Object[] ARR_HEADER_LABELS = new Object[]{"", "Jaccard", "Jaro", "LCS", "Fuzzy", "File 1", "File 2"};

    /**
     * Column width for scores
     */
    private static final int COL_WIDTH_SCORE = 7;

    /**
     * Array containing the column widths of the result table. Float entries to use the "%f" string formatter instead of "%s
     */
    private static final Number[] ARR_COL_WIDTHS = new Number[]{0, (float) COL_WIDTH_SCORE, (float) COL_WIDTH_SCORE, COL_WIDTH_SCORE, COL_WIDTH_SCORE, 0, 0};

    public Compare(final String baseDir, final String nameRegEx) throws IOException {

        final long start = System.currentTimeMillis();

        final Pattern patternName = Pattern.compile(nameRegEx);

        final Path pathDir = Paths.get(baseDir);

        final List<Path> listFiles;

        try (Stream<Path> br = Files.walk(pathDir)) {

            listFiles = br.filter(path -> !Files.isDirectory(path))
                    .filter(path -> patternName.matcher(path.getFileName().toString()).matches())
                    .sorted()
                    .toList();
        }

//        listFiles.forEach(System.out::println);
//        System.exit(0);

        // record the maximum length needed to print the files' paths
        int colWidPath = 0;

        int counter = 1; // updated after each successful comparison
        final double compareSize = Math.pow(listFiles.size(), 2); // maximum number of comparisons

        final ArrayList<CompareResult> listResults = new ArrayList<>();

        for (Path pathSource : listFiles) {

            colWidPath = Math.max(colWidPath, pathSource.toAbsolutePath().toString().length());

            for (Path pathDest : listFiles) {

                System.out.printf("\r%.2f%%", (counter++ / compareSize * 100));

                if (!pathSource.equals(pathDest)) {

                    CompareResult compareResult = new CompareResult(pathSource, pathDest);

                    if (!listResults.contains(compareResult))
                        try {

                            String strSource = getLinesAsString(pathSource);
                            String strDestination = getLinesAsString(pathDest);

                            compareResult.update(new JaccardSimilarity().apply(strSource, strDestination),
                                    new JaroWinklerSimilarity().apply(strSource, strDestination),
                                    new LongestCommonSubsequence().apply(strSource, strDestination),
                                    new FuzzyScore(Locale.ENGLISH).fuzzyScore(strSource, strDestination));

                            listResults.add(compareResult);
                        } catch (Exception ex) {

                            System.err.printf("%nSource: %s%nDestination: %s", pathSource, pathDest);

                            ex.printStackTrace();
                            System.exit(0);
                        }
                }
            }
        }

        System.out.println('\n'); // separates the progress counter from subsequent prints

        // update array holding the column widths
        ARR_COL_WIDTHS[0] = String.format("%.0f", compareSize).length(); // column width for printing the sequence is based on the # of comparisons done
        ARR_COL_WIDTHS[5] = ARR_COL_WIDTHS[6] = colWidPath;

        final String rowSeparator = "+" +
                Arrays.stream(ARR_COL_WIDTHS)
                        .map(this::buildSeparator)
                        .collect(Collectors.joining("+")) +
                "+";

        System.out.println(rowSeparator);

        final String headerFormatters = "|" +
                Arrays.stream(ARR_COL_WIDTHS)
                        .map(colWid -> " %-" + colWid.intValue() + "s |")
                        .collect(Collectors.joining(""))
                + "%n";

        System.out.printf(headerFormatters, ARR_HEADER_LABELS);
        System.out.println(rowSeparator);

        counter = 1;

        listResults.sort(Comparator.reverseOrder());

        final String rowFormatters = "|" +
                Arrays.stream(ARR_COL_WIDTHS)
                        .map(colWid -> " %-" + colWid.intValue() + (colWid instanceof Float ? ".5f" : "s") + " |")
                        .collect(Collectors.joining(""))
                + " %s  %s  %s%n%s%n"; // extra %s for winmerge, file1 path, file2 path, and separator

        for (CompareResult compareResult : listResults)
            System.out.printf(rowFormatters,
                    counter++,
                    compareResult.scoreJaccard,
                    compareResult.scoreJaro,
                    compareResult.scoreLCS,
                    compareResult.scoreFuzzy,
                    compareResult.pathFirstFile,
                    compareResult.pathSecondFile,
                    "winmerge ",
                    compareResult.pathFirstFile,
                    compareResult.pathSecondFile,
                    rowSeparator);

        System.out.printf("%,.4f Sec%n", (System.currentTimeMillis() - start) / 1000.0);
    }

    private String getLinesAsString(Path pathSource) throws IOException {

        try (BufferedReader br = Files.newBufferedReader(pathSource)) {
            return br.lines()
                    .map(line -> line.replaceAll("\\s+", " ")) // remove repeated spaces
                    .collect(Collectors.joining(" ")); // join as a single string
        }
    }

    /**
     * Returns a String of '-' and count equal to @colWidth
     * @param colWidth number of '-' to return
     * @return a String of '-' and count equal to @colWidth
     */
    private String buildSeparator(Number colWidth) {

        // colWidth + 2 for the space before and after the |
        return String.format("%" + (colWidth.intValue() + 2) + "s", " ").replaceAll(" ", "-");
    }

    public static void main(String[] args) throws IOException {

        // 1. Prompt for directory path
        // 2. Prompt for file name filters, or * to include all
        try (Scanner scanner = new Scanner(System.in)) {

            String baseDir = promptDir(scanner);

            String[] arrExtensions = promptSpaceDelimited(scanner, "Enter space delimited file extensions or leave blank for all");
            String[] arrFileNames = promptSpaceDelimited(scanner, "Enter space delimited file names or leave blank for all");

            String extensions = String.join("|", arrExtensions);
            String fileNames = String.join("|", arrFileNames);

            System.out.println("Summary:");
            System.out.printf("%20s%s%n", "Directory Path: ", baseDir);
            System.out.printf("%20s%s%n", "File Extension(s): ", extensions);
            System.out.printf("%20s%s%n", "File Name(s): ", fileNames);
            System.out.println(String.format("%100s%n", " ").replaceAll(" ", "="));

            new Compare(baseDir, String.format("(%s)\\w*\\.(%s)", fileNames, extensions));
        }
    }

    private static String[] promptSpaceDelimited(Scanner scanner, String messagePrompt) {

        String[] arrExtensions = null;

        while (arrExtensions == null) {

            System.out.printf("%s: ", messagePrompt);
            String fileExtensionFilter = scanner.nextLine();

            if (!fileExtensionFilter.isEmpty())
                arrExtensions = fileExtensionFilter.split("\\s+");

            if (arrExtensions == null || arrExtensions.length == 0) {

                arrExtensions = new String[]{".*"};
            }
        }

        return arrExtensions;
    }

    private static String promptDir(final Scanner scanner) {

        String baseDir = null;

        while (baseDir == null) {

            System.out.print("Enter the file's directory. The program will scan subdirectories as well: ");
            baseDir = scanner.nextLine();

            try {
                if (baseDir.isEmpty() || !Files.isDirectory(Paths.get(baseDir)))
                    throw new InvalidPathException(baseDir, "Not a directory");
            } catch (InvalidPathException ex) {

                System.out.printf("'%s' is not a valid directory%n", baseDir);
                baseDir = null;
            }
        }

        return baseDir;
    }
}
