package naturalexceptions.core.id;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates and encapsulates a host-independent, cross-language reproducible deterministic identity.
 *
 * <p>
 * Algorithm Specification:
 * <ol>
 * <li>Inputs: An ordered sequence of string parts.</li>
 * <li>Framing: Each part is encoded in UTF-8. Before each part's bytes, a 4-byte big-endian
 * integer containing the byte length of that part is fed to the digest. This prevents
 * concatenation ambiguity between e.g. ["ab", "c"] and ["a", "bc"].</li>
 * <li>Digest: Standard SHA-256 computed over the framed byte stream.</li>
 * <li>String representation: 64-character canonical lowercase hexadecimal string.</li>
 * <li>UUID representation: Derived from the first 16 bytes of the SHA-256 digest with RFC 4122
 * version 5 (octet 6 high nibble = 0x5) and variant 1 (octet 8 high 2 bits = 0b10).</li>
 * </ol>
 */
public final class DeterministicId implements Comparable<DeterministicId> {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private final String hex;

    private DeterministicId(String hex) {
        this.hex = hex;
    }

    public static DeterministicId derive(String... parts) {
        if (parts == null) {
            throw new IllegalArgumentException("Parts array cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] lenBuffer = new byte[4];
            for (String part : parts) {
                if (part == null) {
                    throw new IllegalArgumentException("Part within parts array cannot be null");
                }
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                int len = bytes.length;
                lenBuffer[0] = (byte) (len >>> 24);
                lenBuffer[1] = (byte) (len >>> 16);
                lenBuffer[2] = (byte) (len >>> 8);
                lenBuffer[3] = (byte) len;
                digest.update(lenBuffer);
                digest.update(bytes);
            }
            byte[] hash = digest.digest();
            return new DeterministicId(bytesToHex(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing in JVM", e);
        }
    }

    public static DeterministicId parse(String hex) {
        if (hex == null || !HEX_PATTERN.matcher(hex)
            .matches()) {
            throw new IllegalArgumentException("Expected 64-character lowercase hexadecimal string: " + hex);
        }
        return new DeterministicId(hex);
    }

    public String hex() {
        return hex;
    }

    public UUID asUuid() {
        byte[] bytes = hexToBytes(hex);
        // Octet 6: version 5
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x50);
        // Octet 8: variant RFC 4122 (0b10xxxxxx)
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);

        long msb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            result[i * 2] = HEX_CHARS[v >>> 4];
            result[i * 2 + 1] = HEX_CHARS[v & 0x0f];
        }
        return new String(result);
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public int compareTo(DeterministicId other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare DeterministicId to null");
        }
        return this.hex.compareTo(other.hex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeterministicId)) return false;
        DeterministicId that = (DeterministicId) o;
        return hex.equals(that.hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hex);
    }

    @Override
    public String toString() {
        return hex;
    }
}
