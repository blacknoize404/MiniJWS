package io.github.blacknoize404.miniApkReader;

import java.util.List;

public record ApkInfo(
    String packageName,
    String versionName,
    long versionCode,
    String minSdkVersion,
    String targetSdkVersion,
    List<String> permissions,
    List<String> features,
    String label,
    String icon
) {}
