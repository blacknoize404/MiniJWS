package io.github.blacknoize404.miniApkReader;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.UseFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class ApkReader {

    private ApkReader() {}

    public static ApkInfo read(Path apkPath) throws IOException {
        try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
            ApkMeta meta = apkFile.getApkMeta();

            return new ApkInfo(
                meta.getPackageName(),
                meta.getVersionName(),
                meta.getVersionCode(),
                meta.getMinSdkVersion(),
                meta.getTargetSdkVersion(),
                meta.getUsesPermissions().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toList()),
                meta.getUsesFeatures().stream()
                    .map(UseFeature::getName)
                    .collect(Collectors.toList()),
                meta.getLabel(),
                meta.getIcon()
            );
        }
    }

    public static String printInfo(ApkInfo info) {
        var sb = new StringBuilder();
        sb.append("=== APK Information ===\n");
        sb.append("Package: ").append(info.packageName()).append("\n");
        sb.append("Version: ").append(info.versionName()).append(" (").append(info.versionCode()).append(")\n");
        sb.append("SDK: min=").append(info.minSdkVersion()).append(", target=").append(info.targetSdkVersion()).append("\n");
        sb.append("Label: ").append(info.label()).append("\n");
        sb.append("Icon: ").append(info.icon()).append("\n");
        sb.append("Permissions:\n");
        info.permissions().forEach(p -> sb.append("  - ").append(p).append("\n"));
        sb.append("Features:\n");
        info.features().forEach(f -> sb.append("  - ").append(f).append("\n"));
        return sb.toString();
    }
}
