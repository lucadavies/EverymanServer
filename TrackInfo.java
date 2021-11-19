public class TrackInfo
{
    private String title;
    private String albumTitle;
    private String length;
    private String artworkPath;
    private String pagePath;
    private String audioPath;

    public TrackInfo(String title, String albumTitle, String length, String pagePath, String audioPath, String artworkPath)
    {
        this.title = title;
        this.albumTitle = albumTitle;
        this.length = length;
        this.pagePath = pagePath;
        this.audioPath = audioPath;
        this.artworkPath = artworkPath;
    }

    public String getTitle() { return title; }

    public String getAlbumTitle() { return albumTitle; }

    public String getLength() { return length; }

    public String getPagePath() { return pagePath; }

    public String getAudioPath() { return audioPath; }

    public String getArtworkPath() { return artworkPath; }
}
