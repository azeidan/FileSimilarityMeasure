import java.nio.file.Path;
import java.util.Objects;

public class CompareResult implements Comparable<CompareResult> {

    /**
     * First file's path
     */
    Path pathFirstFile;
    /**
     * Second file's path
     */
    Path pathSecondFile;

    /**
     * Jaccard score comparison of the two files in @pathFirstFile and @pathSecondFile
     */
    double scoreJaccard;
    /**
     * Jaro score comparison of the two files in @pathFirstFile and @pathSecondFile
     */
    double scoreJaro;

    /**
     * LCS score comparison of the two files in @pathFirstFile and @pathSecondFile
     */
    int scoreLCS;
    /**
     * Fuzzy score comparison of the two files in @pathFirstFile and @pathSecondFile
     */
    int scoreFuzzy;

    public CompareResult(Path pathFirstFile, Path pathSecondFile) {

        this.pathFirstFile = pathFirstFile;
        this.pathSecondFile = pathSecondFile;
    }

    /**
     * Comparision based on the Jaccard + Jaro scores
     *
     * @param other the object to be compared
     *
     * @return <0, 0, or >0 comparision of  Jaccard + Jaro scores
     */
    @Override
    public int compareTo(CompareResult other) {

        return Double.compare(scoreJaccard + scoreJaro, other.scoreJaccard + other.scoreJaro);
    }

    /**
     * Returns the hash of the files paths @pathFirstFile and @pathSecondFile
     * @return hash of the files paths @pathFirstFile and @pathSecondFile
     */
    @Override
    public int hashCode() {
        return Objects.hash(pathFirstFile, pathSecondFile);
    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;

        CompareResult that = (CompareResult) other;

        return pathFirstFile.equals(that.pathFirstFile) && pathSecondFile.equals(that.pathSecondFile) || pathFirstFile.equals(that.pathSecondFile) && pathSecondFile.equals(that.pathFirstFile);
    }

    public void update(double scoreJaccard, double scoreJaro, int scoreLCS, int scoreFuzzy) {

        this.scoreJaccard = scoreJaccard;
        this.scoreJaro = scoreJaro;
        this.scoreLCS = scoreLCS;
        this.scoreFuzzy = scoreFuzzy;

    }
}
