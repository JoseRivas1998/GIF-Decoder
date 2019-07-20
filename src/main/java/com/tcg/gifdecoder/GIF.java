package com.tcg.gifdecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GIF {

    private static final int HEADER_SIZE = 13;

    private byte[] fileBytes;

    private byte[] header;

    private String version;

    private int width;
    private int height;
    private boolean hasGlobalColorTable;
    private int resolution;
    private boolean sorted;
    private int globalColorTableLength;
    private int backgroundColorIndex;
    private int pixelAspectRatio;
    private RGBTriple[] globalColorTable;

    private int cursor;

    private int disposalMethod;
    private boolean userInputFlag;
    private boolean transparentColorFlag;
    private int delayTime;
    private int transparentColorIndex;
    private boolean trailerFound;

    private List<Image> images;

    public GIF(byte[] fileBytes) {
        this.cursor = 0;
        this.fileBytes = Arrays.copyOf(fileBytes, fileBytes.length);
        this.header = Arrays.copyOfRange(this.fileBytes, 0, HEADER_SIZE);
        this.decodeHeader();
        this.cursor = HEADER_SIZE;
        if (hasGlobalColorTable) {
            this.globalColorTable = decodeColorTable(this.globalColorTableLength);
        }
        this.images = new ArrayList<>();
        trailerFound = false;
        while (!trailerFound) {
            decodeBlock();
        }
    }

    private void decodeHeader() {
        String id = bytesToString(this.header, 0, 3);
        if (!id.equals("GIF")) {
            throw new RuntimeException("Invalid GIF ID");
        }
        this.version = bytesToString(this.header, 3, 6);

        this.width = ((this.header[7] & 0xFF) << 8) | (this.header[6] & 0xFF);
        this.height = ((this.header[9] & 0xFF) << 8) | (this.header[8] & 0xFF);

        byte field = this.header[10];

        int w = ((field & 0xFF) & 0b1000_0000) >> 7;
        hasGlobalColorTable = w == 1;

        int x = ((field & 0xFF) & 0b0111_0000) >> 4;
        this.resolution = x + 1;

        int y = ((field & 0xFF) & 0b0000_1000) >> 3;
        this.sorted = y == 1;

        int z = ((field & 0xFF) & 0b0000_0111);
        globalColorTableLength = 1 << (z + 1);

        this.backgroundColorIndex = (this.header[11] & 0xFF);

        this.pixelAspectRatio = (this.header[12] & 0xFF);

    }

    private RGBTriple[] decodeColorTable(int length) {
        byte[] colorTable = Arrays.copyOfRange(this.fileBytes, this.cursor, this.cursor + length * 3);
        RGBTriple[] result = new RGBTriple[length];
        for (int i = 0; i < length; i++) {
            result[i] = new RGBTriple(colorTable[i * 3], colorTable[i * 3 + 1], colorTable[i * 3 + 2]);
        }
        this.cursor += length * 3;
        return result;
    }

    private void decodeBlock() {
        int cursorByte = cursorByte();
        switch (cursorByte) {
            case 0x21:
                decodeExtension();
                break;
            case 0x2C:
                decodeImageDescriptor();
                break;
            case 0x3B:
                trailerFound = true;
                break;
            default:
                //System.out.println(Integer.toHexString(cursorByte));
        }
    }

    private void decodeImageDescriptor() {
        this.cursor++;
        this.images.add(new Image());
    }

    private void decodeExtension() {
        this.cursor++;
        int cursorByte = cursorByte();
        switch (cursorByte) {
            case 0xF9:
                decodeGraphicsControlExtension();
                break;
            case 0xFF:
                decodeApplicationExtension();
                break;
            default:
                //System.out.println(Integer.toHexString(cursorByte));
        }
    }

    private void decodeGraphicsControlExtension() {
        this.cursor++;
        int blockSize = cursorByte();
        this.cursor++;
        int packedField = cursorByte();
        this.disposalMethod = ((packedField & 0xFF) & 0b0001_1100) >> 2;
        this.userInputFlag = (((packedField & 0xFF) & 0b0000_0010) >> 1) == 1;
        this.transparentColorFlag = ((packedField & 0xFF) & 0b0000_0001) == 1;
        this.cursor++;
        this.delayTime = ((this.fileBytes[this.cursor + 1] & 0xFF) << 8) | (this.fileBytes[this.cursor] & 0xFF);
        this.cursor += 2;
        this.transparentColorIndex = cursorByte();
        this.cursor += 2;
    }

    private void decodeApplicationExtension() {
        System.out.println(Integer.toHexString(cursorByte()).toUpperCase());
        this.cursor += 18;
    }

    private int cursorByte() {
        return (this.fileBytes[this.cursor] & 0xFF);
    }

    private static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, 0, bytes.length);
    }

    private static String bytesToHexString(byte[] bytes) {
        return bytesToHexString(bytes, 0, bytes.length);
    }

    private static String bytesToHexString(byte[] bytes, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(String.format("%02X", (bytes[i] & 0xFF)));
            if (i < to - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private static String bytesToString(byte[] bytes, int from, int to) {
        StringBuilder sb = new StringBuilder();
        char[] chars = bytesToCharArray(bytes, from, to);
        for (char aChar : chars) {
            sb.append(aChar);
        }
        return sb.toString();
    }

    private char[] bytesToCharArray(byte[] bytes) {
        return bytesToCharArray(bytes, 0, bytes.length);
    }

    private static char[] bytesToCharArray(byte[] bytes, int from, int to) {
        char[] chars = new char[to - from];
        for (int i = from; i < to; i++) {
            int val = bytes[i];
            if (val >= ' ') {
                chars[i - from] = (char) val;
            } else {
                chars[i - from] = '.';
            }
        }
        return chars;
    }

    private class RGBTriple {
        final int R;
        final int G;
        final int B;

        RGBTriple(byte r, byte g, byte b) {
            this.R = (r & 0xFF);
            this.G = (g & 0xFF);
            this.B = (b & 0xFF);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RGBTriple rgbTriple = (RGBTriple) o;
            return R == rgbTriple.R &&
                    G == rgbTriple.G &&
                    B == rgbTriple.B;
        }

        @Override
        public int hashCode() {
            return Objects.hash(R, G, B);
        }

        @Override
        public String toString() {
            return String.format("rgb(%d, %d, %d)", this.R, this.G, this.B);
        }
    }

    private class Image {
        int imageLeft;
        int imageTop;
        int imageWidth;
        int imageHeight;
        boolean localColorTableFlag;
        boolean interlaceFlag;
        boolean sortFlag;
        int localColorTableLength;

        RGBTriple[] localColorTable;

        Image() {
            imageLeft = ((fileBytes[cursor + 1] & 0xFF) << 8) | (fileBytes[cursor] & 0xFF);
            cursor += 2;
            imageTop = ((fileBytes[cursor + 1] & 0xFF) << 8) | (fileBytes[cursor] & 0xFF);
            cursor += 2;
            imageWidth = ((fileBytes[cursor + 1] & 0xFF) << 8) | (fileBytes[cursor] & 0xFF);
            cursor += 2;
            imageHeight = ((fileBytes[cursor + 1] & 0xFF) << 8) | (fileBytes[cursor] & 0xFF);
            cursor++;
            int packedField = cursorByte();
            localColorTableFlag = (((packedField & 0xFF) & 0b1000_0000) >> 7) == 1;
            interlaceFlag = (((packedField & 0xFF) & 0b0100_0000) >> 6) == 1;
            sortFlag = (((packedField & 0xFF) & 0b0010_0000) >> 5) == 1;
            localColorTableLength = (packedField & 0xFF) & 0b0000_0111;
            cursor += 2;
            if (localColorTableFlag) {
                localColorTable = decodeColorTable(localColorTableLength);
            }
            int LZWMinCodeSize = cursorByte();
            cursor++;
            while(cursorByte() != 0) {
                int bytesFollow = cursorByte();
                int clearCode = localColorTableFlag ? localColorTableLength : globalColorTableLength;
                int endOfInformationCode = clearCode + 1;
                System.out.println(clearCode);
                System.out.println(endOfInformationCode);
                cursor++;
                for (int i = 0; i < bytesFollow; i++) {
                    System.out.println(Integer.toHexString(cursorByte()));
                    cursor++;
                }
            }
            cursor++;
        }



    }

}
