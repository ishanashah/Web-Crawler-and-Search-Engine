package CrawlerAndQueryEngine;
import java.io.Serializable;
import java.net.URL;

public class Page implements Serializable, Comparable {
    private static final long serialVersionUID = 1L;

    // The URL the page was located at.
    private URL url;

    private int connectedness = 1;
    public void increment() {
        connectedness++;
    }
    public int getConnectedness() {
        return connectedness;
    }

    private int ID;
    public Page(URL url, int ID) {
        this.url = url;
        this.ID = ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }



    /**
     * Creates a Page with a given URL.
     * @param url The url of the page.
     */
    public Page(URL url) {
        this.url = url;
    }

    /**
     * @return the URL of the page.
     */
    public URL getURL() { return url; }


    @Override
    public int hashCode(){
        return ID;
        //return url.hashCode();
    }

    public int compareTo(Object other){
        return ((Page) other).connectedness - connectedness;
    }
}
