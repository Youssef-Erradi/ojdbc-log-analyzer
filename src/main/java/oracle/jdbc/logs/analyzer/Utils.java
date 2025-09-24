package oracle.jdbc.logs.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class providing I/O helper methods for file and URL handling.
 * <p>
 * This class is final and cannot be instantiated.
 */
final class Utils {

  private static final String FILE_LOCATION_CANNOT_BE_NULL_OR_EMPTY = "fileLocation cannot be null or empty.";

  private Utils() {}

  /**
   * Returns a {@link BufferedReader} for the specified file location.
   * <p>
   * The location can be either a local file path or a URL.
   *
   * @param fileLocation the file path or URL to read from
   * @return a {@link BufferedReader} for the specified location
   * @throws IOException if an I/O error occurs opening the file or URL
   * @throws IllegalArgumentException if {@code fileLocation} is null or blank
   */
  static BufferedReader getBufferedReader(final String fileLocation) throws IOException {
    requireNonBlank(fileLocation, FILE_LOCATION_CANNOT_BE_NULL_OR_EMPTY);
    return new BufferedReader(getReader(fileLocation));
  }

  /**
   * Returns a {@link Reader} for the specified location.
   * <p>
   * If the location is a valid URL, an {@link InputStreamReader} is returned; otherwise,
   * a {@link FileReader} is returned for a local file.
   *
   * @param fileLocation the file path or URL to read from
   * @return a Reader for the specified location
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if {@code location} is null or blank
   */
  static Reader getReader(final String fileLocation) throws IOException {
    requireNonBlank(fileLocation, FILE_LOCATION_CANNOT_BE_NULL_OR_EMPTY);

    if (isURL(fileLocation))
      return new InputStreamReader(new URL(fileLocation).openStream());
    else
      return new FileReader(fileLocation);
  }

  /**
   * Checks if a string represents a valid URL.
   *
   * @param fileLocation the string to test
   * @return {@code true} if the string is a valid URL, {@code false} otherwise
   * @throws IllegalArgumentException if {@code location} is null or blank
   */
  static boolean isURL(final String fileLocation) {
    requireNonBlank(fileLocation, FILE_LOCATION_CANNOT_BE_NULL_OR_EMPTY);
    try {
      new URL(fileLocation);
      return true;
    } catch (MalformedURLException ignored) {
      return false;
    }
  }

  /**
   * Returns the size in bytes of the file at the given location.
   * <p>
   * The location can be a local file path or a URL.
   * For URLs, this method retrieves the content length from the URL connection.
   * For local files, it queries the filesystem for the file's length.
   *
   * @param fileLocation the file path or URL whose size is to be determined
   * @return the size of the file or URL content, or 0 if the size cannot be determined
   * @throws IllegalArgumentException if {@code fileLocation} is null or blank
   */
  static long getFileSize(String fileLocation) {
    requireNonBlank(fileLocation, FILE_LOCATION_CANNOT_BE_NULL_OR_EMPTY);

    if (isURL(fileLocation)) {
      try {
        return new URL(fileLocation).openConnection().getContentLengthLong();
      } catch (Exception ignore) {
        return 0;
      }
    }
    else
      return new File(fileLocation).length();
  }

  /**
   * Validates that the provided string is not null or blank.
   *
   * @param string the string to validate
   * @param errorMessage the error message for exception
   * @throws IllegalArgumentException if {@code string} is null or blank
   */
  static void requireNonBlank(final String string, final String errorMessage) {
    if (string == null || string.isBlank())
      throw new IllegalArgumentException(errorMessage);
  }
}
