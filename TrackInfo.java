public class TrackInfo
{
    private String title;
    private String albumTitle;
    private String length;
    private String artworkPath;
    private String pagePath;

    public TrackInfo(String title, String albumTitle, String length, String artworkPath, String pagePath)
    {
        this.title = title;
        this.albumTitle = albumTitle;
        this.length = length;
        this.artworkPath = artworkPath;
        this.pagePath = pagePath;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getLength() {
        return length;
    }

    public String getArtworkPath() {
        return artworkPath;
    }

    public String getPagePath() {
        return pagePath;
    }
}
