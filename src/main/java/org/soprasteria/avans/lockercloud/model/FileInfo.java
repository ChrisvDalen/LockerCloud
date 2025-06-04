package org.soprasteria.avans.lockercloud.model;

public class FileInfo {
    private String name;
    private long size;
    private String type;

    public FileInfo(String name, long size, String type) {
        this.name = name;
        this.size = size;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReadableSize() {
        return formatSize(this.size);
    }

    public static String formatSize(long size) {
        double s = size;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while (s >= 1024 && i < units.length - 1) {
            s /= 1024;
            i++;
        }
        return String.format("%.1f %s", s, units[i]);
    }
}
