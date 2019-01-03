package CrawlerAndQueryEngine;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * A very basic implementation of an HTTP web server which provides a search bar and
 * responds to simple search queries.
 *
 * Run this file to run the actual web-server.
 */
public class WebServer {

  /**
   * main method to start a server.
   * Loads a WebIndex from the default save location.
   */
  public static void main(String[] args) throws Exception {
    WebQueryEngine wqe = WebQueryEngine.fromIndex(
        (WebIndex) Index.load("index.db"));
    WebServer server = new WebServer(wqe);

    server.serve();
  }

  private WebQueryEngine engine;

  public WebServer(WebQueryEngine wqe) {
    engine = wqe;
  }

  /**
   * Open the server to incoming connections, loop and respond.
   */
  public void serve() throws IOException {
    int port = 1989;
    ServerSocket serverSocket = new ServerSocket(port);
    System.err.println("Running server on port: " + port);

    // repeatedly wait for connections, and process
    while (true) {
      Socket clientSocket = serverSocket.accept();

      BufferedReader in = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream()));
      BufferedWriter out = new BufferedWriter(
          new OutputStreamWriter(clientSocket.getOutputStream()));

      String s;
      HttpRequest request = parseRequest(in);


      if(request == null) {
        continue;
      }

      // The most basic of routing tables
      switch (request.url) {
        case "/search":
          //long previousTime = java.lang.System.currentTimeMillis();
          renderSearchResults(out, request.params);
          //System.out.println(java.lang.System.currentTimeMillis() - previousTime);
          break;
        case "/":
        default:
          renderHomepage(out);
      }
      out.close();
      in.close();
      clientSocket.close();
    }
  }

  /**
   * This is going to serve the page that shows results of querying the WebIndex.
   */
  private void renderSearchResults(BufferedWriter out,
      HashMap<String, String> params) {
    StringBuilder content = new StringBuilder();
    content.append("<TITLE>Results</TITLE>");
    content.append("<body> <div style='width:800px; margin:0 auto;'>");
    content.append("<a href='/'>");
    content.append(layoutLogo());
    content.append("</a>");

    // Probably add a disclaimer about file links
    List<Page> results = new ArrayList<>(engine.query(params.get("query")));
    Collections.sort(results);

    if (results.size() > 0) {
      content.append("<p>Here are the results of your query.<br> ");
      content.append("Some browsers don't follow file:// links ");
      content.append("for security reasons so you'll have to paste the link ");
      content.append("into the URL bar instead of clicking it.</p>");
    } else {
      content.append("<p>Your query returned no results.</p>");
    }
    content.append("<ul>\n");
    for (Page p : results) {
      content.append("<li>");
      content.append(layoutPageContent(p));
      content.append('\n');
      content.append(p.getConnectedness());
      content.append(" connections");
      content.append("</li>\n");
    }
    content.append("</ul>\n</div> </body>");

    renderResponse(out, content.toString());
  }

  /**
   * This will render our snazzy homepage.
   */
  private void renderHomepage(BufferedWriter out) {
    String content = "<TITLE>Home</TITLE>" +
      "<body> <div style='width:800px; margin:0 auto;'>" + layoutLogo() +
      "<form id='form' method='get' action='search' style='width: 50%; margin:0 auto;'>" +
      "<input name='query' class='element text medium' style='width: 100%;' type='text' maxlength='255' value=''/>" +
      "</form> <br>" +
      "<button type='submit' form='form' value='Submit' style='display: block; margin:0 auto;'>Search</button>" +
      "</div> </body>";

    renderResponse(out, content);
  }

  /**
   * This method is going to try and create the logo on the page by loading
   * an image file. It tries to open the image on each request instead of
   * holding onto it, which isn't ideal.
   */
  private String layoutLogo() {
    try {
      byte[] byteArray = new byte[5000];
      FileInputStream fis = new FileInputStream(new File("tsoogle.png"));
      Base64.Encoder enc = Base64.getEncoder();

      // We're assuming that it manages to read all at once
      int actual_length = fis.read(byteArray);

      byte[] exactByteArray = new byte[actual_length];
      for (int i = 0; i < actual_length; i++) {
        exactByteArray[i] = byteArray[i];
      }

      String base64String = enc.encodeToString(exactByteArray);

      return "<img src='data:image/png;base64," + base64String +
        "' style='display: block; margin:0 auto;'>";
    } catch (IOException e) {
      // Guess we can't show the image
      return "<h1>TSoogle</h1>";
    }
  }

  /**
   * We'll use this to get the HTML that represents our Page object.
   * It might be better to Encapsulate this within the Page class itself.
   */
  private String layoutPageContent(Page p) {
    return "<a href='" + p.getURL() + "'>" + p.getURL() + "</a>";
  }

  /**
   * This method pretends to be a real webserver by sending valid HTTP headers
   * in front of the content that we're responding with.
   */
  private void renderResponse(BufferedWriter out, String content) {
    try {
      out.write("HTTP/1.0 200 OK\r\n" +
          "Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n" +
          "Server: 314H/0.1\r\n" +
          "Content-Type: text/html\r\n" +
          "Content-Length: " + content.length() + "\r\n" +
          "Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n" +
          "Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n" +
          "\r\n" +
          content);
    } catch (IOException e) {
      // An error with this request might not need to bring down the server
      e.printStackTrace(System.err);
    }
  }

  /**
   * This holds the parts of a request we might care about.
   */
  private class HttpRequest {
    public final String method, url;
    public final HashMap<String, String> headers, params;

    HttpRequest(String u, String m,
        HashMap<String, String> h,
        HashMap<String, String> p) {
      url = u;
      method = m;
      headers = h;
      params = p;
    }
  }

  /**
   * Returns a HttpRequest object containing the information from the next
   * section of the input stream, or null if we can't understand it.
   */
  private HttpRequest parseRequest(BufferedReader reader) throws IOException {
    String method = "", url = "";
    HashMap<String, String> headers = new HashMap<>(), params = new HashMap<>();

    String initial = reader.readLine();
    if (initial == null || initial.length() == 0 ||
        Character.isWhitespace(initial.charAt(0))) {
      // Bad things
      return null;
    } else {
      String[] cmd = initial.split("\\s");
      if (cmd.length < 3) {
        // Not enough to parse
        return null;
      } else {
        method = cmd[0];

        // If we have a page request
        if (cmd[0].equals("GET") || cmd[0].equals("HEAD")) {
          int idx = cmd[1].indexOf('?');
          if (idx < 0) {
            // There aren't any params
            url = URLDecoder.decode(cmd[1], "ISO-8859-1");
          } else {
            url = URLDecoder.decode(cmd[1].substring(0, idx), "ISO-8859-1");
            String[] prms = cmd[1].substring(idx+1).split("&");

            // Store the params in the map
            for (String param: prms) {
              String[] temp = param.split("=");
              if (temp.length == 2) {
                params.put(URLDecoder.decode(temp[0], "ISO-8859-1"),
                    URLDecoder.decode(temp[1], "ISO-8859-1"));
              } else if (temp.length == 1 && param.indexOf('=') == param.length()-1) {
                // Empty string is handled separatedly
                params.put(URLDecoder.decode(temp[0], "ISO-8859-1"), "");
              }
            }
          }

          // Read the headers into the map
          String line = reader.readLine();
          while (!line.equals("")) {
            idx = line.indexOf(':');
            if (idx >= 0) {
              headers.put(line.substring(0, idx).toLowerCase(),
                  line.substring(idx+1).trim());
            }
            line = reader.readLine();
          }
        } else {
          // Non-GET request, which we aren't handling right now
          return null;
        }
      }
    }

    return new HttpRequest(url, method, headers, params);
  }
}
