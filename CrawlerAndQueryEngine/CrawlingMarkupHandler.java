package CrawlerAndQueryEngine;

import java.util.*;
import java.net.*;
import org.attoparser.simple.*;

/**
 * A markup handler which is called by the Attoparser markup parser as it parses the input;
 * responsible for building the actual web index.
 */
public class CrawlingMarkupHandler extends AbstractSimpleMarkupHandler {

    //The WebIndex object used to index all pages
    private WebIndex index = new WebIndex();
    //Queue of text representing every word on a page
    private Queue<String> text = new LinkedList<>();
    //The last word being built in the page
    private StringBuilder lastWord = new StringBuilder();
    //Set of all URL's that have been visited
    private HashMap<URL, Page> pastPages = new HashMap<>();
    //New URL's that still need to be visited
    private List<URL> newURLS = new LinkedList<>();
    //The URL currently being parsed
    private Page currentPage;


    public CrawlingMarkupHandler() {}

    /**
    * This method returns the complete index that has been crawled thus far when called.
    */
    public Index getIndex() {
        // TODO: Implement this!
        return index;
    }

    /**
    * This method returns any new URLs found to the Crawler; upon being called, the set of new URLs
    * should be cleared.
    */
    public List<URL> newURLs() {
        List<URL> list = newURLS;
        newURLS = new LinkedList<>();
        return list;
    }

    /**
    * Called when the parser first starts reading a document.
    * @param startTimeNanos  the current time (in nanoseconds) when parsing starts
    * @param line            the line of the document where parsing starts
    * @param col             the column of the document where parsing starts
    */
    public void handleDocumentStart(long startTimeNanos, int line, int col) {
        // TODO: Implement this.
        text = new LinkedList<>();
        lastWord = new StringBuilder();
    }

    /**
    * Called when the parser finishes reading a document.
    * @param endTimeNanos    the current time (in nanoseconds) when parsing ends
    * @param totalTimeNanos  the difference between current times at the start
    *                        and end of parsing
    * @param line            the line of the document where parsing ends
    * @param col             the column of the document where the parsing ends
    */
    public void handleDocumentEnd(long endTimeNanos, long totalTimeNanos, int line, int col) {
        // TODO: Implement this.
        //System.out.println("End of document");
        if(lastWord.length() > 0){
            text.add(lastWord.toString());
        }
        lastWord = new StringBuilder();
        index.addPhrase(text);
    }

    /**
    * Called at the start of any tag.
    * @param elementName the element name (such as "div")
    * @param attributes  the element attributes map, or null if it has no attributes
    * @param line        the line in the document where this elements appears
    * @param col         the column in the document where this element appears
    */
    public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
        // TODO: Implement this.
        if(attributes == null || attributes.keySet() == null) {return;}

        //If the element contains a URl that hasn't been visited,
        //add it to newURLS
        if(elementName.toLowerCase().equals("a")) {
            for(String s: attributes.keySet()) {
                if(s.toLowerCase().equals("href")) {
                    String fin_link = attributes.get(s);
                    URL next = null;
                    try {
                        next = new URL(currentPage.getURL(), fin_link);

                        if(!pastPages.keySet().contains(next)) {
                            String urlString = next.toString();
                            if(urlString.endsWith(".html") || urlString.endsWith(".htm")){
                                newURLS.add(next);
                                pastPages.put(next, new Page(next));
                            }
                        } else {
                            pastPages.get(next).increment();
                        }
                    } catch(MalformedURLException e) {
                    }
                }
            }
        }

    }

    /**
    * Called at the end of any tag.
    * @param elementName the element name (such as "div").
    * @param line        the line in the document where this elements appears.
    * @param col         the column in the document where this element appears.
    */
    public void handleCloseElement(String elementName, int line, int col) {
        // TODO: Implement this.
        //System.out.println("End element:   " + elementName);
    }

    /**
    * Called whenever characters are found inside a tag. Note that the parser is not
    * required to return all characters in the tag in a single chunk. Whitespace is
    * also returned as characters.
    * @param ch      buffer containint characters; do not modify this buffer
    * @param start   location of 1st character in ch
    * @param length  number of characters in ch
    */
    public void handleText(char ch[], int start, int length, int line, int col) {
        // TODO: Implement this.
        //System.out.print("Characters:    \"");

        for(int i = start; i < start + length; i++) {
            //Tokenize the incoming stream of characters
            if(Character.isLetterOrDigit(ch[i])) {
                lastWord.append(Character.toLowerCase(ch[i]));
            } else {
                if(lastWord.length() > 0){
                    text.add(lastWord.toString());
                    lastWord = new StringBuilder();
                }
            }
        }

    }

    public void setCurrentURL(URL currentURL){
        Page p = pastPages.get(currentURL);
        if(p == null) {
            p = new Page(currentURL);
            pastPages.put(currentURL, p);
        }
        index.setCurrentPage(p);
        this.currentPage = p;
    }
}
