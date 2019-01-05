package myapp.upb.androidfirebase.models;

public class UploadInfo {

    private String id;
    private String name;
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UploadInfo(String name, String url) {
        this.id = collapseFilename(name);
        this.name = name;
        this.url = url;
    }

    public UploadInfo () {

    }

    public String toString() {
        return "[" + id + ", " + name + ", " + url + "]";
    }

    public static String collapseFilename(String name) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            switch (name.charAt(i)) {
                case ']':
                case '[':
                case '$':
                case '#':
                case '.':
                    break;
                default:
                    sb.append(name.charAt(i));
            }
        }

        return sb.toString();
    }

    public static String encodeFilename(String name) {
        return name.replace(".", ",");
    }

    public static String decodeFilename(String name) {
        return name.replace(",", ".");
    }
}
