package com.tcg.gifdecoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestClient {

    public static void main(String[] args) throws IOException {
        File file = new File("C:\\Users\\tcgdeploy\\Pictures\\sample_1.gif");
        new GIF(Files.readAllBytes(file.toPath()));
    }

}
