package io.github.blacknoize404.miniApkReader;

import java.io.IOException;
import java.nio.file.Path;

public class ApkInfoExtractor {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: ApkInfoExtractor <path-to-apk>");
            return;
        }

        Path apkPath = Path.of(args[0]);
        if (!apkPath.toFile().exists()) {
            System.err.println("File not found: " + apkPath);
            return;
        }

        try {
            ApkInfo info = ApkReader.read(apkPath);
            System.out.println(ApkReader.printInfo(info));
        } catch (IOException e) {
            System.err.println("Error reading APK: " + e.getMessage());
        }
    }
}
